package com.example.cowintest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

import static com.example.cowintest.MainApplication.NOTIFICATION_SERVICE_CHANNEL_ID;

public class NotificationReceiver extends BroadcastReceiver {
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        Intent serviceIntent = new Intent(context, TextMessageListenerService.class);
        // sendForegroundNotification("CoWIN Bot Stopping...", "Count: 890   Refresh-Rate: 3s", Color.argb(1,66,133,244));
        context.stopService(serviceIntent);
    }

    private void sendForegroundNotification(final String contentTitle, final String contentText, final int color){
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingNotifyIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingNotifyIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }
}