package com.example.cowintest;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RefreshTokenWorkManager extends Worker {
    private final String cowin_base_url = "https://cdn-api.co-vin.in/api";
    final Context context;
    final WorkerParameters workerParams;
    private static final String TAG = "RefreshTokenJobService";
    private final RequestQueue requestQueue;

    public RefreshTokenWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.workerParams = workerParams;
        requestQueue = Volley.newRequestQueue(context);
        Log.d(TAG, "Work manager started");
    }

    private void doBackgroundWork() {
        final String mobile = workerParams.getInputData().getString("mobile");
        generateOTP(mobile);
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
                    SharedPreferences.Editor editor = context.getSharedPreferences("COWIN", Context.MODE_PRIVATE).edit();
                    editor.putString("txnId", response.getString("txnId"));
                    editor.apply();
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
        requestQueue.add(jsonObjectRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        doBackgroundWork();
        Log.d(TAG, "Work manager stopped");
        return Result.success();
    }
}
