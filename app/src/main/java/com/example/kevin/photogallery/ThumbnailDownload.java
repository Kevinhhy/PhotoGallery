package com.example.kevin.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by kevin on 2017/1/24.
 */

public class ThumbnailDownload<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownload";
    private static final int DOWN_MESSAGE = 0;

    private Boolean misQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownListener;


    public ThumbnailDownload(Handler responseHnadler) {
        super(TAG);
        mResponseHandler = responseHnadler;
    }

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownload(T target, Bitmap photo);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownListener = listener;
    }

    //该方法只是在looper运行之前初始化一个handler对象
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            //该方法是处理信息的时候才调用的
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == DOWN_MESSAGE) {
                    T target = (T) msg.obj;
                    handleRequest(target);
                    Log.i(TAG, "handlemessage");
                }
            }
        };
    }

    //在线程结束时调用此方法
    @Override
    public boolean quit() {
        misQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(DOWN_MESSAGE, target).sendToTarget();
            Log.i(TAG, "queueThumbnail");
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(DOWN_MESSAGE);
    }

    //接受url，下载缩略图
    public void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);

            if (url == null) {
                return;
            }

            byte[] bitmapBytes = new FlickerFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url || misQuit) {
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownListener.onThumbnailDownload(target, bitmap);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error downloading bitmap", e);
        }
    }
}
