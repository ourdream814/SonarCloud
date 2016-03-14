package com.softrangers.sonarcloudmobile.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.softrangers.sonarcloudmobile.utils.api.SocketService;

/**
 * Created by Eduard Albu on 14 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class BaseActivity extends AppCompatActivity {

    protected static SocketService socketService;
    protected boolean isBound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            socketService = ((SocketService.LocalBinder) service).getService();
            onServiceBound(socketService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            socketService = null;
        }
    };

    protected void bindService(Context context) {
        bindService(new Intent(context, SocketService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    protected void unbindService() {
        if (isBound) {
            unbindService(mServiceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }

    public void alertUserAboutError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(BaseActivity.this)
                        .setTitle("Sorry")
                        .setMessage(message)
                        .setPositiveButton("ok", null)
                        .create();
                dialog.show();
            }
        });
    }

    public void onServiceBound(SocketService socketService){}
}
