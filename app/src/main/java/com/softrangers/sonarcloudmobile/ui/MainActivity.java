package com.softrangers.sonarcloudmobile.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.MainViewPagerAdapter;
import com.softrangers.sonarcloudmobile.models.Group;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.ui.fragments.ReceiversFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.RecordFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.ScheduleFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.SettingsFragment;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.GroupObserver;
import com.softrangers.sonarcloudmobile.utils.Observable;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ConnectionReceiver;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends BaseActivity implements
        ConnectionReceiver.OnConnected, Observable<GroupObserver> {

    private static final String USER_STATE = "user_state";
    public static final String ACTION_LOGIN = "ACTION_LOGIN";
    public static final int LOGIN_REQUEST_CODE = 2229;
    public static final int RECORD_AUDIO_PERM = 29;
    private static ArrayList<GroupObserver> observers;
    public static boolean statusChanged;
    private Group mGroup;

    private TextView mToolbarTitle;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private MainViewPagerAdapter mPagerAdapter;

    // set selected items to send the record to them
    public static ArrayList<Receiver> selectedReceivers = new ArrayList<>();
    public static Group selectedGroup;
    private IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter = new IntentFilter(Api.Command.AUTHENTICATE);
        intentFilter.addAction(Api.EXCEPTION);
        registerReceiver(mLoginReceiver, intentFilter);
        observers = new ArrayList<>();
        mPagerAdapter = new MainViewPagerAdapter(this, getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.main_activity_viewPager);
        mTabLayout = (TabLayout) findViewById(R.id.main_activity_tabLayout);
        mToolbarTitle = (TextView) findViewById(R.id.main_activity_toolbarTitle);
        assert mToolbarTitle != null;
        mToolbarTitle.setTypeface(SonarCloudApp.avenirMedium);

        ConnectionReceiver.getInstance().addOnConnectedListener(this);
        if (SonarCloudApp.socketService != null) {
            SonarCloudApp.socketService.restartConnection();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                boolean success = jsonResponse.optBoolean("success", false);
                if (!success) {
                    String message = jsonResponse.optString("message", getString(R.string.unknown_error));
                    onCommandFailure(message);
                    return;
                }
                switch (action) {
                    case Api.Command.AUTHENTICATE:
                        onResponseSucceed(jsonResponse);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                onErrorOccurred();
            }
        }
    };

    public void onResponseSucceed(JSONObject response) {
        dismissLoading();
        SonarCloudApp.user = User.build(response);
        setUpTabs();
    }

    public void onCommandFailure(String message) {
        dismissLoading();
        Snackbar.make(mViewPager, message, Snackbar.LENGTH_SHORT).show();
    }

    public void onErrorOccurred() {
        dismissLoading();
        Snackbar.make(mViewPager, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(USER_STATE, SonarCloudApp.user);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setUpTabs() {
        if (mPagerAdapter.getCount() > 0) return;
        mPagerAdapter.addFragment(new ReceiversFragment(), getString(R.string.select_pa_system));
        mPagerAdapter.addFragment(new RecordFragment(), getString(R.string.make_announcement));

        final ScheduleFragment scheduleFragment = new ScheduleFragment();
        mPagerAdapter.addFragment(scheduleFragment, getString(R.string.schedule_announcement));
        mPagerAdapter.addFragment(new SettingsFragment(), getString(R.string.settings));
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);
        setToolbarTitle(mPagerAdapter.getTitle(0));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mPagerAdapter.getItem(position) instanceof ScheduleFragment) {
                    if (!statusChanged) return;
                    if (selectedReceivers.size() > 0) {
                        scheduleFragment.getAllRecordingsFromServer(selectedReceivers);
                    } else if (selectedGroup != null) {
                        scheduleFragment.getAllRecordingsFromServer(selectedGroup.getReceivers());
                    } else {
                        scheduleFragment.clearList();
                    }
                    statusChanged = false;
                }
                setToolbarTitle(mPagerAdapter.getTitle(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mTabLayout.setupWithViewPager(mViewPager);
        TabLayout.Tab receivers = mTabLayout.getTabAt(0);
        TabLayout.Tab record = mTabLayout.getTabAt(1);
        TabLayout.Tab schedule = mTabLayout.getTabAt(2);
        TabLayout.Tab settings = mTabLayout.getTabAt(3);

        assert receivers != null;
        receivers.setIcon(R.mipmap.ic_receivers);

        assert record != null;
        record.setIcon(R.mipmap.ic_microphone);

        assert schedule != null;
        schedule.setIcon(R.mipmap.ic_record_play_schedule);

        assert settings != null;
        settings.setIcon(R.mipmap.ic_settings);

        if (!SonarCloudApp.getInstance().areRecordingPermissed()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                new ExplainPermission().execute();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERM);
            }
        }
    }

    class ExplainPermission extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            AlertDialog permissionDialog = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("We need the permission to record audio in order to be able to make announcements")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERM);
                        }
                    })
                    .create();
            permissionDialog.show();
            return null;
        }
    }

    public void setToolbarTitle(@NonNull String title) {
        mToolbarTitle.setText(title);
    }

    public void logout(View view) {
        SonarCloudApp.getInstance().clearUserSession();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST_CODE);
    }

    @Override
    public void onSocketConnected() {
        if (SonarCloudApp.getInstance().isLoggedIn()) {
            if (SonarCloudApp.socketService != null) {
                registerReceiver(mLoginReceiver, intentFilter);
                Request.Builder builder = new Request.Builder();
                builder.command(Api.Command.AUTHENTICATE);
                builder.device(Api.Device.CLIENT).method(Api.Method.IDENTIFIER).identifier(SonarCloudApp.getInstance().getIdentifier())
                        .secret(SonarCloudApp.getInstance().getSavedData()).seq(SonarCloudApp.SEQ_VALUE);
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
                showLoading();
            }
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String action = data.getAction();
        switch (resultCode) {
            case RESULT_OK:
                switch (action) {
                    case Api.ACTION_ADD_GROUP:
                        mGroup = data.getExtras().getParcelable(AddGroupActivity.GROUP_RESULT_BUNDLE);
                        statusChanged = true;
                        Toast.makeText(this, getString(R.string.group_saved), Toast.LENGTH_SHORT).show();
                        notifyObservers();
                        break;
                    case ACTION_LOGIN:
                        ConnectionReceiver.getInstance().addOnConnectedListener(this);
                        onSocketConnected();
                        break;
                    case ScheduleActivity.ACTION_ADD_SCHEDULE:
                    case ScheduleActivity.ACTION_EDIT_SCHEDULE:
                        sendBroadcast(data);
                        break;
                }
                break;
            case RESULT_CANCELED:
                if (action.equals(ACTION_LOGIN)) {
                    onSocketConnected();
                }
                break;
        }
    }

    @Override
    public void addObserver(GroupObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(GroupObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (GroupObserver observer : observers) {
            if (observer != null) {
                observer.update(mGroup);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(mLoginReceiver);
        } catch (Exception e) {
        }
    }
}
