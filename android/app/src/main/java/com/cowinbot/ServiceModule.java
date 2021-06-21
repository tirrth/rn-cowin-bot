package com.cowinbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    public void startService(ReadableMap preferences) throws JSONException {
        Intent serviceIntent = new Intent(getCurrentActivity(), TextMessageListenerService.class);
        serviceIntent.putExtra("preferences", convertMapToJson(preferences).toString());
        ContextCompat.startForegroundService(getCurrentActivity(), serviceIntent);
    }

    private static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.put(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }

    private static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }

    @ReactMethod
    public void stopService() {
        Intent serviceIntent = new Intent(getCurrentActivity(), TextMessageListenerService.class);
        getCurrentActivity().stopService(serviceIntent);
    }

    @ReactMethod
    public String getCowinToken(){
        SharedPreferences preferences = getReactApplicationContext().getSharedPreferences("COWIN", Context.MODE_PRIVATE);
        return preferences.getString("token", "");
    }

    @ReactMethod
    public void setCowinToken(String token){
        SharedPreferences.Editor editor = getReactApplicationContext().getSharedPreferences("COWIN", Context.MODE_PRIVATE).edit();
        editor.putString("token", token);
        editor.apply();
    }
}