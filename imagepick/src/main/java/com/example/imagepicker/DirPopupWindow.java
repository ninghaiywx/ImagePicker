package com.example.imagepicker;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.imagepicker.bean.FolderBean;

import java.util.List;

/**
 * Created by yue on 2017/9/5.
 */

public class DirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private ListView mListView;
    private View mConvertView;
    private List<FolderBean>mDatas;
    private OnDirSelectedListener onDirSelectedListener;

    public interface OnDirSelectedListener{
        void onSelected(FolderBean bean);
    }

    public void setOnDirSelectedListener(OnDirSelectedListener onDirSelectedListener) {
        this.onDirSelectedListener = onDirSelectedListener;
    }

    public DirPopupWindow(Context context, List<FolderBean> datas){
        calWidthAndHeight(context);
        mConvertView= LayoutInflater.from(context).inflate(R.layout.popup_main,null);
        mDatas=datas;
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_OUTSIDE){
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        mListView= (ListView) mConvertView.findViewById(R.id.list_id);
        mListView.setAdapter(new DirAdapter(context,mDatas));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(onDirSelectedListener!=null){
                    onDirSelectedListener.onSelected(mDatas.get(position));
                }
            }
        });
    }

    /**
     * 计算popupwindow的宽度和高度
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        mWidth=context.getResources().getDisplayMetrics().widthPixels;
        mHeight= (int) (context.getResources().getDisplayMetrics().heightPixels*0.7f);
    }

    private class DirAdapter extends ArrayAdapter<FolderBean> {
        private LayoutInflater mInflater;
        private List<FolderBean>mDatas;
        public DirAdapter(@NonNull Context context,@NonNull List<FolderBean> objects) {
            super(context, 0, objects);
            mInflater=LayoutInflater.from(context);
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder=null;
            if(convertView==null){
                viewHolder=new ViewHolder();
                convertView=mInflater.inflate(R.layout.popup_item,parent,false);
                viewHolder.img= (ImageView) convertView.findViewById(R.id.dir_item_img);
                viewHolder.dirCount= (TextView) convertView.findViewById(R.id.dir_item_count);
                viewHolder.dirName= (TextView) convertView.findViewById(R.id.dir_item_name);

                convertView.setTag(viewHolder);
            }else {
                viewHolder= (ViewHolder) convertView.getTag();
            }
            FolderBean bean=getItem(position);
            viewHolder.img.setImageResource(R.drawable.dir_img);
            ImageLoader.getInstance(3, ImageLoader.TYPE.LIFO).loadImage(bean.getFirstImgPath(),viewHolder.img);
            viewHolder.dirCount.setText(bean.getCount()+"");
            viewHolder.dirName.setText(bean.getName());

            return convertView;
        }

        private class ViewHolder{
            ImageView img;
            TextView dirName,dirCount;
        }
    }
}
