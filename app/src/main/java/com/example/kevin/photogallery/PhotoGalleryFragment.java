package com.example.kevin.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: fragment");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();

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

    private void setupAdapter() {
        if (isAdded()){
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private TextView mTiletTextView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mTiletTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item) {
            mTiletTextView.setText(item.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
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