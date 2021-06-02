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
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsMessage;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import static com.cowinbot.MainApplication.CHANNEL_ID;

public class TextMessageListenerService extends Service {
  private Boolean _stop;
  private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
  private String txnId;
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
        String strMessage = "";
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
            strMessage += "SMS from " + msgs[i].getOriginatingAddress();
            strMessage += " :" + msgs[i].getMessageBody() + "\n";
            // Log and display the SMS message.
            Log.d(TAG, "onReceive: " + strMessage);
            Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show();
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
        while(!_stop){
          try{
            Log.d("API Calling", "Calling API...");
//            findAvailableSlot(true, "382350");
            generateOTP("9106132870");
            Thread.sleep(20000);
          }
          catch (InterruptedException e){
            e.printStackTrace();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        stopForeground(true);
        stopSelf();
      }
    });
    thread.start();
    Log.d("Current Date", getCurrentDate());

    // do heavy work on a background thread
    // stopSelf();
    //    Handler handler = new Handler();
    //    final Runnable r = new Runnable() {
    //      public void run() {
    //        callAPI();
    //        if(!_stop) handler.postDelayed(this, 5000);
    //      }
    //    };
    //    if(!_stop) handler.postDelayed(r, 5000);

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
    final String secret = "U2FsdGVkX1/KEwnJ85xsqldOZj5kzZ1XBbEIzX51gnPjc0jzOwg7hCzjPA9Or/UxVml4du1tu4mxx7RK+L2Hdw==";
    String url = cowin_base_url + "/v2/auth/generateMobileOTP";
    Map<String, String> params = new HashMap();
    params.put("mobile", mobile);
    params.put("secret", secret);
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
        Log.d("Error Request", "That didn't work!!"+ " Status Code: "+ error.networkResponse.statusCode);
      }
    });
    queue.add(jsonObjectRequest);
  }

  private void confirmOTP(String txnId, String otp) throws NoSuchAlgorithmException {
    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
    byte[] message = otp.getBytes();
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(message);
    String url = cowin_base_url + "/v2/auth/validateMobileOtp";
    Map<String, String> params = new HashMap();
    params.put("txnId", txnId);
    params.put("otp", digest.toString());
    JSONObject parameters = new JSONObject(params);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        Log.d("OTP Validated", response.toString());
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        Log.d("Error Request", "That didn't work!!"+ " Status Code: "+ error.networkResponse.statusCode);
      }
    });
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