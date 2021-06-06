package com.example.cowintest;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    private final String cowin_base_url = "https://cdn-api.co-vin.in/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // getSupportActionBar().hide();
        LottieAnimationView lottieAnimationView = findViewById(R.id.splash_animation);
        Integer[] animations = {R.raw.covid_anim_1, R.raw.covid_anim_2, R.raw.covid_anim_3, R.raw.covid_anim_4, R.raw.covid_anim_5};
        final int random_anim_id =  animations[generateRandomInteger(0, animations.length - 1)];
        Log.d("Random Anim Id", "" + random_anim_id);
        lottieAnimationView.enableMergePathsForKitKatAndAbove(true);
        lottieAnimationView.setAnimation(random_anim_id);
        if(getCowinToken() == ""){
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // startActivity(new Intent(getBaseContext(), VerificationActivity.class));
                    // finish();
                    setContentView(R.layout.activity_verification);
                }
            }, 3000);
        } else{
            getBeneficiaries();
        }
    }

    private String getCowinToken(){
        SharedPreferences preferences = getSharedPreferences("COWIN", MODE_PRIVATE);
        return preferences.getString("token", "");
    }

    private void getBeneficiaries(){
        String url = cowin_base_url + "/v2/appointment/sessions/public/";
        final String token = getCowinToken();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Response", response.toString());
                // startActivity(new Intent(getBaseContext(), MainActivity.class));
                // finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error Request", "That didn't work!!");
                // startActivity(new Intent(getBaseContext(), MainActivity.class));
                // finish();
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
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private int generateRandomInteger(int min, int max){
        return (int)Math.floor(Math.random()*(max - min + 1) + min);
    }
}