package com.cowinbot;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class ServiceModule extends ReactContextBaseJavaModule {
    ServiceModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    @NonNull
    public String getName() {
        return "ServiceModule";
    }

    @ReactMethod
    public void startService(){
        Intent serviceIntent = new Intent(getCurrentActivity(), TextMessageListenerService.class);
        serviceIntent.putExtra("inputExtra", "It is working");
        ContextCompat.startForegroundService(getCurrentActivity(), serviceIntent);
    }

    @ReactMethod
    public void stopService() {
        Intent serviceIntent = new Intent(getCurrentActivity(), TextMessageListenerService.class);
        getCurrentActivity().stopService(serviceIntent);
    }
}