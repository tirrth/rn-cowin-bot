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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static com.cowinbot.MainApplication.CHANNEL_ID;


public class TextMessageListenerService extends Service {
  private boolean _stop = false;

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

    Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
    startForeground(1, notification);

    // do heavy work on a background thread
    // stopSelf();
    Handler handler = new Handler();
    final Runnable r = new Runnable() {
      public void run() {
        callAPI();
        if(!_stop) handler.postDelayed(this, 5000);
      }
    };
    if(!_stop) handler.postDelayed(r, 5000);
    //    Handler handler = new Handler();
    //    Thread thread = new Thread(new Runnable(){
    //      public void run() {
    //        try {
    //          while(true) {
    //            Thread.sleep(5000);
    //            handler.post(this);
    //          }
    //        } catch (InterruptedException e) {
    //          e.printStackTrace();
    //        }
    //        while(true)
    //        {
    //          try {
    //            Thread.sleep(5000);
    //            handler.post(this);
    //            // Instantiate the RequestQueue.
    //            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
    //            String url ="http://localhost:3000/exchange-rates/latest";
    //            // Request a string response from the provided URL.
    //            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
    //                    new Response.Listener<String>() {
    //                      @Override
    //                      public void onResponse(String response) {
    //                        // Display the first 500 characters of the response string.
    //                        // textView.setText("Response is: "+ response.substring(0,500));
    //                      }
    //                    }, new Response.ErrorListener() {
    //              @Override
    //              public void onErrorResponse(VolleyError error) {
    //              // textView.setText("That didn't work!");
    //              }
    //            });
    //            // Add the request to the RequestQueue.
    //            queue.add(stringRequest);
    //          } catch (InterruptedException e) {
    //            e.printStackTrace();
    //          }
    //          //REST OF CODE HERE//
    //        }
    //      }
    //    });
    //    thread.start();

    return START_NOT_STICKY;
  }

  private void callAPI(){
    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
    String url = "http://192.168.0.103:3000/exchange-rates/latest";
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
      (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
          Log.d("Success Request", response.toString());
        }
      }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
          // TODO: Handle error
          Log.d("Error Request", "That didn't work!!");
        }
      });
    queue.add(jsonObjectRequest);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    _stop = true;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}