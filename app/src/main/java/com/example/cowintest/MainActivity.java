package com.example.cowintest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.cowintest.adapter.RecyclerViewBeneficiaryAdapter;
import com.example.cowintest.adapter.RecyclerViewVaccinationCenterAdapter;
import com.example.cowintest.model.VaccinationCenterModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements RecyclerViewVaccinationCenterAdapter.OnVaccinationCenterListener {
    private static final int PERMISSION_ALL_PERMISSION_CODE = 1;
    private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
    private BroadcastReceiver messageReceiver;
    private RequestQueue requestQueue;
    private RecyclerView recyclerBeneficiaryView;
    private RecyclerViewBeneficiaryAdapter recyclerBeneficiaryViewAdapter;
    private RecyclerView recyclerCenterView;
    private RecyclerViewVaccinationCenterAdapter recyclerCenterViewAdapter;
    CustomDialog loadingDialog = new CustomDialog(MainActivity.this, R.layout.custom_loading_dialog, false);
    CustomDialog addDistrictDialog = new CustomDialog(MainActivity.this, R.layout.add_district_dialog, true);
    JSONArray beneficiaryList = new JSONArray();
    private AutoCompleteTextView autoCompleteStateDropdown;
    private AutoCompleteTextView autoCompleteDistrictDropdown;
    private EditText mobileEditText;
    Button mobileVerificationButton;
    JSONObject _selectedState;

    public interface VolleyCallback{
        void onSuccess(JSONObject result);
    }

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
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity();
            }
        }, 2000);
    }

    public void startActivity(){
        requestQueue = Volley.newRequestQueue(this);
        if (getCowinToken() == "") getStates();
        else getBeneficiaries();
    }

    public void getStates(){
        String url = cowin_base_url + "/v2/admin/location/states";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray states = response.optJSONArray("states");
                setContentView(R.layout.activity_verification);
                setStateDropdown(states);
                requestPermissions();
                mobileEditText = (EditText) findViewById(R.id.mobileTextInput);
                mobileVerificationButton = (Button) findViewById(R.id.mobileVerificationButton);
                mobileEditText.addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        verificationActivityValidations();
                    }
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                });
                autoCompleteStateDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        _selectedState = states.optJSONObject(i);
                        verificationActivityValidations();
                    }
                });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error Request", "That didn't work!!");
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    public void verificationActivityValidations(){
        if(mobileEditText.getText().length() == 10 && _selectedState != null) mobileVerificationButton.setEnabled(true);
        else mobileVerificationButton.setEnabled(false);
    }

    public void setStateDropdown(JSONArray states){
        ArrayList<String> statesArrayList = new ArrayList<String>();
        for(int i = 0; i < states.length(); i++){
            statesArrayList.add(states.optJSONObject(i).optString("state_name"));
        }
        ArrayAdapter stateAdapter = new ArrayAdapter(this, R.layout.dropdown_tem, statesArrayList);
        autoCompleteStateDropdown = (AutoCompleteTextView)findViewById(R.id.autoCompleteStateTextView);
        autoCompleteStateDropdown.setAdapter(stateAdapter);
    }

    public void setDistrictDropdown(JSONArray districts){
        ArrayList<String> statesArrayList = new ArrayList<String>();
        for(int i = 0; i < districts.length(); i++){
            statesArrayList.add(districts.optJSONObject(i).optString("district_name"));
        }
        ArrayAdapter stateAdapter = new ArrayAdapter(this, R.layout.dropdown_tem, statesArrayList);
        autoCompleteDistrictDropdown = (AutoCompleteTextView) addDistrictDialog.customDialogView.findViewById(R.id.autoCompleteDistrictTextView);
        autoCompleteDistrictDropdown.setAdapter(stateAdapter);
    }

    public void setStatusBarColor(final int color){
        getWindow().setStatusBarColor(color);
    }

    public void verifyMobileNumber(View v) throws JSONException {
        loadingDialog.startLoadingDialog();
        registerBroadcastReceiver();
        final String mobileNumber = mobileEditText.getText().toString();
        JSONObject userInfo = new JSONObject();
        userInfo.put("mobile", mobileNumber);
        userInfo.put("state_info", _selectedState);
        setUserInfo(userInfo);
        generateOTP(mobileNumber);
    }

    private void registerBroadcastReceiver(){
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
                    // setContentView(R.layout.activity_main);
                    getBeneficiaries();
                    Log.d("OTP Confirmation Token", response.getString("token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    loadingDialog.dismissDialog();
                }
                unregisterBroadcastReceiver();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("Confirm OTP Error", "That didn't work!!");
                loadingDialog.dismissDialog();
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

    private String getMobileNumber() {
        return getUserInfo().optString("mobile");
    }

    private JSONObject getUserInfo() {
        try {
            SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
            return new JSONObject(preferences.getString("user_info", "{}"));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private JSONObject getStateInfo(){
        return getUserInfo().optJSONObject("state_info");
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

    private void setUserInfo(final JSONObject userInfo) {
        SharedPreferences.Editor editor = getSharedPreferences("COWIN", MODE_PRIVATE).edit();
        editor.putString("user_info", userInfo.toString());
        editor.apply();
    }

    private void getBeneficiaries(){
        String url = cowin_base_url + "/v2/appointment/beneficiaries";
        final String token = getCowinToken();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Response", response.toString());
                // setContentView(R.layout.activity_main);
                try {
                    setContentView(R.layout.beneficiary_list);
                    beneficiaryList = response.getJSONArray("beneficiaries");
                    setBeneficiaryRecyclerView(beneficiaryList);
                    loadingDialog.dismissDialog();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error Request", "That didn't work!!");
                if(error != null &&  error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    loadingDialog.startLoadingDialog();
                    registerBroadcastReceiver();
                    generateOTP(getMobileNumber());
                }
                else{
                    Log.d("Info", "Something went wrong..");
                }
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

    private void setBeneficiaryRecyclerView(JSONArray beneficiaries) throws JSONException {
        JSONArray newArray = new JSONArray();
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        newArray.put(beneficiaries.optJSONObject(0));
        recyclerBeneficiaryView = findViewById(R.id.beneficiaryRecyclerView);
        recyclerBeneficiaryView.setHasFixedSize(true);
        recyclerBeneficiaryView.setLayoutManager(new LinearLayoutManager(this));
        recyclerBeneficiaryViewAdapter = new RecyclerViewBeneficiaryAdapter(MainActivity.this, newArray);
        recyclerBeneficiaryView.setAdapter(recyclerBeneficiaryViewAdapter);
    }

    private void setVaccinationCenterRecyclerView(JSONArray centers) {
        recyclerCenterView.setHasFixedSize(true);
        recyclerCenterView.setLayoutManager(new LinearLayoutManager(this));
        recyclerCenterViewAdapter = new RecyclerViewVaccinationCenterAdapter(getCentersList(centers), this);
        recyclerCenterView.setAdapter(recyclerCenterViewAdapter);
    }

    public List<VaccinationCenterModel> getCentersList(JSONArray centers) {
        ArrayList mVaccinationCentersList = new ArrayList<>();
        for (int i = 0; i < centers.length(); i++) {
            mVaccinationCentersList.add(new VaccinationCenterModel(centers.optJSONObject(i)));
        }
        return mVaccinationCentersList;
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

    public void gotoBotPreferences(View v) {
        CircularProgressIndicator botFabProgressBar = (CircularProgressIndicator) findViewById(R.id.fabProgress);
        botFabProgressBar.setVisibility(View.VISIBLE);
        v.setVisibility(View.GONE);
        final String state_id = getStateInfo().optString("state_id");
        Log.d("State Id", state_id);
        String url = cowin_base_url + "/v2/admin/location/districts/" + state_id;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("District Information", response.toString());
                // setContentView(R.layout.bot_preferences);
                RelativeLayout fabView = (RelativeLayout) findViewById(R.id.fabView);
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetTheme);
                View bottomSheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.bot_preferences_bottom_sheet, (LinearLayout)findViewById(R.id.botPreferencesContainer));
                ImageView addDistrictImageView = (ImageView) bottomSheetView.findViewById(R.id.addDistrictImageView);
                addDistrictImageView.setOnClickListener(v1 -> onAddDistrict(response.optJSONArray("districts")));
                bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) { fabView.setVisibility(View.VISIBLE); }
                });
                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();
                fabView.setVisibility(View.GONE);
                v.setVisibility(View.VISIBLE);
                botFabProgressBar.setVisibility(View.GONE);
            }
        }, error -> Log.d("Error Request", "That didn't work!!"));
        requestQueue.add(jsonObjectRequest);
    }

    public void onAddDistrict(JSONArray districts) {
        addDistrictDialog.startLoadingDialog();
        setDistrictDropdown(districts);
        final TextView districtErrorMessageView = (TextView) addDistrictDialog.customDialogView.findViewById(R.id.districtDialogErrorMessageTextView);
        final ProgressBar districtDialogResourceLoader = (ProgressBar) addDistrictDialog.customDialogView.findViewById(R.id.districtDialogResourceLoader);
        recyclerCenterView = addDistrictDialog.customDialogView.findViewById(R.id.centerRecyclerView);
        autoCompleteDistrictDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                recyclerCenterView.setVisibility(View.GONE);
                districtErrorMessageView.setVisibility(View.GONE);
                districtDialogResourceLoader.setVisibility(View.VISIBLE);
                try {
                    final String districtId = districts.getJSONObject(i).optString("district_id");
                    // Log.d("Selected District", districts.getJSONObject(i).toString());
                    getCenters(districtId, getTomorrowDate(), response -> {
                        Log.d("Center Results", response.toString());
                        setVaccinationCenterRecyclerView(response.optJSONArray("centers"));
                        districtDialogResourceLoader.setVisibility(View.GONE);
                        recyclerCenterView.setVisibility(View.VISIBLE);
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getCenters(final String districtId, final String date, final VolleyCallback callback){
        String url = cowin_base_url + "/v2/appointment/sessions/public/calendarByDistrict";
        url += "?district_id=" + districtId + "&date=" + date;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("District Information", response.toString());
                callback.onSuccess(response);
            }
        }, error -> Log.d("Error Request", "That didn't work!!"));
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onVaccinationCenterClick(int position, VaccinationCenterModel selectedCenter) {
        Log.d("Weelll", "Its working");
        // Log.d("selectedCenter", selectedCenter.getAddress());
        Log.d("selectedCenter", selectedCenter.getName());
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

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, TextMessageListenerService.class);
        stopService(serviceIntent);
    }

    public void logOut(View v){
        SharedPreferences.Editor editor = getSharedPreferences("COWIN", MODE_PRIVATE).edit();
        editor.clear().apply();
        startActivity();
    }
}