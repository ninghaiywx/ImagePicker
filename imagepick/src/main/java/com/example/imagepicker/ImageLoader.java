package com.example.imagepicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by yue on 2017/9/4.
 * 图片加载类
 */

public class ImageLoader {
    public enum TYPE{
        FIFO,LIFO
    }

    private static ImageLoader mInstance;
    //图片缓存
    private LruCache<String,Bitmap>mCache;
    //线程池
    private ExecutorService mThreadPool;
    //队列调度方式
    private TYPE mType= TYPE.LIFO;
    //任务队列
    private LinkedList<Runnable> mTaskQueue;
    //后台轮询线程
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    private Handler mUIHandler;

    //信号量
    private Semaphore mSemaphoreThreadHandler=new Semaphore(0);
    private Semaphore mSemaphoreThread;

    private ImageLoader(int threadCount, TYPE type){
        init(threadCount,type);
    }

    /**
     * 初始化方法
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, TYPE type) {
        mPoolThread=new Thread(){
            @Override
            public void run(){
                Looper.prepare();
                mPoolThreadHandler=new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程取出一个任务去执行
                        mThreadPool.execute(getTasks());
                        try {
                            mSemaphoreThread.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                mSemaphoreThreadHandler.release();
                Looper.loop();
            }
        };

        mPoolThread.start();

        //获取应用最大内存
        int maxMemory= (int) Runtime.getRuntime().maxMemory();
        int cacheMemory=maxMemory/8;

        mCache=new LruCache<String, Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();
            }
        };

        mThreadPool=Executors.newFixedThreadPool(threadCount);
        mTaskQueue=new LinkedList<>();

        mType=type;

        mSemaphoreThread=new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个任务
     * @return
     */
    private Runnable getTasks() {
        if(mType== TYPE.FIFO){
            return mTaskQueue.removeFirst();
        }else if(mType== TYPE.LIFO){
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public static ImageLoader getInstance(int threadCount,TYPE type){
        if(mInstance==null){
            synchronized (ImageLoader.class){
                if(mInstance==null){
                    mInstance=new ImageLoader(threadCount,type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据path为imageview设置图片
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView){
        imageView.setTag(path);

        if(mUIHandler==null){
            mUIHandler=new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    //获取得到的图片设置在imageview上
                    ImageBeanHolder imageBean= (ImageBeanHolder) msg.obj;
                    Bitmap bm=imageBean.bitmap;
                    ImageView imageView=imageBean.imageView;
                    String path=imageBean.path;

                    if(imageView.getTag().toString().equals(path)){
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        //现根据path到缓存中找bitmap
        Bitmap bm=getBitmapFromCache(path);

        //如果从内存中找到了图片
        if(bm!=null) {
            refreshBitmap(bm, imageView, path);
        }else {
            addTasks(new Runnable() {
                @Override
                public void run() {
                    //加载图片并压缩
                    //1.获取图片显示的大小
                    ImageSize imageSize=getImageViewSize(imageView);
                    //2.压缩图片
                    Bitmap bm=decodeSampledBitmapFromPath(path,imageSize.width,imageSize.height);
                    //3.把图片加载到内存
                    addBitmapToCache(path,bm);

                    refreshBitmap(bm, imageView, path);

                    mSemaphoreThread.release();
                }
            });
        }
    }

    /**
     * 消息提交到handler
     * @param bm
     * @param imageView
     * @param path
     */
    private void refreshBitmap(Bitmap bm, ImageView imageView, String path) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder(imageView, path, bm);
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    /**
     * 把图片添加到内存
     * @param path
     * @param bm
     */
    private void addBitmapToCache(String path, Bitmap bm) {
        if(getBitmapFromCache(path)==null) {
            if(bm!=null) {
                mCache.put(path, bm);
            }
        }
    }

    /**
     * 压缩图片
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        //获取图片宽和高并不加载到内存中
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,options);

        options.inSampleSize=caculateInSampleSize(options,width,height);

        options.inJustDecodeBounds=false;
        Bitmap bitmap=BitmapFactory.decodeFile(path,options);
        return bitmap;
    }

    /**
     * 根据实际的宽和高和需求的宽和高计算图片压缩比例
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width=options.outWidth;
        int height=options.outHeight;

        int inSampleSize=1;

        if(width>reqWidth||height>reqHeight){
            int widthRatio=Math.round(width*1.0f/reqWidth);
            int heightRatio=Math.round(height*1.0f/reqHeight);

            inSampleSize=Math.max(widthRatio,heightRatio);
        }

        return inSampleSize;
    }

    /**
     * 根据imageview获取适当宽和高
     * @param imageView
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize=new ImageSize();
        ViewGroup.LayoutParams lp=imageView.getLayoutParams();
        //获取Imageview宽度
        int width=imageView.getWidth();
        //如果获取不到，从设置的参数中获取
        if(width<=0){
            width=lp.width;
        }
        //如果还获取不到，就等于屏幕宽度
        if(width<=0){
            width=imageView.getContext().getResources().getDisplayMetrics().widthPixels;
        }
        //与宽度相同的获取方法
        int height=imageView.getWidth();
        if(height<=0){
            height=lp.width;
        }
        if(height<=0){
            height=imageView.getContext().getResources().getDisplayMetrics().heightPixels;
        }

        imageSize.width=width;
        imageSize.height=height;
        return imageSize;
    }

    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if(mPoolThreadHandler==null) {
                mSemaphoreThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 根据Path在缓存中获取Bitmap
     * @param key 就是图片path
     * @return
     */
    private Bitmap getBitmapFromCache(String key) {
        return mCache.get(key);
    }

    private class ImageBeanHolder{
        ImageView imageView;
        String path;
        Bitmap bitmap;
        public ImageBeanHolder(ImageView imageView,String path,Bitmap bitmap){
            this.imageView=imageView;
            this.path=path;
            this.bitmap=bitmap;
        }
    }

    private class ImageSize{
        int width,height;
    }
}
