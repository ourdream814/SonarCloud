package com.softrangers.sonarcloudmobile.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;

/**
 * Created by Eduard Albu on 14 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class BaseActivity extends AppCompatActivity {

    private AlertDialog mLoadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void alertUserAboutError(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(BaseActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("ok", null)
                        .create();
                dialog.show();
            }
        });
    }

    public boolean isLoading() {
        return mLoadingDialog != null && mLoadingDialog.isShowing();
    }

    public void showLoading() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null);
        TextView loadingText = (TextView) view.findViewById(R.id.loading_dialog_textView);
        loadingText.setTypeface(SonarCloudApp.avenirMedium);
        mLoadingDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();
        mLoadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mLoadingDialog.show();
    }

    public void dismissLoading() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
    }
}
