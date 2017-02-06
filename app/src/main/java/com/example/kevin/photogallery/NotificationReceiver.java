package com.example.kevin.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

/**
 * Created by kevin on 2017/2/6.
 */

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (getResultCode() != Activity.RESULT_OK) {
            return;
        }
        int requestCod = intent.getIntExtra(PollJobService.RESULT_CODE, 0);
        Notification notification = intent.getParcelableExtra(PollJobService.NOTIFICATION);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify(requestCod, notification);
    }
}
