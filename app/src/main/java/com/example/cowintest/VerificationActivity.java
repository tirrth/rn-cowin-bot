package com.example.cowintest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class VerificationActivity extends AppCompatActivity {
    private static final int PERMISSION_ALL_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        requestPermissions();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.loading_view);
            }
        }, 3000);
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
        // Toast.makeText(VerificationActivity.this, "All Permissions Denied", Toast.LENGTH_SHORT).show();
    }
}