package com.softrangers.sonarcloudmobile.utils.widgets;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by eduard on 4/15/16.
 */
public class InternetConnectionStatusbarListener {

    private final int NOTIFICATION_ID = 1257;
    public Context mContext;
    private StatusBarNotification sStatusBarView;
    private WindowManager mWindowManager;
    private NotificationManager mNotificationManager;
    private boolean mPreviousInternetStateWasOnline = true;
    private boolean mIsPaused = true;
    private Thread mBgThread;

    public InternetConnectionStatusbarListener(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mBgThread = new Thread() {
            public void run() {
                while (true) {

                    if (mIsPaused) continue;

                    final boolean currentInternetStateIsOnline = isOnline();
                    if (currentInternetStateIsOnline) {
                        if (!mPreviousInternetStateWasOnline) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        hideNotification();
                                        mPreviousInternetStateWasOnline = currentInternetStateIsOnline;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    } else {
                        if (mPreviousInternetStateWasOnline) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        showNotification();
                                        mPreviousInternetStateWasOnline = currentInternetStateIsOnline;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }

                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public void start() {
        mBgThread.start();
    }

    public void stop() {
        try {
            hideNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBgThread.interrupt();
    }

    public void pause() {
        mIsPaused = true;
        try {
            hideNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resume(Context context) {
        mContext = context;
        mIsPaused = false;
        if (!mPreviousInternetStateWasOnline) {
            try {
                showNotification();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean isOnline() {
        return pingAddress("google.com", 80);
    }

    public boolean pingAddress(String ip, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, port), 2000);
            socket.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private int getInternalDimensionSize(Resources res, String key) {
        int result = 0;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private WindowManager.LayoutParams getWindowManagerParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT); // must be translucent to support KitKat gradient
        params.gravity = Gravity.TOP;
        params.height = getInternalDimensionSize(mContext.getResources(), "status_bar_height");
        return params;
    }

    public void showNotification() throws Exception {

        if (sStatusBarView == null) {
            sStatusBarView = new StatusBarNotification(mContext);
            mWindowManager.addView(sStatusBarView, getWindowManagerParams());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setWhen(0);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void hideNotification() throws Exception {
        if (sStatusBarView != null) {
            mWindowManager.removeView(sStatusBarView);
            sStatusBarView = null;
        }
        mNotificationManager.cancel(NOTIFICATION_ID);
    }


    public class StatusBarNotification extends LinearLayout {
        public StatusBarNotification(Context context) {
            super(context, null, 0);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            View view = LayoutInflater.from(context).inflate(R.layout.notification, this);
            TextView tv = (TextView) view.findViewById(R.id.internetConnectionListener_textView);
        }
    }
}
