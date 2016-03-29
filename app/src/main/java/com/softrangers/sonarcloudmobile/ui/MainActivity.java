package com.softrangers.sonarcloudmobile.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.util.List;

public class MainActivity extends BaseActivity implements
        ConnectionReceiver.OnConnected, Observable<GroupObserver>,
        ReceiversFragment.OnRecordFragmentListener {

    private static final String USER_STATE = "user_state";
    public static final String ACTION_LOGIN = "ACTION_LOGIN";
    public static final int LOGIN_REQUEST_CODE = 2229;
    public static final int RECORD_AUDIO_PERM = 29;
    private static ArrayList<GroupObserver> observers;
    public static boolean statusChanged;
    private Group mGroup;

    private TextView mToolbarTitle;
    private LinearLayout mMainBottomBar;
    private ImageButton mReceiversSelector;
    private ImageButton mAnnouncementsSelector;
    private ImageButton mRecordingsSelector;
    private ImageButton mSettingsSelector;
    private SelectedFragment mSelectedFragment;

    private ReceiversFragment mReceiversFragment;
    private RecordFragment mRecordFragment;
    private ScheduleFragment mScheduleFragment;
    private SettingsFragment mSettingsFragment;

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
        // initialize bottom buttons
        assert mToolbarTitle != null;
        mToolbarTitle = (TextView) findViewById(R.id.main_activity_toolbarTitle);
        mToolbarTitle.setTypeface(SonarCloudApp.avenirMedium);
        ConnectionReceiver.getInstance().addOnConnectedListener(this);
        if (SonarCloudApp.socketService != null) {
            SonarCloudApp.socketService.restartConnection();
        }
    }

    private void initializeBottomButtons() {
        // instantiate fragments
        mReceiversFragment = new ReceiversFragment();
        mRecordFragment = new RecordFragment();
        mSettingsFragment = new SettingsFragment();
        //link bottom buttons
        mMainBottomBar = (LinearLayout) findViewById(R.id.main_bottomBar);
        mReceiversSelector = (ImageButton) findViewById(R.id.bottom_PASystems_selector);
        mAnnouncementsSelector = (ImageButton) findViewById(R.id.bottom_announcements_selector);
        mRecordingsSelector = (ImageButton) findViewById(R.id.bottom_recordings_selector);
        mSettingsSelector = (ImageButton) findViewById(R.id.bottom_settings_selector);
        // set first button selected and add fragment to container
        mSelectedFragment = SelectedFragment.RECEIVERS;
        changeFragment(mReceiversFragment);
        invalidateViews();
        if (!SonarCloudApp.getInstance().areRecordingPermissed()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                new ExplainPermission().execute();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERM);
            }
        }
    }

    private void invalidateViews() {
        switch (mSelectedFragment) {
            case RECEIVERS:
                mToolbarTitle.setText(getString(R.string.select_pa_system));
                mReceiversSelector.setBackgroundResource(R.drawable.bottom_bright_gradient);
                mAnnouncementsSelector.setBackgroundResource(android.R.color.transparent);
                mRecordingsSelector.setBackgroundResource(android.R.color.transparent);
                mSettingsSelector.setBackgroundResource(android.R.color.transparent);
                break;
            case ANNOUNCEMENTS:
                mToolbarTitle.setText(getString(R.string.make_announcement));
                mReceiversSelector.setBackgroundResource(android.R.color.transparent);
                mAnnouncementsSelector.setBackgroundResource(R.drawable.bottom_bright_gradient);
                mRecordingsSelector.setBackgroundResource(android.R.color.transparent);
                mSettingsSelector.setBackgroundResource(android.R.color.transparent);
                break;
            case RECORDINGS:
                mToolbarTitle.setText(getString(R.string.schedules));
                mReceiversSelector.setBackgroundResource(android.R.color.transparent);
                mAnnouncementsSelector.setBackgroundResource(android.R.color.transparent);
                mRecordingsSelector.setBackgroundResource(R.drawable.bottom_bright_gradient);
                mSettingsSelector.setBackgroundResource(android.R.color.transparent);
                break;
            case SETTINGS:
                mToolbarTitle.setText(getString(R.string.settings));
                mReceiversSelector.setBackgroundResource(android.R.color.transparent);
                mAnnouncementsSelector.setBackgroundResource(android.R.color.transparent);
                mRecordingsSelector.setBackgroundResource(android.R.color.transparent);
                mSettingsSelector.setBackgroundResource(R.drawable.bottom_bright_gradient);
                break;
        }
    }

    public void onBottomButtonsClick(View view) {
        switch (view.getId()) {
            case R.id.bottom_PASystems_selector:
                changeFragment(mReceiversFragment);
                mSelectedFragment = SelectedFragment.RECEIVERS;
                invalidateViews();
                break;
            case R.id.bottom_announcements_selector:
                changeFragment(mRecordFragment);
                mSelectedFragment = SelectedFragment.ANNOUNCEMENTS;
                invalidateViews();
                break;
            case R.id.bottom_recordings_selector:
                sendReceiversToScheduleFragment(selectedReceivers);
                changeFragment(mScheduleFragment);
                mSelectedFragment = SelectedFragment.RECORDINGS;
                invalidateViews();
                break;
            case R.id.bottom_settings_selector:
                changeFragment(mSettingsFragment);
                mSelectedFragment = SelectedFragment.SETTINGS;
                invalidateViews();
                break;
        }
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
        initializeBottomButtons();
    }

    public void onCommandFailure(String message) {
        dismissLoading();
        Snackbar.make(mMainBottomBar, message, Snackbar.LENGTH_SHORT).show();
    }

    public void onErrorOccurred() {
        dismissLoading();
        Snackbar.make(mMainBottomBar, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(USER_STATE, SonarCloudApp.user);
    }

    private void changeFragment(Fragment newFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment old = fragmentManager.findFragmentByTag(newFragment.getClass().getSimpleName());
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                transaction.hide(fragment);
            }
        }
        if (old != null && old.equals(newFragment)) {
            transaction.show(old);
        } else {
            transaction.add(R.id.main_fragmentContainer, newFragment,
                    newFragment.getClass().getSimpleName());
        }
        transaction.commit();
    }

    @Override
    public void onReceiverClicked(Receiver receiver) {
        selectedGroup = null;
        if (receiver.isSelected()) {
            selectedReceivers.add(receiver);
        } else {
            selectedReceivers.remove(receiver);
        }
    }

    @Override
    public void onGroupClicked(Group group) {
        selectedReceivers.clear();
        selectedGroup = group;
        sendReceiversToScheduleFragment(group.getReceivers());
    }

    private void sendReceiversToScheduleFragment(ArrayList<Receiver> receivers) {
        mScheduleFragment = (ScheduleFragment) getSupportFragmentManager()
                .findFragmentByTag(ScheduleFragment.class.getSimpleName());
        if (mScheduleFragment != null) {
            if (receivers.size() <= 0) {
                mScheduleFragment.clearLists();
            } else {
                mScheduleFragment.getAllRecordingsFromServer(receivers);
                mScheduleFragment.getAllScheduledRecords(receivers);
            }
        } else {
            mScheduleFragment = new ScheduleFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(ScheduleFragment.RECEIVERS_ARGS, receivers);
            mScheduleFragment.setArguments(bundle);
        }
    }

    enum SelectedFragment {
        RECEIVERS, ANNOUNCEMENTS, RECORDINGS, SETTINGS
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

    public void logout(View view) {
        SonarCloudApp.getInstance().clearUserSession();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST_CODE);
    }

    public void lockApplication(View view) {
        startActivity(new Intent(this, LockAppActivity.class));
        overridePendingTransition(R.anim.enter, R.anim.exit);
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
