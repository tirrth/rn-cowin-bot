package com.cowinbot;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import static com.cowinbot.MainApplication.CHANNEL_ID;

public class TextMessageListenerService extends Service {
  private Boolean _stop;
  private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
  private String txnId;
  private String token;
  private static final String TAG = TextMessageListenerService.class.getSimpleName();
  public static final String pdu_type = "pdus";
  private BroadcastReceiver messageReceiver;

  @Override
  public void onCreate() {
    super.onCreate();
    final IntentFilter intentFilter = new IntentFilter();
    final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
    intentFilter.addAction(SMS_RECEIVED);
    this.messageReceiver = new BroadcastReceiver() {
      @TargetApi(Build.VERSION_CODES.M)
      @Override
      public void onReceive(Context context, Intent intent) {
        // Get the SMS message.
        // if(intent.getAction().equals(SMS_RECEIVED)){ }
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String format = bundle.getString("format");
        // Retrieve the SMS message received.
        Object[] pdus = (Object[]) bundle.get(pdu_type);
        if (pdus != null) {
          // Check the Android version.
          boolean isVersionM = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
          // Fill the msgs array.
          msgs = new SmsMessage[pdus.length];
          for (int i = 0; i < msgs.length; i++) {
            // Check Android version and use appropriate createFromPdu.
            if (isVersionM) {
              // If Android version M or newer:
              msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            } else {
              // If Android version L or older:
              msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            }
            // Build the message to show.
            final String originatingAddress = msgs[i].getOriginatingAddress();
            final String messageBody = msgs[i].getMessageBody();
            if(originatingAddress.equals("JD-NHPSMS") && messageBody.contains("Your OTP to register/access CoWIN is")){
              String otp = messageBody.split(" ")[6].substring(0, 6);
              Log.d(TAG, "OTP is: " + otp);
              Toast.makeText(context, ("OTP is: " + otp), Toast.LENGTH_SHORT).show();
              confirmOTP(otp);
            }
          }
        }
      }
    };
    this.registerReceiver(this.messageReceiver, intentFilter);
    _stop = false;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
    String input = intent.getStringExtra("inputExtra");
    showNotification(input);

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        Log.d("Check Running", "Start to Run");
        try {
          generateOTP("9106132870");
          Thread.sleep(30000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
//        while(!_stop){
//          try{
//            Log.d("API Calling", "Calling API...");
//            // findAvailableSlot(true, "382350");
//            generateOTP("9106132870");
//            Thread.sleep(20000);
//          }
//          catch (InterruptedException e){
//            e.printStackTrace();
//          } catch (Exception e) {
//            e.printStackTrace();
//          }
//        }
        stopForeground(true);
        stopSelf();
      }
    });
    thread.start();
    Log.d("Current Date", getCurrentDate());

    return START_REDELIVER_INTENT;
  }

  private void showNotification(String input){
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
  }

  private void generateOTP(String mobile) {
    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
//    final String secret = "U2FsdGVkX1/KEwnJ85xsqldOZj5kzZ1XBbEIzX51gnPjc0jzOwg7hCzjPA9Or/UxVml4du1tu4mxx7RK+L2Hdw==";
    String url = cowin_base_url + "/v2/auth/public/generateOTP";
    Log.d("url", url);
    Map<String, String> params = new HashMap();
    params.put("mobile", mobile);
//    params.put("secret", secret);
    JSONObject parameters = new JSONObject(params);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        try {
          txnId = response.getString("txnId");
          Log.d("OTP Generated", response.getString("txnId"));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        Log.d("Validate OTP Error", "That didn't work!!"+ " Status Code: "+ error.networkResponse.statusCode);
      }
    });
    queue.add(jsonObjectRequest);
  }

  private void confirmOTP(String otp) {
    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
    String url = cowin_base_url + "/v2/auth/public/confirmOTP";
    Map<String, String> params = new HashMap();
    params.put("otp", DigestUtils.sha256Hex(otp));
    params.put("txnId", txnId);
    JSONObject parameters = new JSONObject(params);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        try {
          token = response.getString("token");
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
//    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//     jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    queue.add(jsonObjectRequest);
  }

  private void findAvailableSlot(Boolean is_pincode, String param){
    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
    String url = cowin_base_url + "/v2/appointment/sessions/public/" + (is_pincode ? "calendarByPin": "calendarByDistrict");
    url += ((is_pincode ? "?pincode=" : "?district_id=") + param) + ("&date=" + getCurrentDate());
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        Log.d("Success Request", response.toString());
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        if(error.networkResponse.statusCode == 401)
        Log.d("Error Request", "That didn't work!!"+ " Status Code: "+ error.networkResponse.statusCode);
      }
    });
    queue.add(jsonObjectRequest);
  }

  private String getCurrentDate(){
    SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
    return date.toString();
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
    this.unregisterReceiver(this.messageReceiver);
    Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    _stop = true;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}