package com.example.cowintest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View v) throws JSONException {
        SharedPreferences.Editor editor = getSharedPreferences("COWIN", MODE_PRIVATE).edit();
        editor.putString("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiI1ZGY3YjE4MC02YTcyLTRiOTYtYWQxNy01ZmE0MjcwMTVmY2EiLCJ1c2VyX2lkIjoiNWRmN2IxODAtNmE3Mi00Yjk2LWFkMTctNWZhNDI3MDE1ZmNhIiwidXNlcl90eXBlIjoiQkVORUZJQ0lBUlkiLCJtb2JpbGVfbnVtYmVyIjo5MTA2MTMyODcwLCJiZW5lZmljaWFyeV9yZWZlcmVuY2VfaWQiOjg0NDgxNjAyOTMzNDEwLCJzZWNyZXRfa2V5IjoiYjVjYWIxNjctNzk3Ny00ZGYxLTgwMjctYTYzYWExNDRmMDRlIiwic291cmNlIjoiY293aW4iLCJ1YSI6Ik1vemlsbGEvNS4wIChNYWNpbnRvc2g7IEludGVsIE1hYyBPUyBYIDEwXzE1XzcpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS85MS4wLjQ0NzIuNzcgU2FmYXJpLzUzNy4zNiIsImRhdGVfbW9kaWZpZWQiOiIyMDIxLTA2LTA1VDA2OjQzOjM2LjcwOFoiLCJpYXQiOjE2MjI4NzU0MTYsImV4cCI6MTYyMjg3NjMxNn0.c7JZJXVp0R80tQHti80FJSAwZP9Taf22uN2Uw8L4G9o");
        editor.apply();
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
}