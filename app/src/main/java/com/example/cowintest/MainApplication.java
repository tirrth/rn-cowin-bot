package com.example.cowintest;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MainApplication extends Application {
    public static final String NOTIFICATION_SERVICE_CHANNEL_ID = "CoWinBotServiceChannel";
    public static final String NOTIFICATION_INFO_CHANNEL_ID = "CoWinBotInfoChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_SERVICE_CHANNEL_ID, "CoWIN Bot Channel", NotificationManager.IMPORTANCE_HIGH);
            serviceChannel.setDescription("This is CoWin Bot Notification Channel");
            NotificationChannel serviceInfoChannel = new NotificationChannel(NOTIFICATION_INFO_CHANNEL_ID, "CoWIN Bot Channel", NotificationManager.IMPORTANCE_HIGH);
            serviceInfoChannel.setDescription("This is CoWin Bot Information Channel");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(serviceInfoChannel);
        }
    }
}
