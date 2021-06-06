package com.example.cowintest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.example.cowintest.MainApplication.NOTIFICATION_INFO_CHANNEL_ID;
import static com.example.cowintest.MainApplication.NOTIFICATION_SERVICE_CHANNEL_ID;

public class TextMessageListenerService extends Service {
  private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
  private JSONObject userSharedPreferences = new JSONObject();
  private final String workManagerTag = "CowinRefreshTokenWorkManager";
  private static ScheduledExecutorService threadPool;
  private static NetworkInfo activeNetworkInfo;
  private RequestQueue requestQueue;
  private WorkManager workManager;
  private PeriodicWorkRequest workRequest;
  private BroadcastReceiver messageReceiver;
  private NotificationManagerCompat notificationManagerCompat;
  static ScheduledFuture<?> t;

  @Override
  public void onCreate() {
    super.onCreate();
    notificationManagerCompat = NotificationManagerCompat.from(this);
    workManager = WorkManager.getInstance(this);
    requestQueue = Volley.newRequestQueue(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
    try {
      userSharedPreferences = new JSONObject(Objects.requireNonNull(intent.getStringExtra("preferences")));
      registerBroadcastReceiver();
    } catch (JSONException e) {
      e.printStackTrace();
      stopService();
    }
    return START_REDELIVER_INTENT;
  }

  private void registerBroadcastReceiver(){
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
    intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
    intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    this.messageReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(context == null || intent == null || intent.getAction() == null) return;
        switch (intent.getAction()){
          case ConnectivityManager.CONNECTIVITY_ACTION: {
            Log.d("Connectivity Manager", "Connectivity Changed");
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            try {
              if (!isNetworkConnected()) {
                sendForegroundNotification("Internet Connectivity is Gone");
                stopBotActivity();
              } else {
                sendForegroundNotification("It is working");
                startBotActivity();
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
            break;
          }
          case Telephony.Sms.Intents.SMS_RECEIVED_ACTION: {
            SmsMessage[] textMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            for (int i = 0; i < textMessages.length; i++) {
              Log.d("Text Message Address", textMessages[i].getOriginatingAddress());
              Log.d("Text Message Body", textMessages[i].getMessageBody());
              final String originatingAddress = textMessages[i].getOriginatingAddress();
              final String messageBody = textMessages[i].getMessageBody();
              if (originatingAddress.contains("NHPSMS") && messageBody.contains("Your OTP to register/access CoWIN is")) {
                String otp = messageBody.split(" ")[6].substring(0, 6);
                Log.d("MessageListenerService", "OTP is: " + otp);
                Toast.makeText(context, ("OTP is: " + otp), Toast.LENGTH_SHORT).show();
                confirmOTP(otp);
              }
            }
            break;
          }
        }
      }
    };
    this.registerReceiver(this.messageReceiver, intentFilter);
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
      findAvailableSlot(is_pincode, param);
    }
  }

  private void sendForegroundNotification(final String contentText){
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
    broadcastIntent.putExtra("toastMessage", "The service has been stopped successfully");
    PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_SERVICE_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_android)
      .setContentTitle("Foreground Service")
      .setContentText(contentText)
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

  private void sendInformationNotification(){
    Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_INFO_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_android)
      .setContentTitle("CoWIN Bot")
      .setContentText("Congratulations!! Your vaccination slot has been registered successfully.")
      .setColor(Color.BLUE)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setAutoCancel(true)
      .setOnlyAlertOnce(true)
      .build();
    notificationManagerCompat.notify(2, notification);
  }

  private void confirmOTP(String otp) {
    String url = cowin_base_url + "/v2/auth/validateMobileOtp";
    final String _txnId = getCowinTxnId();
    Map<String, String> params = new HashMap();
    params.put("otp", DigestUtils.sha256Hex(otp));
    params.put("txnId", _txnId);
    JSONObject parameters = new JSONObject(params);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        try {
          setCowinToken(response.getString("token"));
          Log.d("OTP Confirmation Token", response.getString("token"));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        error.printStackTrace();
        Log.d("Confirm OTP Error", "That didn't work!!");
      }
    }){
      @Override
      public Map<String, String> getHeaders () throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Origin", "https://selfregistration.cowin.gov.in"); // Just to make cowin's backend API happy and so to get some response but no Timeout Error.... Any base_url on the internet can be written here, even the local urls(http://localhost:<port>).
        return params;
      }
    };
    requestQueue.add(jsonObjectRequest);
  }

  private void findAvailableSlot(Boolean is_pincode, String param){
    String url = cowin_base_url + "/v2/appointment/sessions/public/";
    url += (is_pincode ? "calendarByPin?pincode=" : "calendarByDistrict?district_id=") + param + ("&date=" + getTomorrowDate());
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
              Log.d("isBotActivityRunning", "" + isBotActivityRunning());
              if (checkVaccineAvailability(session) && isBotActivityRunning()) {
                stopBotActivity();
                Log.d("Separator", "--------------------------------------------------------------------");
                Log.d("This slot is Available", session.toString());
                final String sessionId = session.getString("session_id");
                String preferredSlot = session.getJSONArray("slots").length() > 0 ? session.getJSONArray("slots").getString(0) : "";
                final int dose = 1;
                final int centerId = center.getInt("center_id");
                final JSONArray beneficiaries = new JSONArray();
                beneficiaries.put("84481602933410");
                final JSONObject parameters = new JSONObject();
                parameters.put("center_id", centerId);
                parameters.put("dose", dose);
                parameters.put("session_id", sessionId);
                parameters.put("slot", preferredSlot);
                parameters.put("beneficiaries", beneficiaries);
                // bookSlot(parameters);
              }
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

  private Boolean checkVaccineAvailability(JSONObject session) throws JSONException {
    if(session.getInt("available_capacity") > 0 && session.getInt("available_capacity_dose1") > 0 && session.getInt("min_age_limit") == 18) return true;
    return false;
  }

  private String getCowinToken(){
    SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
    return preferences.getString("token", "");
  }

  private String getCowinTxnId(){
    SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
    return preferences.getString("txnId", "");
  }

  private void setCowinToken(final String token) {
    SharedPreferences.Editor editor = getSharedPreferences("COWIN", MODE_PRIVATE).edit();
    editor.putString("token", token);
    editor.apply();
  }

  private void bookSlot(JSONObject parameters) {
    Log.d("CoWIN Token", getCowinToken());
    Log.d("Separator", "--------------------------------------------------------------------");
    // Log.d("Slot booking parameters", parameters.toString());
    String url = cowin_base_url + "/v2/appointment/schedule";
    final String token = getCowinToken();
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        Log.d("Last Message of the day", response.toString());
        sendInformationNotification();
        stopForeground(true);
        stopSelf();
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        error.printStackTrace();
        Log.d("Last Error", error.toString());
        Log.d("Last Error Message", "That didn't work!!");
        // startThreadPool();
        // if(error != null && error.networkResponse != null){ if(error.networkResponse.statusCode == 400){ } }
      }
    }){
      @Override
      public Map<String, String> getHeaders () throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Content-Type", "application/json; charset=UTF-8");
        params.put("Authorization", "Bearer ".concat(token));
        return params;
      }
    };
    requestQueue.add(jsonObjectRequest);
  }

  private String getTomorrowDate(){
    Date date = new Date();
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DATE, 1);
    date = c.getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    return formatter.format(date);
  }

  public void startWorkManager() throws JSONException {
    final Data data = new Data.Builder().putString("mobile", userSharedPreferences.getString("mobile")).build();
    final Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    workRequest = new PeriodicWorkRequest.Builder(RefreshTokenWorkManager.class, 19, TimeUnit.MINUTES)
      .setInputData(data)
      .setConstraints(constraints)
      .addTag(workManagerTag)
      .setInitialDelay(0, TimeUnit.MINUTES)
      .build();
    // workManager.enqueue(workRequest);
    workManager.enqueueUniquePeriodicWork(workManagerTag, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
  }

  public void stopWorkManager(){
    // workManager.cancelWorkById(workRequest.getId());
    workManager.cancelAllWorkByTag(workManagerTag);
  }

  private void unregisterBroadcastReceiver(){
    try {
      this.unregisterReceiver(this.messageReceiver);
    } catch (IllegalArgumentException e){
      Log.d("Broadcast Listener", "Error occurred when unregistering BroadcastReceiver");
    }
  }

  private void startThreadPool() throws JSONException {
    final long refresh_interval = Long.parseLong(userSharedPreferences.getString("refresh_interval"));
    final JSONArray pincodes = userSharedPreferences.getJSONArray("pincodes");
    final JSONArray districtIds = userSharedPreferences.getJSONArray("district_ids");
    final int corePoolSize = pincodes.length() + districtIds.length();
    threadPool = Executors.newScheduledThreadPool(corePoolSize);
    for (int i = 0; i < pincodes.length(); ++i) t = threadPool.scheduleWithFixedDelay(new FindVaccineAvailability(true, pincodes.getString(i)), 0, refresh_interval, TimeUnit.SECONDS);
    for (int i = 0; i < districtIds.length(); ++i) t = threadPool.scheduleWithFixedDelay(new FindVaccineAvailability(false, districtIds.getString(i)), 0, refresh_interval, TimeUnit.SECONDS);
  }

  private void stopThreadPool(){
    t.cancel(true); // Just to increase the probability/possibility of interrupting the Thread Pool to stop the parallel execution...
    threadPool.shutdownNow();
  }

  private boolean isNetworkConnected(){
    if(activeNetworkInfo == null) return false;
    return activeNetworkInfo.isConnected();
  }

  private void startBotActivity() throws JSONException {
    startWorkManager();
    startThreadPool();
  }

  private boolean isBotActivityRunning(){
    return isWorkScheduled(workManagerTag) && isThreadPoolScheduled();
  }

  private void stopBotActivity() {
    Log.d("isWorkScheduled", "" + isWorkScheduled(workManagerTag));
    Log.d("isThreadPoolScheduled", "" + isThreadPoolScheduled());
    if(isWorkScheduled(workManagerTag)) stopWorkManager();
    if(isThreadPoolScheduled()) stopThreadPool();
  }

  private boolean isThreadPoolScheduled(){
    if(threadPool == null) return false;
    return !threadPool.isTerminated();
  }

  private boolean isWorkScheduled(String tag) {
    if(workManager == null) return false;
    ListenableFuture<List<WorkInfo>> statuses = workManager.getWorkInfosByTag(tag);
    try {
      boolean running = false;
      List<WorkInfo> workInfoList = statuses.get();
      for (WorkInfo workInfo : workInfoList) {
        WorkInfo.State state = workInfo.getState();
        Log.d("Work Manager State", state.toString());
        running = (state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED);
      }
      return running;
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }

  private void stopService(){
    stopForeground(true);
    stopSelf();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterBroadcastReceiver();
    stopBotActivity();
    Toast.makeText(this, "The service has been stopped successfully", Toast.LENGTH_SHORT).show();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}