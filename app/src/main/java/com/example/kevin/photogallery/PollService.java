package com.example.kevin.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.List;

/**
 * Created by kevin on 2017/2/3.
 */

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    private static final int SERVICE_INTERVAL = 1000 * 60;

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public PollService() {
        super(TAG);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (isOn) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), SERVICE_INTERVAL, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);

        return pi != null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getPreLastResultId(this);
        List<GalleryItem> items;

        if (query == null) {
            items = new FlickerFetcher().fetchPhoto(1);
        } else {
            items = new FlickerFetcher().searchPhoto(query, 1);
        }

        if (items.size() == 0) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {

        } else {
            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.notify(0, notification);
        }

        QueryPreferences.setLastResultId(this, resultId);
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }
}
