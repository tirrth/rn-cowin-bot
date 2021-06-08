package com.example.cowintest;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class CustomDialog {
    private Activity activity;
    private AlertDialog dialog;
    private int layout;
    AlertDialog.Builder builder;
    private boolean isCancelable;
    public View customDialogView;

    CustomDialog(Activity mActivity, int layout, boolean isCancelable){
        activity = mActivity;
        this.layout = layout;
        this.isCancelable = isCancelable;
    }

    void startLoadingDialog(){
        builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        customDialogView = inflater.inflate(layout, null);
        builder.setView(customDialogView);
        builder.setCancelable(isCancelable);
        dialog = builder.create();
        dialog.show();
    }

    void dismissDialog(){
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
    }
}
