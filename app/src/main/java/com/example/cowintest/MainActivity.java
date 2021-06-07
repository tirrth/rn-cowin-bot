package com.example.cowintest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_ALL_PERMISSION_CODE = 1;
    private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
    private BroadcastReceiver messageReceiver;
    private RequestQueue requestQueue;
    private String _txnId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(Color.WHITE);
        setContentView(R.layout.activity_splash);
        LottieAnimationView lottieAnimationView = findViewById(R.id.splash_animation);
        Integer[] animations = {R.raw.covid_anim_1, R.raw.covid_anim_2, R.raw.covid_anim_3, R.raw.covid_anim_4, R.raw.covid_anim_5};
        final int random_anim_id =  animations[generateRandomInteger(0, animations.length - 1)];
        Log.d("Random Anim Id", "" + random_anim_id);
        lottieAnimationView.enableMergePathsForKitKatAndAbove(true);
        lottieAnimationView.setAnimation(random_anim_id);
        requestQueue = Volley.newRequestQueue(this);
        if (getCowinToken() == ""){
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    setContentView(R.layout.activity_verification);
                    requestPermissions();
                    EditText mobileEditText = (EditText) findViewById(R.id.mobileTextInput);
                    Button mobileVerificationButton = (Button) findViewById(R.id.mobileVerificationButton);
                    mobileEditText.addTextChangedListener(new TextWatcher() {
                        public void afterTextChanged(Editable s) {
                            if(s.length() == 10) mobileVerificationButton.setEnabled(true);
                            else mobileVerificationButton.setEnabled(false);
                        }
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    });
                }
            }, 3000);
        } else getBeneficiaries();
        // SharedPreferences.Editor editor = getSharedPreferences("COWIN", MODE_PRIVATE).edit();
        // editor.putString("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiI1ZGY3YjE4MC02YTcyLTRiOTYtYWQxNy01ZmE0MjcwMTVmY2EiLCJ1c2VyX3R5cGUiOiJCRU5FRklDSUFSWSIsInVzZXJfaWQiOiI1ZGY3YjE4MC02YTcyLTRiOTYtYWQxNy01ZmE0MjcwMTVmY2EiLCJtb2JpbGVfbnVtYmVyIjo5MTA2MTMyODcwLCJiZW5lZmljaWFyeV9yZWZlcmVuY2VfaWQiOjg0NDgxNjAyOTMzNDEwLCJ0eG5JZCI6IjJmZjFlZTJlLWFkOGItNGFiMy1hNTI5LTMyYjdmMmQ4YWUzZSIsImlhdCI6MTYyMjg4NTk1NywiZXhwIjoxNjIyODg2ODU3fQ.4kN6oMT5GMK-H_tP0EW-mQS5UeDs1YijQtiS855K_YU");
        // editor.apply();
    }

    public void setStatusBarColor(final int color){
        getWindow().setStatusBarColor(color);
    }

    public void verifyMobileNumber(View view){
        LoadingDialog loadingDialog = new LoadingDialog(MainActivity.this);
        loadingDialog.startLoadingDialog();

        // Initializing Broadcast Receiver
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(context == null || intent == null || intent.getAction() == null) return;
                switch (intent.getAction()){
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
        EditText mobileEditText = (EditText) findViewById(R.id.mobileTextInput);
        generateOTP(mobileEditText.getText().toString());
    }

    private void generateOTP(String mobile) {
        String url = cowin_base_url + "/v2/auth/generateMobileOTP";
        Map<String, String> params = new HashMap();
        params.put("mobile", mobile);
        params.put("secret", "U2FsdGVkX19VJvTgYTEgcrIAfZFL0wjV7lBCWux4KQNdW5hcE6aiY/DTsagJWhoeJJhWVu0xBVXDOkIWwoqn7g==");
        JSONObject parameters = new JSONObject(params);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    setCowinTxnId(response.getString("txnId"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Validate OTP Error", "That didn't work!!");
            }
        });
        requestQueue.add(jsonObjectRequest);
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
                unregisterBroadcastReceiver();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("Confirm OTP Error", "That didn't work!!");
                unregisterBroadcastReceiver();
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

    public void startService(View v) throws JSONException {
        Intent serviceIntent = new Intent(this, TextMessageListenerService.class);
        JSONArray pincodes = new JSONArray();
        pincodes.put("382350");
        pincodes.put("382345");
        pincodes.put("380050");
        pincodes.put("380045");
        pincodes.put("382323");
        JSONArray district_ids = new JSONArray();
        district_ids.put("154");
        district_ids.put("174");
        district_ids.put("158");
        district_ids.put("175");
        district_ids.put("181");
        JSONObject preferences = new JSONObject();
        preferences.put("mobile", "9106132870");
        preferences.put("refresh_interval", "3");
        preferences.put("pincodes", pincodes);
        preferences.put("district_ids", district_ids);
        serviceIntent.putExtra("preferences", preferences.toString());
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, TextMessageListenerService.class);
        stopService(serviceIntent);
    }

    private void requestPermissions(){
        String[] PERMISSIONS = { Manifest.permission.RECEIVE_SMS };
        if (!hasPermissions(this, PERMISSIONS)) ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL_PERMISSION_CODE);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) for (String permission : permissions) if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    // This function is called when user accept or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == PERMISSION_ALL_PERMISSION_CODE) && (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) return;
    }

    private void unregisterBroadcastReceiver(){
        try {
            this.unregisterReceiver(this.messageReceiver);
        } catch (IllegalArgumentException e){
            Log.d("Broadcast Listener", "Error occurred when unregistering BroadcastReceiver");
        }
    }

    private String getCowinToken(){
        SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
        return preferences.getString("token", "");
    }

    private String getCowinTxnId(){
        SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
        return preferences.getString("txnId", "");
    }

    private void setCowinTxnId(final String txnId) {
        SharedPreferences.Editor editor = getSharedPreferences("COWIN", Context.MODE_PRIVATE).edit();
        editor.putString("txnId", txnId);
        editor.apply();
    }

    private void setCowinToken(final String token) {
        SharedPreferences.Editor editor = getSharedPreferences("COWIN", MODE_PRIVATE).edit();
        editor.putString("token", token);
        editor.apply();
    }

    private void getBeneficiaries(){
        String url = cowin_base_url + "/v2/appointment/beneficiaries";
        final String token = getCowinToken();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Response", response.toString());
                setContentView(R.layout.activity_main);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error Request", "That didn't work!!");
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

    private int generateRandomInteger(int min, int max){
        return (int)Math.floor(Math.random() * (max - min + 1) + min);
    }
}