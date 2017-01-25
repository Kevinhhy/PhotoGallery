package com.example.kevin.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;

/**
 * Created by kevin on 2017/1/21.
 */

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int mInt = 1;
    private static int mRecyclerViewRow = 3;
    private ThumbnailDownload<PhotoHolder> mThumbnailDownload;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: fragment");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();

        Handler responseHandler = new Handler();
        mThumbnailDownload = new ThumbnailDownload<>(responseHandler);
        mThumbnailDownload.setThumbnailDownloadListener(new ThumbnailDownload.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownload(PhotoHolder target, Bitmap photo) {
                Drawable drawable = new BitmapDrawable(getResources(), photo);
                target.bindImage(drawable);
            }
        });
        mThumbnailDownload.start();
        mThumbnailDownload.getLooper();

    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("pause");

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        final GridLayoutManager manager = new GridLayoutManager(getActivity(), mRecyclerViewRow);
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                System.out.println("change");
                mRecyclerViewRow = mRecyclerView.getLayoutManager().getWidth() / 360;
                manager.setSpanCount(mRecyclerViewRow);

            }
        });

        mRecyclerView.setLayoutManager(manager);

        System.out.println("manager");



        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                View bottom = recyclerView.getLayoutManager().getChildAt(recyclerView.getLayoutManager().getChildCount() - 1);

                int lastChildBottom = bottom.getBottom();
                int recyclerBottom = recyclerView.getBottom() - recyclerView.getPaddingBottom();
                int lastPosition = recyclerView.getLayoutManager().getPosition(bottom);

                if (recyclerBottom == lastChildBottom && lastPosition == recyclerView.getLayoutManager().getItemCount() - 1) {
                    mInt++;
                    Toast.makeText(getActivity(), "第" + mInt + "页", Toast.LENGTH_SHORT).show();
                    new FetchItemTask().execute();
                    setupAdapter();
                }
//                View top = recyclerView.getLayoutManager().getChildAt(0);
//
//                int topChild = top.getTop();
//                int topRecycler = recyclerView.getTop() - recyclerView.getPaddingTop();
//                int firstPosition = recyclerView.getLayoutManager().getPosition(top);

//                if (mInt != 1) {
//                    if (topChild == topRecycler && firstPosition == 0) {
//                    Toast.makeText(getActivity(), "滑动到顶了", Toast.LENGTH_SHORT).show();
//                    new FetchItemTask().execute();
//                    setupAdapter();
//                    mInt--;
//                    }
//                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        setupAdapter();
        System.out.println("view");
        return v;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownload.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownload.quit();
        mThumbnailDownload.clearCache();
    }

    private void setupAdapter() {
        if (isAdded()){
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mPhotoView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mPhotoView = (ImageView) itemView.findViewById(R.id.fragment_photo_image_view);
        }

        public void bindImage(Drawable drawable) {
            mPhotoView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(v);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Bitmap bitmap = mThumbnailDownload.getCacheImage(galleryItem.getUrl());

            if (bitmap == null) {
                Drawable drawable = getResources().getDrawable(R.drawable.bill_up_close);
                holder.bindImage(drawable);
                System.out.println("bind");
                mThumbnailDownload.queueThumbnail(holder, galleryItem.getUrl());
            } else {
                holder.bindImage(new BitmapDrawable(getResources(), bitmap));
            }
            preloadImage(position);
        }

        private void preloadImage(int position) {
            final int imageBufferSize = 10;

            int startPosition = Math.max(position - imageBufferSize, 0);
            int endPosition = Math.max(position + imageBufferSize, mGalleryItems.size());

            for (int i = startPosition; i <= endPosition; i++) {
                if (i == position) {
                    continue;
                }

                mThumbnailDownload.preloadImage(mGalleryItems.get(position).getUrl());
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

            return new FlickerFetcher().fetchItems(mInt);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();
        }
    }
}