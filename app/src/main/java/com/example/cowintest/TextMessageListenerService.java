package com.example.cowintest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.example.cowintest.MainApplication.NOTIFICATION_INFO_CHANNEL_ID;
import static com.example.cowintest.MainApplication.NOTIFICATION_SERVICE_CHANNEL_ID;

public class TextMessageListenerService extends Service {
  private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
  private JSONObject userSharedPreferences = new JSONObject();
  private static final String WORKER_TAG_SYNC_DATA = "CowinWorkManager";
  private static final String SYNC_DATA_WORK_NAME = "CowinRefreshTokenWork";
  private static ScheduledExecutorService threadPool;
  private static NetworkInfo activeNetworkInfo;
  private RequestQueue requestQueue;
  private WorkManager workManager;
  private PeriodicWorkRequest workRequest;
  private BroadcastReceiver messageReceiver;
  private NotificationManagerCompat notificationManagerCompat;
  static ScheduledFuture<?> t;
  private static String isPreviousNetworkStatusConnected;
  private static int total_api_calls_count = 0;
  private Intent notificationIntent;
  private PendingIntent pendingNotifyIntent;
  private Intent broadcastNotifyIntent;
  private PendingIntent actionIntent;
  private Notification notification;

  @Override
  public void onCreate() {
    super.onCreate();
    notificationManagerCompat = NotificationManagerCompat.from(this);
    requestQueue = Volley.newRequestQueue(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
    try {
      userSharedPreferences = new JSONObject(Objects.requireNonNull(intent.getStringExtra("preferences")));
      initializeForegroundNotification();
      registerBroadcastReceiver();
    } catch (JSONException e) {
      e.printStackTrace();
      stopService();
    }
    return START_REDELIVER_INTENT;
  }

  private void registerBroadcastReceiver(){
    isPreviousNetworkStatusConnected = "";
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
            final String isNetworkStatusConnected = String.valueOf(isNetworkConnected() && internetIsConnected());
            Log.d("NetworkStatusConnected", isNetworkStatusConnected);
            Log.d("PreviousNetwork", isPreviousNetworkStatusConnected);
            if(isNetworkStatusConnected.equals(isPreviousNetworkStatusConnected)) return;
            isPreviousNetworkStatusConnected = isNetworkStatusConnected;
            try {
              if (Boolean.parseBoolean(isNetworkStatusConnected)) {
                sendForegroundNotification("CoWIN Bot Running...","Count: " + total_api_calls_count + "   Refresh-Rate: 3s", Color.argb(1,66,133,244));
                startBotActivity();
              } else {
                stopBotActivity();
                sendForegroundNotification("CoWIN Bot Stopped!","No Internet Connection at the Moment", Color.argb(1,219,68,55));
                // new java.util.Timer().schedule(
                //   new java.util.TimerTask() {
                //     @Override
                //     public void run() {
                //       sendForegroundNotification("CoWIN Bot Stopped!","No Internet Connection at the Moment", Color.argb(1,219,68,55));
                //     }
                //   }, 2000
                // );
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
                // Toast.makeText(context, ("OTP is: " + otp), Toast.LENGTH_SHORT).show();
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

  public boolean internetIsConnected() {
    try {
      String command = "ping -c 1 google.com";
      return (Runtime.getRuntime().exec(command).waitFor() == 0);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isNetworkConnected(){
    if(activeNetworkInfo == null) return false;
    return activeNetworkInfo.isConnected();
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
      total_api_calls_count++;
      if(isBotActivityRunning()) sendForegroundNotification("CoWIN Bot Running...","Count: " + total_api_calls_count + "   Refresh-Rate: 3s", Color.argb(1,66,133,244));
      findAvailableSlot(is_pincode, param);
    }
  }

  private void initializeForegroundNotification(){
    notificationIntent = new Intent(this, MainActivity.class);
    pendingNotifyIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    broadcastNotifyIntent = new Intent(this, NotificationReceiver.class);
    actionIntent = PendingIntent.getBroadcast(this, 0, broadcastNotifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    // broadcastNotifyIntent.putExtra("toastMessage", "The service has been stopped successfully");
  }

  private void sendForegroundNotification(final String contentTitle, final String contentText, final int color){
    notification = new NotificationCompat.Builder(this, NOTIFICATION_SERVICE_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification_logo)
      .setContentTitle(contentTitle)
      .setContentText(contentText)
      .setColor(color)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setContentIntent(pendingNotifyIntent)
      .setAutoCancel(true)
      .setOnlyAlertOnce(true)
      .addAction(R.drawable.ic_cancel, "Stop", actionIntent)
      .build();
    startForeground(1, notification);
  }

  private void sendInformationNotification(){
    Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_INFO_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification_logo)
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
              if (checkVaccineAvailability(session) && isBotActivityRunning()) {
                // stopBotActivity();
                // Log.d("Separator", "--------------------------------------------------------------------");
                // Log.d("This slot is Available", session.toString());
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
      .addTag(WORKER_TAG_SYNC_DATA)
      .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
      .build();
    workManager = WorkManager.getInstance(this);
    workManager.enqueueUniquePeriodicWork(SYNC_DATA_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
  }

  public void stopWorkManager(){
    // workManager.cancelWorkById(workRequest.getId());
    // workManager.cancelAllWorkByTag(WORKER_TAG_SYNC_DATA);
    workManager.cancelAllWork();
    workManager = null;
  }

  private void unregisterBroadcastReceiver(){
    try {
      total_api_calls_count = 0;
      isPreviousNetworkStatusConnected = "";
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

  private void startBotActivity() throws JSONException {
    startWorkManager();
    startThreadPool();
  }

  private boolean isBotActivityRunning(){
    return isWorkScheduled() && isThreadPoolScheduled();
  }

  private void stopBotActivity() {
    // Log.d("isWorkScheduled", "" + isWorkScheduled());
    // Log.d("isThreadPoolScheduled",  "" + isThreadPoolScheduled());
    if(isWorkScheduled()) stopWorkManager();
    if(isThreadPoolScheduled()) stopThreadPool();
  }

  private boolean isThreadPoolScheduled(){
    if(threadPool == null) return false;
    return !threadPool.isTerminated();
  }

  private boolean isWorkScheduled() {
    if (workManager == null) return false;
    return true; // Not the best practice, but couldn't find anything else on the internet...
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