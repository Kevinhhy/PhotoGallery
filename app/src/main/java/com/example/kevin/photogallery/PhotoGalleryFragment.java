package com.example.kevin.photogallery;

import android.app.ProgressDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by kevin on 2017/1/21.
 */

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";

    private static int mRecyclerViewRow = 3;

    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int mInt = 0;
    private ProgressBar mProgressBar;
    private Callback mCallback;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    public interface Callback{
        int getPages();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: fragment");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        updateItems();
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
                    if (mInt < mCallback.getPages()) {
                        mInt++;
                        Toast.makeText(getActivity(), "第" + mInt + "页", Toast.LENGTH_SHORT).show();
                        updateItems();
                        setupAdapter();

                    }
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
        setupAdapter();

        mProgressBar = (ProgressBar) v.findViewById(R.id.fragment_photo_gallery_progress_bar);
        showProgressBar(true);
        return v;

    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getActivity(),query);
                mInt = 1;
                searchItem.collapseActionView();
                updateItems();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem serviceItem = menu.findItem(R.id.menu_item_poll_service);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            if (PollService.isServiceAlarmOn(getActivity())) {
                serviceItem.setTitle(R.string.stop_polling);
            } else {
                serviceItem.setTitle(R.string.start_polling);
            }
        } else {
            Log.i(TAG, "sdk version: " + Build.VERSION.SDK_INT);
            final int JOB_ID = 1;
            if (isSchedulerCreated(JOB_ID)) {
                serviceItem.setTitle(R.string.stop_polling);
            } else {
                serviceItem.setTitle(R.string.start_polling);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_search:
                mInt = 1;
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_poll_service:
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    Log.i(TAG, "IntentService start");
                    boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                    PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                } else {
                    final int JOB_ID = 1;
                    JobScheduler schedular = (JobScheduler) getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);

                    if (isSchedulerCreated(JOB_ID)) {
                        Log.i(TAG, "Jobservice canceled");
                        schedular.cancel(JOB_ID);
                    } else {
                        Log.i(TAG, "Jobservice start");

                        JobInfo jobinfo = new JobInfo.Builder(JOB_ID, new ComponentName(getActivity(), PollJobService.class))
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                                .setPeriodic(1000 * 6)
                                .setPersisted(true)
                                .build();
                        schedular.schedule(jobinfo);
                    }
                }
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isSchedulerCreated(int id) {
        JobScheduler scheduler = (JobScheduler) getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasCreated = false;
        for (JobInfo jobinfo : scheduler.getAllPendingJobs()) {
            if (jobinfo.getId() == id) {
                hasCreated = true;
            }
        }
        QueryPreferences.setAlarmOn(getActivity(), hasCreated);
        return hasCreated;
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemTask(query, this).execute();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showProgressBar(boolean isShow) {
        if (isShow) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
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

        public void bindGalleryItem(GalleryItem item) {
            Picasso.with(getActivity())
                    .load(item.getUrl())
                    .placeholder(R.drawable.basset)
                    .into(mPhotoView);
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
            holder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        String mQuery;
        PhotoGalleryFragment mPhotoGalleryFragment;

        public FetchItemTask(String query, PhotoGalleryFragment fragment) {
            mQuery = query;
            mPhotoGalleryFragment = fragment;
        }

        @Override
        protected void onPreExecute() {
            if (mPhotoGalleryFragment.isResumed()) {
                mPhotoGalleryFragment.showProgressBar(true);
            }
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            FlickerFetcher flicker = new FlickerFetcher();
            mCallback = flicker;
            if (mQuery == null) {
                return flicker.fetchPhoto(mInt);
            }
            else {
                return flicker.searchPhoto(mQuery,mInt);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();

            mPhotoGalleryFragment.showProgressBar(false);
        }
    }
}