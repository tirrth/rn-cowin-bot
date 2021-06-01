//package com.cowinbot;
//import android.content.Intent;
//import android.os.Bundle;
//import com.facebook.react.HeadlessJsTaskService;
//import com.facebook.react.bridge.Arguments;
//import com.facebook.react.jstasks.HeadlessJsTaskConfig;
//import javax.annotation.Nullable;
//
//public class TextMessageListenerService extends HeadlessJsTaskService {
//
//  @Override
//  protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
//    Bundle extras = intent.getExtras();
//    if (extras != null) {
//      return new HeadlessJsTaskConfig(
//          "MessageListener",
//          extras != null ? Arguments.fromBundle(extras) : null,
//          5000, // timeout for the task
//          true // optional: defines whether or not the task is allowed in foreground. Default is false
//        );
//    }
//    return null;
//  }
//}

package com.cowinbot;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import static com.cowinbot.MainApplication.CHANNEL_ID;


public class TextMessageListenerService extends Service {

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    String input = intent.getStringExtra("inputExtra");

    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this,
            0, notificationIntent, 0);

    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_android)
            .setContentIntent(pendingIntent)
            .build();

    startForeground(1, notification);

    // do heavy work on a background thread
    // stopSelf();

    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}