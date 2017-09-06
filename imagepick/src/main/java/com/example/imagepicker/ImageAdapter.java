package com.example.imagepicker;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {
        private ArrayList<String> selectedImg=new ArrayList<>();
        private LayoutInflater inflater;
        private List<String>data;
        private String dirPath;
        private int mPickCount;
        private Context mContext;
        private OnImageClickListener onImageClickListener;

    public void setOnImageClickListener(OnImageClickListener onImageClickListener) {
        this.onImageClickListener = onImageClickListener;
    }

    public ImageAdapter(Context context, List<String>data, String dirPath,int mPickCount){
            this.data=data;
            this.mPickCount=mPickCount;
            this.dirPath=dirPath;
            mContext=context;
            inflater=LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder=null;
            if(convertView==null){
                convertView=inflater.inflate(R.layout.item_gridview,viewGroup,false);
                viewHolder=new ViewHolder();
                viewHolder.img= (ImageView) convertView.findViewById(R.id.image_item);
                viewHolder.select= (ImageButton) convertView.findViewById(R.id.item_select);
                convertView.setTag(viewHolder);
            }else {
                viewHolder= (ViewHolder) convertView.getTag();
            }

            final String filePath=dirPath+"/"+data.get(position);
            viewHolder.img.setImageResource(R.drawable.default_img);
            if(selectedImg.contains(filePath)) {
                viewHolder.select.setImageResource(R.drawable.selected_img);
            }else {
                viewHolder.select.setImageResource(R.drawable.unselected_img);
            }
            viewHolder.img.setColorFilter(null);

            ImageLoader.getInstance(3, ImageLoader.TYPE.LIFO).loadImage(dirPath+"/"+data.get(position),viewHolder.img);

            final ViewHolder finalViewHolder = viewHolder;
            viewHolder.img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //图片已经被选择
                    if(selectedImg.contains(filePath)){
                        selectedImg.remove(filePath);
                        finalViewHolder.img.setColorFilter(null);
                        finalViewHolder.select.setImageResource(R.drawable.unselected_img);
                        if(onImageClickListener!=null){
                            onImageClickListener.onImageClick(false,selectedImg.size());
                        }
                    }
                    //未被选择并且当前选择数小于最大选择数
                    else if(!selectedImg.contains(filePath)&&selectedImg.size()<mPickCount){
                        selectedImg.add(filePath);
                        finalViewHolder.img.setColorFilter(Color.parseColor("#77000000"));
                        finalViewHolder.select.setImageResource(R.drawable.selected_img);
                        if(onImageClickListener!=null){
                            onImageClickListener.onImageClick(true,selectedImg.size());
                        }
                    }
                    else if(selectedImg.size()>=mPickCount){
                        Toast.makeText(mContext,"选取图片数不能超过"+mPickCount+"张",Toast.LENGTH_SHORT).show();
                    }
                }
            });

            return convertView;
        }

        private class ViewHolder{
            ImageView img;
            ImageButton select;
        }

    public ArrayList<String> getSelectedImg() {
        return selectedImg;
    }

    public interface OnImageClickListener{
        void onImageClick(boolean isSelected,int selectedCount);
    }
}