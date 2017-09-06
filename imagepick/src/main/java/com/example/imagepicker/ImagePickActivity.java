package com.example.imagepicker;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imagepicker.bean.FolderBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImagePickActivity extends AppCompatActivity {
    private DirPopupWindow popupWindow;
    private GridView mGridView;
    private ImageAdapter adapter;
    private RelativeLayout bottomLayout;
    private TextView dirText;
    private Button dirButton;
    private List<String>imgs=new ArrayList<>();
    private List<FolderBean>folderBeanList=new ArrayList<>();
    private File mCurrentDir;
    private int mMaxCount;
    private int mPickCount;

    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            dataToView();

            initPopupWindow();

            //回调每个图片点击的事件
            adapter.setOnImageClickListener(new ImageAdapter.OnImageClickListener() {
                @Override
                public void onImageClick(boolean isSelected,int selectedCount) {
                    dirButton.setText(selectedCount+"/"+mPickCount);
                    if(selectedCount==0){
                        dirButton.setClickable(false);
                        dirButton.setEnabled(false);
                        dirButton.setBackgroundColor(Color.parseColor("#CACACA"));
                    }else {
                        dirButton.setClickable(true);
                        dirButton.setEnabled(true);
                        dirButton.setBackgroundColor(Color.parseColor("#4FA95D"));
                    }
                }
            });

            //设置按钮点击事件
            dirButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent=new Intent();
                    intent.putStringArrayListExtra(PickFlag.PICK_LIST,adapter.getSelectedImg());
                    setResult(RESULT_OK,intent);
                    finish();
                }
            });
        }
    };

    /**
     * 初始化popupwindow
     */
    private void initPopupWindow() {
        popupWindow=new DirPopupWindow(this,folderBeanList);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        popupWindow.setOnDirSelectedListener(new DirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean bean) {
                mCurrentDir=new File(bean.getDir());
                dataToView();
                popupWindow.dismiss();
            }
        });
    }

    /**
     * 内容区域变量
     */
    private void lightOn() {
        WindowManager.LayoutParams lp=getWindow().getAttributes();
        lp.alpha=1.0f;
        getWindow().setAttributes(lp);
    }

    /**
     * 内容区域变暗
     */
    private void lightOff() {
        WindowManager.LayoutParams lp=getWindow().getAttributes();
        lp.alpha=0.3f;
        getWindow().setAttributes(lp);
    }

    /**
     * 根据数据绑定到视图
     */
    private void dataToView() {
        if(mCurrentDir==null){
            Toast.makeText(this,"未发现任何图片",Toast.LENGTH_SHORT).show();
            return;
        }

        imgs= Arrays.asList(mCurrentDir.list());
        adapter=new ImageAdapter(this,imgs,mCurrentDir.getAbsolutePath(),mPickCount);
        mGridView.setAdapter(adapter);

        dirText.setText(mCurrentDir.getName());
        mMaxCount=imgs.size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_pick);
        //设置标题名字
        setTitle("选择图片");
        mGridView= (GridView) findViewById(R.id.gridview_id);
        bottomLayout=(RelativeLayout)findViewById(R.id.bottom_layout);
        dirText=(TextView)findViewById(R.id.dir_name_id);
        dirButton=(Button)findViewById(R.id.dir_button_id);
        //检查访问sd卡的权限，没有就申请访问sd卡的权限
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }else {
            //扫描图片
            scanImgs();
        }

        //从上一个活动获取传来的需要选取的图片数量，默认为1
        mPickCount=getIntent().getIntExtra(PickFlag.PICK_COUNT,1);
        //初始化button上显示的字,并不可点击
        dirButton.setText("0/"+mPickCount);
        dirButton.setClickable(false);
        dirButton.setEnabled(false);

        /**
         * 点击底部弹出选择文件夹界面
         */
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //popupWindow.setAnimationStyle();
                popupWindow.showAsDropDown(bottomLayout,0,0);
                lightOff();
            }
        });
    }

    /**
     * 扫描图片
     */
    private void scanImgs() {
        //判断存储卡是否可用
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Toast.makeText(this,"当前存储卡不可用",Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri imgUri= MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr=ImagePickActivity.this.getContentResolver();

                Cursor cursor=cr.query(imgUri,null,MediaStore.Images.Media.MIME_TYPE+"=?or "+MediaStore.Images.Media.MIME_TYPE+"=?",new String[]{"image/jpeg","image/png"},MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mDirPaths=new HashSet<>();
                if(cursor.moveToFirst()) {
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        File parentFile = new File(path).getParentFile();
                        if (parentFile == null) {
                            continue;
                        }

                        String dirPath = parentFile.getAbsolutePath();
                        FolderBean folderBean = null;

                        if (mDirPaths.contains(dirPath)) {
                            continue;
                        } else {
                            mDirPaths.add(dirPath);
                            folderBean = new FolderBean();
                            folderBean.setDir(dirPath);
                            folderBean.setFirstImgPath(path);
                        }

                        if (parentFile.list() == null) {
                            continue;
                        }

                        int picCount = parentFile.list().length;

                        folderBean.setCount(picCount);

                        folderBeanList.add(folderBean);

                        if (picCount > mMaxCount) {
                            mMaxCount = picCount;
                            mCurrentDir = parentFile;
                        }
                    }
                }
                cursor.close();

                //通知handler扫描图片完成
                mHandler.sendEmptyMessage(0x110);
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    scanImgs();
                }else {
                    Toast.makeText(this,"授权失败",Toast.LENGTH_SHORT).show();
                }
        }
    }
}
