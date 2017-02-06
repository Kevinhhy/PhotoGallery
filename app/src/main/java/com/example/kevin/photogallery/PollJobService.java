package com.example.kevin.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by kevin on 2017/2/3.
 */

public class PollJobService extends JobService {
    private static final String TAG = "PollJobService";
    public static final String ACTION_SHOW_NOTIFICATION = "com.example.kevin.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.example.kevin.photogallery.PRIVATE";
    public static final String RESULT_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    private PollTask mPollTask;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "onStartJob");
        mPollTask = new PollTask();
        mPollTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mPollTask != null) {
            mPollTask.cancel(true);
        }
        return false;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, Void> {
        @Override
        protected Void doInBackground(JobParameters... params) {
            Log.i(TAG, "doInBackground");
            JobParameters parameters = params[0];

            if (!isNetworkAvailableAndConnected()) {
                return null;
            }
            String query = QueryPreferences.getStoredQuery(PollJobService.this);
            List<GalleryItem> items;
            int page = 1;

            if (query == null) {
                items = new FlickerFetcher().fetchPhoto(page);
            } else {
                items = new FlickerFetcher().searchPhoto(query, page);
            }

            if (items.size() == 0) {
                return null;
            }

            String lastResultId = QueryPreferences.getPreLastResultId(PollJobService.this);
            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.i(TAG, "No new picture");
            } else {
                Log.i(TAG, "Got new pictures");
                Intent i = PhotoGalleryActivity.newIntent(PollJobService.this);
                PendingIntent pi = PendingIntent.getActivity(PollJobService.this, 0, i, 0);

                Notification notification = new NotificationCompat.Builder(PollJobService.this)
                        .setTicker(getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(getString(R.string.new_pictures_title))
                        .setContentText(getString(R.string.new_pictures_text))
                        .setVibrate(new long[]{100, 100, 100, 100})
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                showBackgroundNotification(0, notification);
            }
            QueryPreferences.setLastResultId(PollJobService.this, resultId);
            jobFinished(parameters, false);
            return null;
        }

        private void showBackgroundNotification(int requestCode, Notification notification) {
            Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
            i.putExtra(RESULT_CODE, requestCode);
            i.putExtra(NOTIFICATION, notification);
            sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
        }

        private boolean isNetworkAvailableAndConnected() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

            boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
            boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
            return isNetworkConnected;
        }

    }
}
