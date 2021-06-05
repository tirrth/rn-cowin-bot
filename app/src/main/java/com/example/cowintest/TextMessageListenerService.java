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
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
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
import java.util.concurrent.TimeUnit;

import static com.example.cowintest.MainApplication.NOTIFICATION_INFO_CHANNEL_ID;
import static com.example.cowintest.MainApplication.NOTIFICATION_SERVICE_CHANNEL_ID;

public class TextMessageListenerService extends Service {
  private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
  // private static String cowin_token;
  private JSONObject userSharedPreferences = new JSONObject();
  private static ScheduledExecutorService threadPool;
  // private static ScheduledFuture<?>[] scheduledFuture;
  private final int jobSchedulerId = 910613287;
  private RequestQueue requestQueue;
  private WorkManager workManager;
  private WorkRequest workRequest;
  private BroadcastReceiver messageReceiver;
  private static final String pdu_type = "pdus";
  private NotificationManagerCompat notificationManagerCompat;
  // private boolean _isVaccineAvailable = false;

  @Override
  public void onCreate() {
    super.onCreate();
    notificationManagerCompat = NotificationManagerCompat.from(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
    sendForegroundNotification();
    registerBroadcastReceiver();
    try {
      requestQueue = Volley.newRequestQueue(this);
      userSharedPreferences = new JSONObject(Objects.requireNonNull(intent.getStringExtra("preferences")));
      Log.d("Preferences", userSharedPreferences.toString());
      // scheduleJob();
      startWorkManager();
      final long refresh_interval = Long.parseLong(userSharedPreferences.getString("refresh_interval"));
      final JSONArray pincodes = userSharedPreferences.getJSONArray("pincodes");
      final JSONArray districtIds = userSharedPreferences.getJSONArray("district_ids");
      final int corePoolSize = pincodes.length() + districtIds.length();
      threadPool = Executors.newScheduledThreadPool(corePoolSize);
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

  private void registerBroadcastReceiver(){
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
    intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
    this.messageReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(context == null || intent == null || intent.getAction() == null) return;
        if(intent.getAction() != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return;
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
        // if(originatingAddress.equals("JD-NHPSMS") && messageBody.contains("Your OTP to register/access CoWIN is")){
        // Log.d("Short Way", textMessages[0].getDisplayOriginatingAddress());
        // Log.d("Short Way", textMessages[0].getServiceCenterAddress());
        // Log.d("Short Way", textMessages[0].getOriginatingAddress());
        // Log.d("Short Way", textMessages[0].getMessageBody());
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

  private void sendForegroundNotification(){
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
    broadcastIntent.putExtra("toastMessage", "The service has been stopped successfully");
    PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_SERVICE_CHANNEL_ID)
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

  private void sendInformationNotification(){
    Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_INFO_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_android)
      .setContentTitle("CoWIN Bot")
      .setContentText("Congratulations!! Your slot has been registered successfully.")
      .setColor(Color.BLUE)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setAutoCancel(true)
      .setOnlyAlertOnce(true)
      .build();
    notificationManagerCompat.notify(2, notification);
  }

  private void confirmOTP(String otp) {
    String url = cowin_base_url + "/v2/auth/public/confirmOTP";
    final String _txnId = getCowinTxnId();
    Map<String, String> params = new HashMap();
    params.put("otp", DigestUtils.sha256Hex(otp));
    params.put("txnId", _txnId);
    JSONObject parameters = new JSONObject(params);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        try {
          // SharedPreferences.Editor editor = getSharedPreferences("COWIN", Context.MODE_PRIVATE).edit();
          // editor.putString("token", response.getString("token"));
          // editor.apply();
          setCowinToken(response.getString("token"));
          Log.d("Access Token", response.getString("token"));
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
    });
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
          // Log.d("Centers", centers.toString());
          for (int i = 0; i < centers.length(); ++i) {
            final JSONObject center = centers.getJSONObject(i);
            final JSONArray sessions = center.getJSONArray("sessions");
            for (int j = 0; j < sessions.length(); ++j) {
              final JSONObject session = sessions.getJSONObject(j);
              if (checkVaccineAvailability(session) && !threadPool.isTerminated()){
                // _isVaccineAvailable = true;
                threadPool.shutdownNow();
                // for (int k = 0; k < scheduledFuture.length; ++k) scheduledFuture[k].cancel(true);
                Log.d("Separator", "--------------------------------------------------------------------");
                Log.d("This slot is Available", session.toString());
                final String sessionId = session.getString("session_id");
                String preferredSlot = session.getJSONArray("slots").length() > 0 ? session.getJSONArray("slots").getString(0) : "";
                final int dose = 1;
                final int centerId = center.getInt("center_id");
                // final String beneficiaries[] = new String[1];
                // beneficiaries[0] = "84481602933410";
                final JSONArray beneficiaries = new JSONArray();
                beneficiaries.put("84481602933410");
                final JSONObject parameters = new JSONObject();
                parameters.put("center_id", centerId);
                parameters.put("dose", dose);
                parameters.put("session_id", sessionId);
                parameters.put("slot", preferredSlot);
                parameters.put("beneficiaries", beneficiaries);
                // Log.d("Param", param);
                // Log.d("Parameters", parameters.toString());
                bookSlot(parameters);
              }
              // else{
              //   Log.d("Thread Pool", "Thread pool has been terminated");
              // }
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
    // return cowin_token;
  }

  private String getCowinTxnId(){
    SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
    return preferences.getString("txnId", "");
  }

  private void setCowinToken(final String token) {
    SharedPreferences.Editor editor = getSharedPreferences("COWIN", MODE_PRIVATE).edit();
    editor.putString("token", token);
    editor.apply();
    // cowin_token = token;
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
        // stopForeground(true);
        // stopSelf();
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        error.printStackTrace();
        Log.d("Last Error", error.toString());
        Log.d("Last Error Message", "That didn't work!!");
        sendInformationNotification();
        // stopForeground(true);
        // stopSelf();
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

  public void scheduleJob() throws JSONException {
    PersistableBundle bundle = new PersistableBundle();
    bundle.putString("mobile", userSharedPreferences.getString("mobile"));
    ComponentName componentName = new ComponentName(this, RefreshTokenJobService.class);
    JobInfo info = new JobInfo.Builder(jobSchedulerId, componentName)
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
    scheduler.cancel(jobSchedulerId);
    Log.d("Job Scheduler", "Job cancelled");
  }

  public void startWorkManager() throws JSONException {
    workManager = WorkManager.getInstance(this);
    final Data data = new Data.Builder().putString("mobile", userSharedPreferences.getString("mobile")).build();
    workRequest = new PeriodicWorkRequest.Builder(RefreshTokenWorkManager.class, 19, TimeUnit.MINUTES)
            .setInputData(data)
            .setInitialDelay(3, TimeUnit.MINUTES)
            .build();
    workManager.enqueue(workRequest);
  }

  public void stopWorkManager(){
    workManager.cancelWorkById(workRequest.getId());
  }

  private void unregisterBroadcastReceiver(){
    try {
      this.unregisterReceiver(this.messageReceiver);
    } catch (IllegalArgumentException e){
      Log.d("Broadcast Listener", "Error occurred when unregistering BroadcastReceiver");
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterBroadcastReceiver();
    threadPool.shutdownNow();
    // cancelJob();
    stopWorkManager();
    Toast.makeText(this, "The service has been stopped successfully", Toast.LENGTH_SHORT).show();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}