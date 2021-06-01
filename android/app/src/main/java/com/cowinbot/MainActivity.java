package com.cowinbot;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import com.facebook.react.ReactActivity;

public class MainActivity extends ReactActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//    startService();
  }

//  public void startService() {
//    Intent serviceIntent = new Intent(this, TextMessageListenerService.class);
//    serviceIntent.putExtra("inputExtra", "It is working");
//    ContextCompat.startForegroundService(this, serviceIntent);
//  }
//
//  public void stopService() {
//    Intent serviceIntent = new Intent(this, TextMessageListenerService.class);
//    stopService(serviceIntent);
//  }

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "CoWinBot";
  }
}
