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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.cowintest.adapter.RecyclerViewBeneficiaryAdapter;
import com.example.cowintest.adapter.RecyclerViewPreferredBeneficiaryAdapter;
import com.example.cowintest.adapter.RecyclerViewPreferredDistrictAdapter;
import com.example.cowintest.adapter.RecyclerViewPreferredPincodeAdapter;
import com.example.cowintest.adapter.RecyclerViewVaccinationCenterAdapter;
import com.example.cowintest.model.BeneficiaryModel;
import com.example.cowintest.model.VaccinationCenterModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements RecyclerViewPreferredDistrictAdapter.OnPreferredDistrictListener, RecyclerViewPreferredPincodeAdapter.OnPreferredPincodeListener, RecyclerViewPreferredBeneficiaryAdapter.OnPreferredBeneficiaryListener {
    private static final int PERMISSION_ALL_PERMISSION_CODE = 1;
    private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
    private BroadcastReceiver messageReceiver;
    private RequestQueue requestQueue;
    private RecyclerView recyclerBeneficiaryView;
    private RecyclerViewBeneficiaryAdapter recyclerBeneficiaryViewAdapter;
    private RecyclerView recyclerCenterView;
    private RecyclerViewVaccinationCenterAdapter recyclerCenterViewAdapter;
    private RecyclerView recyclerPreferredDistrictView;
    private RecyclerViewPreferredDistrictAdapter recyclerViewPreferredDistrictAdapter;
    private RecyclerView recyclerPreferredPincodeView;
    private RecyclerViewPreferredPincodeAdapter recyclerViewPreferredPincodeAdapter;
    private RecyclerView recyclerPreferredBeneficiaryView;
    private RecyclerViewPreferredBeneficiaryAdapter recyclerViewPreferredBeneficiaryAdapter;
    CustomDialog loadingDialog = new CustomDialog(MainActivity.this, R.layout.custom_loading_dialog, false);
    CustomDialog addDistrictDialog = new CustomDialog(MainActivity.this, R.layout.add_district_dialog, true);
    CustomDialog addPincodeDialog = new CustomDialog(MainActivity.this, R.layout.add_pincode_dialog, true);
    JSONArray beneficiaryList = new JSONArray();
    private AutoCompleteTextView autoCompleteStateDropdown;
    private AutoCompleteTextView autoCompleteDistrictDropdown;
    private EditText mobileEditText;
    Button mobileVerificationButton;
    JSONObject _selectedState;
    JSONArray _preferredDistrictList = new JSONArray();
    JSONArray _preferredPincodeList = new JSONArray();
    boolean _isPreferredAge45Plus;
    int _preferredDoseNumber;
    List<Integer> _preferredVaccines = new ArrayList<>(3);
    BottomSheetDialog preferenceBottomSheetDialog;
    View preferencesBottomSheetView;

    public interface VolleyCallback {
        void onSuccess(JSONObject result);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(Color.WHITE);
        setContentView(R.layout.activity_splash);
        LottieAnimationView lottieAnimationView = findViewById(R.id.splash_animation);
        // Integer[] animations = {R.raw.covid_anim_1, R.raw.covid_anim_2, R.raw.covid_anim_3, R.raw.covid_anim_4, R.raw.covid_anim_5};
        // final int random_anim_id =  animations[generateRandomInteger(0, animations.length - 1)];
        // Log.d("Random Anim Id", "" + random_anim_id);
        LocationTracker locationTracker = new LocationTracker(this);
        if(!locationTracker.canGetLocation() || locationTracker.isGPSEnabled){
            locationTracker.showSettingsAlert();
        }
        Log.d("Postal Code", "" + locationTracker.getPostalCode());
        lottieAnimationView.enableMergePathsForKitKatAndAbove(true);
        lottieAnimationView.setAnimation(R.raw.covid_anim_2);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity();
            }
        }, 2000);
    }

    public void startActivity(){
        requestQueue = Volley.newRequestQueue(this);
        if (getCowinToken().equals("")) getStates();
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
                autoCompleteStateDropdown.setOnItemClickListener((adapterView, view, i, l) -> {
                    _selectedState = states.optJSONObject(i);
                    verificationActivityValidations();
                });
            }
        }, error -> Log.d("Error Request", "That didn't work!!"));
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
        ArrayAdapter stateAdapter = new ArrayAdapter(this, R.layout.dropdown_item, statesArrayList);
        autoCompleteStateDropdown = (AutoCompleteTextView)findViewById(R.id.autoCompleteStateTextView);
        autoCompleteStateDropdown.setAdapter(stateAdapter);
    }

    public void setDistrictDropdown(JSONArray districts){
        ArrayList<String> statesArrayList = new ArrayList<String>();
        for(int i = 0; i < districts.length(); i++){
            statesArrayList.add(districts.optJSONObject(i).optString("district_name"));
        }
        ArrayAdapter stateAdapter = new ArrayAdapter(this, R.layout.dropdown_item, statesArrayList);
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
                    // Log.d("response", response.toString());
                    setCowinTxnId(response.getString("txnId"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, error -> Log.d("Validate OTP Error", "That didn't work!!"));
        requestQueue.add(jsonObjectRequest);
    }

    private void confirmOTP(String otp) {
        String url = cowin_base_url + "/v2/auth/validateMobileOtp";
        final String _txnId = getCowinTxnId();
        Map<String, String> params = new HashMap();
        params.put("otp", new String(Hex.encodeHex(DigestUtils.sha256(otp))));
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
        String[] PERMISSIONS = { Manifest.permission.RECEIVE_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
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

    private void getBeneficiaries() {
        String url = cowin_base_url + "/v2/appointment/beneficiaries";
        final String token = getCowinToken();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Response", response.toString());
                try {
                    setContentView(R.layout.beneficiary_list);
                    beneficiaryList = response.getJSONArray("beneficiaries");
                    setBeneficiaryRecyclerView(beneficiaryList);
                    loadingDialog.dismissDialog();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, error -> {
            Log.d("Beneficiary Error Request", "That didn't work!!");
            if(error != null &&  error.networkResponse != null) Log.d("Beneficiary Status Code", "" + error.networkResponse.statusCode);
            if(error != null &&  error.networkResponse != null && error.networkResponse.statusCode == 401) {
                loadingDialog.startLoadingDialog();
                registerBroadcastReceiver();
                generateOTP(getMobileNumber());
            }
            else Log.d("Info", "Something went wrong..");
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
        recyclerCenterViewAdapter = new RecyclerViewVaccinationCenterAdapter(getCenterList(centers));
        recyclerCenterView.setAdapter(recyclerCenterViewAdapter);
    }

    private void setPreferredDistrictRecyclerView(JSONArray preferredDistricts){
        recyclerPreferredDistrictView.setHasFixedSize(true);
        recyclerPreferredDistrictView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPreferredDistrictAdapter = new RecyclerViewPreferredDistrictAdapter(preferredDistricts, this);
        recyclerPreferredDistrictView.setAdapter(recyclerViewPreferredDistrictAdapter);
    }

    private void setPreferredPincodeRecyclerView(JSONArray preferredPincodes){
        recyclerPreferredPincodeView.setHasFixedSize(true);
        recyclerPreferredPincodeView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPreferredPincodeAdapter = new RecyclerViewPreferredPincodeAdapter(preferredPincodes, this);
        recyclerPreferredPincodeView.setAdapter(recyclerViewPreferredPincodeAdapter);
    }

    public void setPreferredBeneficiaryRecyclerView(JSONArray preferredBeneficiary){
        recyclerPreferredBeneficiaryView.setHasFixedSize(true);
        recyclerPreferredBeneficiaryView.setLayoutManager(new LinearLayoutManager(this));
        // recyclerViewPreferredBeneficiaryAdapter = new RecyclerViewPreferredBeneficiaryAdapter(getBeneficiaryList(preferredBeneficiary), this);
        List<BeneficiaryModel> beneficiaryList = getBeneficiaryList(preferredBeneficiary);
        List<BeneficiaryModel> ageGroupFilteredBeneficiaryList = getAgeGroupFilteredBeneficiaries(beneficiaryList);
        List<BeneficiaryModel> doseFilteredBeneficiaryList = getDoseFilteredBeneficiaries(ageGroupFilteredBeneficiaryList);
        if (doseFilteredBeneficiaryList.size() > 0) {
            int padding_bottom_in_dp = 10;  // 10 dps
            final float scale = getResources().getDisplayMetrics().density;
            int padding_bottom_in_px = (int) (padding_bottom_in_dp * scale + 0.5f);
            preferencesBottomSheetView.findViewById(R.id.preferredBeneficiaryRecyclerView).setPadding(0, 0, 0, padding_bottom_in_px);
            preferencesBottomSheetView.findViewById(R.id.noBeneficiariesFoundCard).setVisibility(View.GONE);
        }
        else {
            preferencesBottomSheetView.findViewById(R.id.preferredBeneficiaryRecyclerView).setPadding(0, 0, 0, 0);
            GradientDrawable noBeneficiariesFoundShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.noBeneficiariesFoundCard).getBackground();
            GradientDrawable noBeneficiariesFoundIndicator = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.noBeneficiariesFoundCustomRadioButton).getBackground();
            noBeneficiariesFoundShape.setStroke(1, Color.argb(255, 219, 68, 55));
            noBeneficiariesFoundShape.setColor(Color.argb(50, 219, 68, 55));
            noBeneficiariesFoundIndicator.setColor(Color.argb(150, 219, 68, 55));
            preferencesBottomSheetView.findViewById(R.id.noBeneficiariesFoundCard).setVisibility(View.VISIBLE);
        }
        recyclerViewPreferredBeneficiaryAdapter = new RecyclerViewPreferredBeneficiaryAdapter(doseFilteredBeneficiaryList, this);
        recyclerPreferredBeneficiaryView.setAdapter(recyclerViewPreferredBeneficiaryAdapter);
    }

    public List<VaccinationCenterModel> getCenterList(JSONArray centers) {
        ArrayList mVaccinationCenterList = new ArrayList<>();
        for (int i = 0; i < centers.length(); i++) {
            mVaccinationCenterList.add(new VaccinationCenterModel(centers.optJSONObject(i)));
        }
        return mVaccinationCenterList;
    }

    public List<BeneficiaryModel> getBeneficiaryList(JSONArray beneficiaries) {
        ArrayList mBeneficiaryList = new ArrayList<>();
        for (int i = 0; i < beneficiaries.length(); i++) {
            mBeneficiaryList.add(new BeneficiaryModel(beneficiaries.optJSONObject(i)));
        }
        return mBeneficiaryList;
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
                preferenceBottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetTheme);
                preferencesBottomSheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.bot_preferences_bottom_sheet, (LinearLayout)findViewById(R.id.botPreferencesContainer));
                ImageView addDistrictImageView = (ImageView) preferencesBottomSheetView.findViewById(R.id.addDistrictImageView);
                ImageView addPincodeImageView = (ImageView) preferencesBottomSheetView.findViewById(R.id.addPincodeImageView);
                addDistrictImageView.setOnClickListener(v1 -> onAddDistrict(response.optJSONArray("districts")));
                addPincodeImageView.setOnClickListener(v1 -> onAddPincode());
                preferenceBottomSheetDialog.setOnDismissListener(dialog -> fabView.setVisibility(View.VISIBLE));
                preferenceBottomSheetDialog.setContentView(preferencesBottomSheetView);
                preferenceBottomSheetDialog.show();
                fabView.setVisibility(View.GONE);
                v.setVisibility(View.VISIBLE);
                botFabProgressBar.setVisibility(View.GONE);

                // Recycler View Initialization and Setup
                recyclerPreferredDistrictView = preferenceBottomSheetDialog.findViewById(R.id.preferredDistrictRecyclerView);
                setPreferredDistrictRecyclerView(_preferredDistrictList);
                recyclerPreferredPincodeView = preferenceBottomSheetDialog.findViewById(R.id.preferredPincodeRecyclerView);
                setPreferredPincodeRecyclerView(_preferredPincodeList);

                // Age Limit, Dose Number and Beneficiaries Recycler View Configuration
                recyclerPreferredBeneficiaryView = preferenceBottomSheetDialog.findViewById(R.id.preferredBeneficiaryRecyclerView);
                LinearLayout ageGroupMinorLinearLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.ageGroupMinorLinearLayout);
                LinearLayout ageGroupMajorLinearLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.ageGroupMajorLinearLayout);
                LinearLayout firstDoseLinearLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.firstDoseLinearLayout);
                LinearLayout secondDoseLinearLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.secondDoseLinearLayout);
                final int normalColor = Color.WHITE;
                final int normalIndicatorColor = Color.argb(255, 221, 221, 221);
                final int normalStrokeColor = Color.argb(255,204, 204, 204);
                GradientDrawable bgMinorIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.ageGroupMinorIndicator).getBackground();
                GradientDrawable bgMajorIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.ageGroupMajorIndicator).getBackground();
                GradientDrawable bgFirstDoseIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.firstDoseIndicator).getBackground();
                GradientDrawable bgSecondDoseIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.secondDoseIndicator).getBackground();
                ageGroupMinorLinearLayout.setOnClickListener(ageGroupMinorView -> {
                    GradientDrawable bgMinorShape = (GradientDrawable) ageGroupMinorView.getBackground();
                    final int selectedColor = Color.argb(50,61, 220, 132);
                    final int selectedIndicatorColor = Color.argb(150, 61, 220, 132);
                    final int selectedStrokeColor = Color.argb(255,61, 220, 132); // pink - rgb(255,192,203)
                    bgMinorShape.setColor(selectedColor);
                    bgMinorShape.setStroke(1, selectedStrokeColor);
                    bgMinorIndicatorShape.setColor(selectedIndicatorColor);
                    GradientDrawable bgMajorShape = (GradientDrawable) ageGroupMajorLinearLayout.getBackground();
                    bgMajorShape.setColor(normalColor);
                    bgMajorShape.setStroke(1, normalStrokeColor);
                    bgMajorIndicatorShape.setColor(normalIndicatorColor);
                    _isPreferredAge45Plus = false;
                    setPreferredBeneficiaryRecyclerView(beneficiaryList);
                });
                ageGroupMajorLinearLayout.setOnClickListener(ageGroupMajorView -> {
                    GradientDrawable bgMajorShape = (GradientDrawable) ageGroupMajorView.getBackground();
                    final int selectedColor = Color.argb(50,66, 133, 244);
                    final int selectedIndicatorColor = Color.argb(150, 66, 133, 244);
                    final int selectedStrokeColor = Color.argb(255,66, 133, 244);
                    bgMajorShape.setColor(selectedColor);
                    bgMajorShape.setStroke(1, selectedStrokeColor);
                    bgMajorIndicatorShape.setColor(selectedIndicatorColor);
                    GradientDrawable bgMinorShape = (GradientDrawable) ageGroupMinorLinearLayout.getBackground();
                    bgMinorShape.setColor(normalColor);
                    bgMinorShape.setStroke(1, normalStrokeColor);
                    bgMinorIndicatorShape.setColor(normalIndicatorColor);
                    _isPreferredAge45Plus = true;
                    setPreferredBeneficiaryRecyclerView(beneficiaryList);
                });
                firstDoseLinearLayout.setOnClickListener(firstDoseView -> {
                    GradientDrawable bgFirstDoseShape = (GradientDrawable) firstDoseView.getBackground();
                    final int selectedColor = Color.argb(50,61, 220, 132);
                    final int selectedIndicatorColor = Color.argb(150, 61, 220, 132);
                    final int selectedStrokeColor = Color.argb(255,61, 220, 132); // pink - rgb(255,192,203)
                    bgFirstDoseShape.setColor(selectedColor);
                    bgFirstDoseShape.setStroke(1, selectedStrokeColor);
                    bgFirstDoseIndicatorShape.setColor(selectedIndicatorColor);
                    GradientDrawable bgSecondDoseShape = (GradientDrawable) secondDoseLinearLayout.getBackground();
                    bgSecondDoseShape.setColor(normalColor);
                    bgSecondDoseShape.setStroke(1, normalStrokeColor);
                    bgSecondDoseIndicatorShape.setColor(normalIndicatorColor);
                    _preferredDoseNumber = 1;
                    setPreferredBeneficiaryRecyclerView(beneficiaryList);
                });
                secondDoseLinearLayout.setOnClickListener(secondDoseView -> {
                    GradientDrawable bgSecondDoseShape = (GradientDrawable) secondDoseView.getBackground();
                    final int selectedColor = Color.argb(50,66, 133, 244);
                    final int selectedIndicatorColor = Color.argb(150, 66, 133, 244);
                    final int selectedStrokeColor = Color.argb(255,66, 133, 244);
                    bgSecondDoseShape.setColor(selectedColor);
                    bgSecondDoseShape.setStroke(1, selectedStrokeColor);
                    bgSecondDoseIndicatorShape.setColor(selectedIndicatorColor);
                    GradientDrawable bgFirstDoseShape = (GradientDrawable) firstDoseLinearLayout.getBackground();
                    bgFirstDoseShape.setColor(normalColor);
                    bgFirstDoseShape.setStroke(1, normalStrokeColor);
                    bgFirstDoseIndicatorShape.setColor(normalIndicatorColor);
                    _preferredDoseNumber = 2;
                    setPreferredBeneficiaryRecyclerView(beneficiaryList);
                });
                ageGroupMinorLinearLayout.performClick();
                firstDoseLinearLayout.performClick();

                vaccineSelectionClickListeners();
            }
        }, error -> {
            if(error != null && error.networkResponse != null) Log.d("Districts Status Code", "" + error.networkResponse.statusCode);
            Log.d("Error Request", "That didn't work!!");
        });
        requestQueue.add(jsonObjectRequest);
    }

    public void selectVaccineCard(GradientDrawable bgLayoutShape, GradientDrawable bgIndicatorShape, boolean isSelected) {
        final int selectedColor = Color.argb(50,61, 220, 132);
        final int selectedIndicatorColor = Color.argb(150, 61, 220, 132);
        final int selectedStrokeColor = Color.argb(255,61, 220, 132);
        final int normalColor = Color.WHITE;
        final int normalIndicatorColor = Color.argb(255, 221, 221, 221);
        final int normalStrokeColor = Color.argb(255,204, 204, 204);
        bgLayoutShape.setColor(isSelected ? selectedColor : normalColor);
        bgLayoutShape.setStroke(1, isSelected ? selectedStrokeColor : normalIndicatorColor);
        bgIndicatorShape.setColor(isSelected ? selectedIndicatorColor : normalStrokeColor);
    }

    private void vaccineSelectionClickListeners() {
        LinearLayout covaxinLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.covaxinLinearLayout);
        LinearLayout covishieldLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.covishieldLinearLayout);
        LinearLayout sputnikLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.sputnikLinearLayout);
        LinearLayout allVaccinesLayout = (LinearLayout) preferencesBottomSheetView.findViewById(R.id.allVaccinesLinearLayout);
        GradientDrawable bgCovaxinLayoutShape = (GradientDrawable) covaxinLayout.getBackground();
        GradientDrawable bgCovishieldLayoutShape = (GradientDrawable) covishieldLayout.getBackground();
        GradientDrawable bgSputnikLayoutShape = (GradientDrawable) sputnikLayout.getBackground();
        GradientDrawable bgAllVaccinesLayoutShape = (GradientDrawable) allVaccinesLayout.getBackground();
        GradientDrawable bgCovaxinIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.covaxinIndicator).getBackground();
        GradientDrawable bgCovishieldIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.covishieldIndicator).getBackground();
        GradientDrawable bgSputnikIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.sputnikIndicator).getBackground();
        GradientDrawable bgAllVaccinesIndicatorShape = (GradientDrawable) preferencesBottomSheetView.findViewById(R.id.allVaccinesIndicator).getBackground();
        final int selectedColor = Color.argb(50,61, 220, 132);
        final int selectedIndicatorColor = Color.argb(150, 61, 220, 132);
        final int selectedStrokeColor = Color.argb(255,61, 220, 132);
        covaxinLayout.setOnClickListener(v -> {
            if(!_preferredVaccines.contains(1)) _preferredVaccines.add(1);
            else  _preferredVaccines.remove(Integer.valueOf(1));
            selectVaccineCard(bgCovaxinLayoutShape, bgCovaxinIndicatorShape, !_preferredVaccines.contains(1));
        });
        covishieldLayout.setOnClickListener(v -> {
            if(!_preferredVaccines.contains(2)) _preferredVaccines.add(2);
            else  _preferredVaccines.remove(Integer.valueOf(2));
            selectVaccineCard(bgCovishieldLayoutShape, bgCovishieldIndicatorShape, !_preferredVaccines.contains(2));
        });
        sputnikLayout.setOnClickListener(v -> {
            if(!_preferredVaccines.contains(3)) _preferredVaccines.add(3);
            else  _preferredVaccines.remove(Integer.valueOf(3));
            selectVaccineCard(bgSputnikLayoutShape, bgSputnikIndicatorShape, !_preferredVaccines.contains(3));
        });
        allVaccinesLayout.setOnClickListener(v -> {
            if(!_preferredVaccines.contains(1)) _preferredVaccines.add(1);
            if(!_preferredVaccines.contains(2)) _preferredVaccines.add(2);
            if(!_preferredVaccines.contains(3)) _preferredVaccines.add(3);
        });
    }

    public List<BeneficiaryModel> getAgeGroupFilteredBeneficiaries(List<BeneficiaryModel> beneficiaryList) {
        ArrayList filteredBeneficiaries = new ArrayList<>();
        for(int i = 0; i < beneficiaryList.size(); i++){
            if(!_isPreferredAge45Plus && !beneficiaryList.get(i).is45Plus()) filteredBeneficiaries.add(beneficiaryList.get(i));
            else if(_isPreferredAge45Plus && beneficiaryList.get(i).is45Plus()) filteredBeneficiaries.add(beneficiaryList.get(i));
        }
        return filteredBeneficiaries;
    }

    public List<BeneficiaryModel> getDoseFilteredBeneficiaries(List<BeneficiaryModel> beneficiaryList){
        ArrayList filteredBeneficiaries = new ArrayList<>();
        for(int i = 0; i < beneficiaryList.size(); i++){
            if((_preferredDoseNumber == 1) && !beneficiaryList.get(i).is1stDoseGiven()) filteredBeneficiaries.add(beneficiaryList.get(i));
            else if((_preferredDoseNumber == 2) && beneficiaryList.get(i).is1stDoseGiven() && !beneficiaryList.get(i).is2ndDoseGiven()) filteredBeneficiaries.add(beneficiaryList.get(i));
        }
        return filteredBeneficiaries;
    }

    public void onAddDistrict(JSONArray districts) {
        addDistrictDialog.startLoadingDialog();
        setDistrictDropdown(districts);
        final LinearLayout districtInfoMessageView = (LinearLayout) addDistrictDialog.customDialogView.findViewById(R.id.districtDialogInfoMessageTextView);
        final ProgressBar districtDialogResourceLoader = (ProgressBar) addDistrictDialog.customDialogView.findViewById(R.id.districtDialogResourceLoader);
        final LinearLayout districtConfigButtons = (LinearLayout) addDistrictDialog.customDialogView.findViewById(R.id.districtConfigButtons);
        final LinearLayout selectAllCentersButton = (LinearLayout) addDistrictDialog.customDialogView.findViewById(R.id.selectAllDistrictCentersButton);
        final LinearLayout addDistrictButton = (LinearLayout) addDistrictDialog.customDialogView.findViewById(R.id.addDistrictButton);
        final TextView selectAllCentersTextView = (TextView) addDistrictDialog.customDialogView.findViewById(R.id.selectAllDistrictCentersTextView);
        recyclerCenterView = addDistrictDialog.customDialogView.findViewById(R.id.centerRecyclerView);
        selectAllCentersButton.setOnClickListener(v -> {
            recyclerCenterViewAdapter.setIsSelectAll(!recyclerCenterViewAdapter.isSelectAll());
            selectAllCentersTextView.setText(!recyclerCenterViewAdapter.isSelectAll() ? "Select All" : "Unselect");
        });
        autoCompleteDistrictDropdown.setOnItemClickListener((adapterView, view, i, l) -> {
            districtConfigButtons.setVisibility(View.GONE);
            recyclerCenterView.setVisibility(View.GONE);
            districtInfoMessageView.setVisibility(View.GONE);
            districtDialogResourceLoader.setVisibility(View.VISIBLE);
            final JSONObject selectedDistrict = districts.optJSONObject(i);
            final String districtId = selectedDistrict.optString("district_id");
            // Log.d("Selected District", districts.getJSONObject(i).toString());
            getCentersByDistrict(districtId, getDate(1), response -> {
                Log.d("Center Results", response.toString());
                setVaccinationCenterRecyclerView(response.optJSONArray("centers"));
                districtDialogResourceLoader.setVisibility(View.GONE);
                recyclerCenterView.setVisibility(View.VISIBLE);
                districtConfigButtons.setVisibility(View.VISIBLE);
                selectAllCentersButton.performClick();
                addDistrictButton.setOnClickListener(v -> onAddDistrictClick(selectedDistrict));
                // recyclerCenterViewAdapter.setIsSelectAll(false);
                // selectAllCentersTextView.setText("Select All");
            });
        });
    }

    public void onAddPincode() {
        addPincodeDialog.startLoadingDialog();
        final LinearLayout pincodeInfoMessageView = addPincodeDialog.customDialogView.findViewById(R.id.pincodeDialogInfoMessageTextView);
        final ProgressBar pincodeDialogResourceLoader = addPincodeDialog.customDialogView.findViewById(R.id.pincodeDialogResourceLoader);
        final LinearLayout pincodeConfigButtons = addPincodeDialog.customDialogView.findViewById(R.id.pincodeConfigButtons);
        final TextInputEditText pincodeTextInputEditText = addPincodeDialog.customDialogView.findViewById(R.id.pincodeTextInput);
        final ImageView searchPincodeImageView = addPincodeDialog.customDialogView.findViewById(R.id.searchPincodeImageView);
        final LinearLayout selectAllCentersButton = addPincodeDialog.customDialogView.findViewById(R.id.selectAllPincodeCentersButton);
        final LinearLayout addPincodeButton = addPincodeDialog.customDialogView.findViewById(R.id.addPincodeButton);
        final TextView selectAllCentersTextView = addPincodeDialog.customDialogView.findViewById(R.id.selectAllPincodeCentersTextView);
        recyclerCenterView = addPincodeDialog.customDialogView.findViewById(R.id.centerRecyclerView);
        selectAllCentersButton.setOnClickListener(v -> {
            recyclerCenterViewAdapter.setIsSelectAll(!recyclerCenterViewAdapter.isSelectAll());
            selectAllCentersTextView.setText(!recyclerCenterViewAdapter.isSelectAll() ? "Select All" : "Unselect");
        });
//        pincodeTextInputEditText.setOnEditorActionListener((v, actionId, event) -> {
//            if(actionId == EditorInfo.IME_ACTION_SEND) return true;
//            return false;
//        });
        searchPincodeImageView.setOnClickListener(v -> {
            pincodeConfigButtons.setVisibility(View.GONE);
            recyclerCenterView.setVisibility(View.GONE);
            pincodeInfoMessageView.setVisibility(View.GONE);
            pincodeDialogResourceLoader.setVisibility(View.VISIBLE);
            final String pincode = pincodeTextInputEditText.getText().toString();
            getCentersByPincode(pincode, getDate(0), response -> {
                Log.d("Center Results", response.toString());
                setVaccinationCenterRecyclerView(response.optJSONArray("centers"));
                pincodeDialogResourceLoader.setVisibility(View.GONE);
                recyclerCenterView.setVisibility(View.VISIBLE);
                pincodeConfigButtons.setVisibility(View.VISIBLE);
                selectAllCentersButton.performClick();
                addPincodeButton.setOnClickListener(view -> onAddPincodeClick(pincode));
            });
        });
    }

    public void onAddDistrictClick(JSONObject selectedDistrict) {
        if(_preferredDistrictList.length() + _preferredPincodeList.length() >= 10) {
            Toast.makeText(this, "Can not add more than 10 preferences", Toast.LENGTH_SHORT);
            addDistrictDialog.dismissDialog();
            return;
        }
        JSONObject districtPreferences = new JSONObject();
        JSONArray prefCenters = new JSONArray();
        // Log.d("selectedDistrict", selectedDistrict.toString());

        final int selectedDistrictId = selectedDistrict.optInt("district_id");
        for(int i = 0; i < _preferredDistrictList.length(); i++){
            final int districtId = Objects.requireNonNull(_preferredDistrictList.optJSONObject(i).optJSONObject("district_info")).optInt("district_id");
            if(districtId == selectedDistrictId) _preferredDistrictList.remove(i); // Remove the same district info only if it exists
        }

        for(int i = 0; i < recyclerCenterViewAdapter.centerList.size(); i++){
            final VaccinationCenterModel center = recyclerCenterViewAdapter.centerList.get(i);
            if(center.isSelected()) prefCenters.put(center);
            // Log.d("center", "" + center.isSelected());
        }
        try {
            districtPreferences.put("district_info", selectedDistrict);
            districtPreferences.put("pref_centers", prefCenters);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Log.d("_preferredDistrictList", _preferredDistrictList.toString());
        _preferredDistrictList.put(districtPreferences);
        addDistrictDialog.dismissDialog();
        // setPreferredDistrictRecyclerView(_preferredDistrictList);
        recyclerViewPreferredDistrictAdapter.notifyDataSetChanged();
    }

    public void onAddPincodeClick(String selectedPincode) {
        if(_preferredDistrictList.length() + _preferredPincodeList.length() <= 10) {
            Toast.makeText(this, "Can not add more than 10 preferences", Toast.LENGTH_SHORT);
            addPincodeDialog.dismissDialog();
            return;
        }
        JSONObject pincodePreferences = new JSONObject();
        JSONArray prefCenters = new JSONArray();
        for(int i = 0; i < _preferredPincodeList.length(); i++){
            final String pincode = (_preferredPincodeList.optJSONObject(i).optString("pincode"));
            if(pincode.equals(selectedPincode)) _preferredPincodeList.remove(i); // Remove the same district info only if it exists
        }
        for(int i = 0; i < recyclerCenterViewAdapter.centerList.size(); i++){
            final VaccinationCenterModel center = recyclerCenterViewAdapter.centerList.get(i);
            if(center.isSelected()) prefCenters.put(center);
        }
        try {
            pincodePreferences.put("pincode", selectedPincode);
            pincodePreferences.put("pref_centers", prefCenters);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        _preferredPincodeList.put(pincodePreferences);
        addPincodeDialog.dismissDialog();
        recyclerViewPreferredPincodeAdapter.notifyDataSetChanged();
        Log.d("_preferredPincodeList", _preferredPincodeList.toString());
    }

    // @Override
    // public void onVaccinationCenterClick(int position, VaccinationCenterModel selectedCenter) {
    //     Log.d("Test", "This is just for testing purpose");
    // }

    @Override
    public void onRemovePreferredDistrictClick(int position) {
        // Log.d("Removed Position", "" + position);
        _preferredDistrictList.remove(position);
        recyclerViewPreferredDistrictAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRemovePreferredPincodeClick(int position) {
        _preferredPincodeList.remove(position);
        recyclerViewPreferredPincodeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSelectPreferredBeneficiaryClick(int position) {
        Log.d("Selection", "Beneficiary Selected" + position);
    }

    // @Override
    // public void onRemovePreferredBeneficiaryClick(int position) {
    //     Log.d("Position Check", "" + position);
    // }

    public void getCentersByDistrict(final String districtId, final String date, final VolleyCallback callback){
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

    public void getCentersByPincode(final String pincode, final String date, final VolleyCallback callback){
        String url = cowin_base_url + "/v2/appointment/sessions/public/calendarByPin";
        url += "?pincode=" + pincode + "&date=" + date;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Pincode Information", response.toString());
                callback.onSuccess(response);
            }
        }, error -> Log.d("Error Request", "That didn't work!!"));
        requestQueue.add(jsonObjectRequest);
    }

    private String getDate(int days_to_add){
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, days_to_add);
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