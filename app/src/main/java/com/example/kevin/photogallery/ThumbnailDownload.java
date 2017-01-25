package com.example.kevin.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
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
    private static final int PRELOAD_MESSAGE = 1;

    private Boolean misQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownListener;
    private LruCache<String, Bitmap> mLruCache;


    public ThumbnailDownload(Handler responseHnadler) {
        super(TAG);
        mResponseHandler = responseHnadler;
        mLruCache = new LruCache<String, Bitmap>(16384);
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
                switch (msg.what) {
                    case DOWN_MESSAGE:
                        T target = (T) msg.obj;
                        handleRequest(target);
                        Log.i(TAG, "handlemessage");
                        break;
                    case PRELOAD_MESSAGE:
                        String url = (String) msg.obj;
                        downloadImage(url);
                        break;
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

    public void preloadImage(String url) {
        mRequestHandler.obtainMessage(PRELOAD_MESSAGE, url).sendToTarget();
    }

    public Bitmap getCacheImage(String url) {
        return mLruCache.get(url);
    }

    public void clearCache() {
        mLruCache.evictAll();
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(DOWN_MESSAGE);
    }

    //接受url，下载缩略图
    private void handleRequest(final T target) {
        final Bitmap bitmap;

        final String url = mRequestMap.get(target);

        if (url == null) {
            return;
        }

        bitmap = downloadImage(url);
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
    }

    private Bitmap downloadImage(String url) {
        Bitmap bitmap;

        if (url == null) {
            return null;
        }

        bitmap = mLruCache.get(url);
        if (bitmap != null) {
            return bitmap;
        }

        try {
            byte[] bitmapBytes = new FlickerFetcher().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            mLruCache.put(url, bitmap);
            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error downloading photo: ",e);
            return null;
        }
    }
}

