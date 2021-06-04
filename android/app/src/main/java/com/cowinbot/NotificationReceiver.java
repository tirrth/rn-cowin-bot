package com.cowinbot;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.WindowManager;
import android.widget.Toast;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // builder.setTitle("Stop the BOT")
        //     .setMessage("Are you sure you want to stop this service?")
        //     .setCancelable(false)
        //     .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        //         public void onClick(DialogInterface dialog, int which) {
        //             Intent serviceIntent = new Intent(context, TextMessageListenerService.class);
        //             context.stopService(serviceIntent);
        //             String message = intent.getStringExtra("toastMessage");
        //             Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        //         }
        //     })
        //     .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
        //         public void onClick(DialogInterface dialog, int id) {
        //             dialog.cancel();
        //         }
        //     })
        //     .setIcon(android.R.drawable.ic_dialog_alert);
        // AlertDialog alert = builder.create();
        // alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
        // alert.show();
        Intent serviceIntent = new Intent(context, TextMessageListenerService.class);
        context.stopService(serviceIntent);
    }
}