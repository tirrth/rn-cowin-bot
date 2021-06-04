package com.cowinbot;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.cowinbot.MainApplication.CHANNEL_ID;

public class TextMessageListenerService extends Service {
  public static Boolean _stop = false;
  private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
  private JSONObject userSharedPreferences = new JSONObject();
  private ScheduledExecutorService threadPool;

  @Override
  public void onCreate() {
    super.onCreate();
    // _stop = false;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
    showNotification();
    try {
      userSharedPreferences = new JSONObject(Objects.requireNonNull(intent.getStringExtra("preferences")));
      scheduleJob();
    } catch (JSONException e){
      e.printStackTrace();
      Log.d("Error", e.toString());
    }
    
    // Thread thread = new Thread(new Runnable() {
    //   @Override
    //   public void run() {
    //     while(!_stop){
    //       try{
    //         final int refresh_interval = Integer.parseInt(userSharedPreferences.getString("refresh_interval"));
    //         findAvailableSlot(true, "382350");
    //         Thread.sleep(refresh_interval * 1000);
    //       } catch (Exception e){
    //         e.printStackTrace();
    //       }
    //     }
    //     stopForeground(true);
    //     stopSelf();
    //   }
    // });
    // thread.start();

    try {
      final long refresh_interval = Long.parseLong(userSharedPreferences.getString("refresh_interval"));
      threadPool = Executors.newScheduledThreadPool(10);
      threadPool.scheduleWithFixedDelay(new Task(), 0, refresh_interval, TimeUnit.SECONDS);
    } catch (JSONException e) {
      e.printStackTrace();
      stopForeground(true);
      stopSelf();
    }
    return START_REDELIVER_INTENT;
  }

  private class Task implements Runnable{
    @Override
    public void run() {
      findAvailableSlot(true, "382350");
    }
  }

  private void showNotification(){
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
    broadcastIntent.putExtra("toastMessage", "The service has been stopped successfully");
    PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_android)
            .setContentTitle("Foreground Service")
            .setContentText("It is working")
            .setColor(Color.BLUE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(R.mipmap.ic_launcher, "Stop", actionIntent)
            .build();
    startForeground(1, notification);
  }

  private void findAvailableSlot(Boolean is_pincode, String param){
    String url = cowin_base_url + "/v2/appointment/sessions/";
    url += (is_pincode ? "calendarByPin?pincode=" : "calendarByDistrict?district_id=") + param + ("&date=" + getCurrentDate());
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        try {
          final JSONArray centers = response.getJSONArray("centers");
          for (int i = 0; i < centers.length(); ++i) {
            final JSONObject center = centers.getJSONObject(i);
            final JSONArray sessions = center.getJSONArray("sessions");
            for (int j = 0; j < sessions.length(); ++j) {
              final JSONObject session = sessions.getJSONObject(j);
              checkVaccineAvailability(session);
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // if(error.networkResponse.statusCode == 401)
        Log.d("Error Request", "That didn't work!!");
      }
    }){
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        final String token = getCowinToken();
        params.put("Authorization", token);
        return params;
      }
    };
    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
    queue.add(jsonObjectRequest);
  }

  private Boolean checkVaccineAvailability(JSONObject session) {
    Log.d("Session", session.toString());
    return false;
  }

  private String getCowinToken(){
    SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
    return preferences.getString("token", "");
  }

  private void bookSlot(){
    final String token = getCowinToken();
    Log.d("Device Token", token);
  }

  private String getCurrentDate(){
    Date date = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    return formatter.format(date);
  }

  public void scheduleJob() throws JSONException {
    PersistableBundle bundle = new PersistableBundle();
    bundle.putString("mobile", userSharedPreferences.getString("mobile"));
    ComponentName componentName = new ComponentName(this, RefreshTokenJobService.class);
    JobInfo info = new JobInfo.Builder(910613287, componentName)
            .setRequiresCharging(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setPersisted(true)
            .setPeriodic(19 * 60 * 1000)
            .setExtras(bundle)
            .build();
    JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
    scheduler.cancelAll();
    int resultCode = scheduler.schedule(info);
    if (resultCode == JobScheduler.RESULT_SUCCESS) {
      Log.d("Job Scheduler", "Job scheduled");
    } else {
      Log.d("Job Scheduler", "Job scheduling failed");
    }
  }

  public void cancelJob() {
    JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
    scheduler.cancel(910613287);
    Log.d("Job Scheduler", "Job cancelled");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    cancelJob();
    threadPool.shutdownNow();
    Toast.makeText(this, "The service has been stopped successfully", Toast.LENGTH_SHORT).show();
    // _stop = true;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}