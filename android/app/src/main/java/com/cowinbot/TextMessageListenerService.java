package com.example.cowintest;

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
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

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
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.example.cowintest.MainApplication.CHANNEL_ID;

public class TextMessageListenerService extends Service {
  private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
  private JSONObject userSharedPreferences = new JSONObject();
  private ScheduledExecutorService threadPool;
  private final int jobSchedulerId = 910613287;
  private RequestQueue requestQueue;
  private WorkManager workManager;
  private WorkRequest workRequest;

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
    showNotification();
    try {
      requestQueue = Volley.newRequestQueue(this);
      userSharedPreferences = new JSONObject(Objects.requireNonNull(intent.getStringExtra("preferences")));
      Log.d("Preferences", userSharedPreferences.toString());
      startWorkManager();
      // scheduleJob();
      final long refresh_interval = Long.parseLong(userSharedPreferences.getString("refresh_interval"));
      threadPool = Executors.newScheduledThreadPool(10);
      final JSONArray pincodes = userSharedPreferences.getJSONArray("pincodes");
      final JSONArray districtIds = userSharedPreferences.getJSONArray("district_ids");
      for (int i = 0; i < pincodes.length(); ++i) threadPool.scheduleWithFixedDelay(new FindVaccineAvailability(true, pincodes.getString(i)), 0, refresh_interval, TimeUnit.SECONDS);
      for (int i = 0; i < districtIds.length(); ++i) threadPool.scheduleWithFixedDelay(new FindVaccineAvailability(false, districtIds.getString(i)), 0, refresh_interval, TimeUnit.SECONDS);
    } catch (JSONException e) {
      e.printStackTrace();
      Log.d("Error", e.toString());
      stopForeground(true);
      stopSelf();
    }
    return START_REDELIVER_INTENT;
  }

  private class FindVaccineAvailability implements Runnable{
    private final Boolean is_pincode;
    private final String param;

    FindVaccineAvailability(Boolean firstIsPincode, String firstParam){
      is_pincode = firstIsPincode;
      param = firstParam;
    }

    @Override
    public void run() {
      Log.d("FindVaccineAvailability", "Checking Availability Slot...");
      findAvailableSlot(is_pincode, param);
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
    String url = cowin_base_url + "/v2/appointment/sessions/public/";
    url += (is_pincode ? "calendarByPin?pincode=" : "calendarByDistrict?district_id=") + param + ("&date=" + getCurrentDate());
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        try {
          final JSONArray centers = response.getJSONArray("centers");
          // Log.d("Centers", centers.toString());
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
        Log.d("Error Request", "That didn't work!!");
      }
    });
    requestQueue.add(jsonObjectRequest);
  }

  private Boolean checkVaccineAvailability(JSONObject session) {
    // Log.d("Session", session.toString());
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
    JobInfo info = new JobInfo.Builder(jobSchedulerId, componentName)
            .setRequiresCharging(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setPersisted(true)
            .setPeriodic(16 * 60 * 1000)
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
    scheduler.cancel(jobSchedulerId);
    Log.d("Job Scheduler", "Job cancelled");
  }

  public void startWorkManager() throws JSONException {
    workManager = WorkManager.getInstance(this);
    workRequest = new PeriodicWorkRequest.Builder(RefreshTokenWorkManager.class, 19, TimeUnit.MINUTES).setInputData(new Data.Builder().putString("mobile", userSharedPreferences.getString("mobile")).build()).build();
    workManager.enqueue(workRequest);
  }

  public void stopWorkManager(){
    workManager.cancelWorkById(workRequest.getId());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // cancelJob();
    stopWorkManager();
    threadPool.shutdownNow();
    Toast.makeText(this, "The service has been stopped successfully", Toast.LENGTH_SHORT).show();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}