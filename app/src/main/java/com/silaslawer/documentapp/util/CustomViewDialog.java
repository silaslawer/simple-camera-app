package com.silaslawer.documentapp.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.silaslawer.documentapp.R;


public class CustomViewDialog {

    public void showDialog(Activity activity, String msg) {
        Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "Lato-Regular.ttf");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.custom_dialog);
        TextView text = dialog.findViewById(R.id.text_dialog);
        text.setTypeface(typeface);
        text.setText(msg);
        Button dialogButton = dialog.findViewById(R.id.btn_dialog);
        dialogButton.setTypeface(typeface);
        dialogButton.setOnClickListener(v -> activity.finish());
        dialog.show();
    }


    public void showDialogTwo(Activity activity, String msg) {
        Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "Lato-Regular.ttf");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.custom_dialog);
        TextView text = dialog.findViewById(R.id.text_dialog);
        text.setTypeface(typeface);
        text.setText(msg);
        Button dialogButton = dialog.findViewById(R.id.btn_dialog);
        dialogButton.setTypeface(typeface);
        dialogButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
