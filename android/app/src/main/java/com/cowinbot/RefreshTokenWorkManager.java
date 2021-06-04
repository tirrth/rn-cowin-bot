package com.example.cowintest;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RefreshTokenWorkManager extends Worker {
    private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
    private final Context context;
    private String _txnId;
    private static final String TAG = "RefreshTokenJobService";
    private static final String pdu_type = "pdus";
    private BroadcastReceiver messageReceiver;
    private final WorkerParameters workerParams;
    private RequestQueue requestQueue;
    private boolean isWorkManagerRunning = true;

    public RefreshTokenWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d("Work Manager", "Work Manager started");
        this.context = context;
        this.workerParams = workerParams;
        requestQueue = Volley.newRequestQueue(context);
        final IntentFilter intentFilter = new IntentFilter();
        final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        intentFilter.addAction(SMS_RECEIVED);
        this.messageReceiver = new BroadcastReceiver() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs;
            String format = bundle.getString("format");
            // Retrieve the SMS message received.
            Object[] pdus = (Object[]) bundle.get(pdu_type);
            if (pdus != null) {
                boolean isVersionM = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
                msgs = new SmsMessage[pdus.length];
                for (int i = 0; i < msgs.length; i++) {
                    if (isVersionM) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                    } else {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
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
        context.registerReceiver(this.messageReceiver, intentFilter);
    }

    private void generateOTP(String mobile) {
        String url = cowin_base_url + "/v2/auth/public/generateOTP";
        Map<String, String> params = new HashMap();
        params.put("mobile", mobile);
        JSONObject parameters = new JSONObject(params);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    _txnId = response.getString("txnId");
                } catch (JSONException e) {
                    e.printStackTrace();
                    stopWorkManager();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Validate OTP Error", "That didn't work!!"+ " Status Code: "+ error.networkResponse.statusCode);
                stopWorkManager();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void confirmOTP(String otp) {
        String url = cowin_base_url + "/v2/auth/public/confirmOTP";
        Map<String, String> params = new HashMap();
        params.put("otp", DigestUtils.sha256Hex(otp));
        params.put("txnId", _txnId);
        JSONObject parameters = new JSONObject(params);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    SharedPreferences.Editor editor = context.getSharedPreferences("COWIN", Context.MODE_PRIVATE).edit();
                    editor.putString("token", response.getString("token"));
                    editor.apply();
                    Log.d("Access Token", response.getString("token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                stopWorkManager();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("Confirm OTP Error", "That didn't work!!");
                stopWorkManager();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void doBackgroundWork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isWorkManagerRunning) return;
                try {
                    final String mobile = workerParams.getInputData().getString("mobile");
                    generateOTP(mobile);
                    Thread.sleep(3 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopWorkManager();
            }
        }).start();
    }

    @NonNull
    @Override
    public Result doWork() {
        doBackgroundWork();
        return Result.success();
    }

    private void stopWorkManager(){
        Log.d("Work Manager", "Work Manager finished");
        unregisterBroadcastReceiver();
        isWorkManagerRunning = true;
        WorkManager.getInstance(context).cancelWorkById(workerParams.getId());
    }

    private void unregisterBroadcastReceiver(){
        try {
            context.unregisterReceiver(this.messageReceiver);
        } catch (IllegalArgumentException e){
            Log.d(TAG, "Error occurred when unregistering BroadcastReceiver");
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        isWorkManagerRunning = true;
        unregisterBroadcastReceiver();
        Log.d("Work Manager", "Work Manager cancelled before completion");
    }
}