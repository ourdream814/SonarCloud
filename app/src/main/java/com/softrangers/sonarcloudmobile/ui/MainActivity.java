package com.softrangers.sonarcloudmobile.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Group;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.ui.fragments.ReceiversFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.RecordFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.ScheduleFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.SettingsFragment;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.observers.GroupObserver;
import com.softrangers.sonarcloudmobile.utils.observers.Observable;
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
    private static boolean isAlreadyStarted;

    private TextView mToolbarTitle;
    private LinearLayout mMainBottomBar;
    private ImageButton mReceiversSelector;
    private ImageButton mAnnouncementsSelector;
    private ImageButton mRecordingsSelector;
    private ImageButton mSettingsSelector;
    public SelectedFragment mSelectedFragment;

    private static ReceiversFragment receiversFragment;
    private static RecordFragment recordFragment;
    private static ScheduleFragment scheduleFragment;
    private static SettingsFragment settingsFragment;

    // set selected items to send the record to them
    public static ArrayList<Receiver> selectedReceivers = new ArrayList<>();
    public static Group selectedGroup;
    private IntentFilter intentFilter;

    static {
        observers = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter = new IntentFilter(Api.Command.AUTHENTICATE);
        intentFilter.addAction(Api.EXCEPTION);
        registerReceiver(mLoginReceiver, intentFilter);
        // initialize bottom buttons
        assert mToolbarTitle != null;
        mToolbarTitle = (TextView) findViewById(R.id.main_activity_toolbarTitle);
        mToolbarTitle.setTypeface(SonarCloudApp.avenirMedium);
        initializeBottomButtons();

        if (savedInstanceState != null) {
            receiversFragment = (ReceiversFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                    receiversFragment.getClass().getSimpleName());
            recordFragment = (RecordFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                    recordFragment.getClass().getSimpleName());
            scheduleFragment = (ScheduleFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                    scheduleFragment.getClass().getSimpleName());
            settingsFragment = (SettingsFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                    settingsFragment.getClass().getSimpleName());
        } else {
            receiversFragment = (ReceiversFragment) getSupportFragmentManager().findFragmentById(R.id.receivers_fragment);
            recordFragment = (RecordFragment) getSupportFragmentManager().findFragmentById(R.id.record_fragment);
            settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
            scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.schedules_fragment);
        }

        mSelectedFragment = SelectedFragment.RECEIVERS;
        changeFragment(receiversFragment);
        invalidateViews();
        if (!SonarCloudApp.getInstance().isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
            return;
        }

        if (SonarCloudApp.dataSocketService != null) {
            SonarCloudApp.dataSocketService.restartConnection();
        }

        showLoading();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        getSupportFragmentManager().putFragment(outState, receiversFragment.getClass().getSimpleName(), receiversFragment);
        getSupportFragmentManager().putFragment(outState, recordFragment.getClass().getSimpleName(), recordFragment);
        getSupportFragmentManager().putFragment(outState, scheduleFragment.getClass().getSimpleName(), scheduleFragment);
        getSupportFragmentManager().putFragment(outState, settingsFragment.getClass().getSimpleName(), settingsFragment);
    }

    //------------------- Fragment controls -------------------//

    /**
     * Initialize bottom buttons and set the {@link ReceiversFragment} as the first fragment in
     * the activity
     *
     * @see {@link MainActivity#changeFragment(Fragment)}
     */
    private void initializeBottomButtons() {
        //link bottom buttons
        mMainBottomBar = (LinearLayout) findViewById(R.id.main_bottomBar);
        mReceiversSelector = (ImageButton) findViewById(R.id.bottom_PASystems_selector);
        mAnnouncementsSelector = (ImageButton) findViewById(R.id.bottom_announcements_selector);
        mRecordingsSelector = (ImageButton) findViewById(R.id.bottom_recordings_selector);
        mSettingsSelector = (ImageButton) findViewById(R.id.bottom_settings_selector);
        if (!SonarCloudApp.getInstance().canUseMicrophone()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                new ExplainPermission().execute();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERM);
            }
        }
    }

    /**
     * Change the current fragment with the given fragment.
     * method first check if the fragment is not already added to the list then it hides all other
     * fragments and then either shows or add the given fragment into app back stack
     *
     * @param newFragment which you want to show on the screen
     */
    private void changeFragment(Fragment newFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            transaction.hide(fragment);
        }
        transaction.show(newFragment);
        transaction.commit();
    }

    /**
     * Called when on of the four bottom buttons is clicked
     *
     * @param view of the actual clicked button
     */
    public void onBottomButtonsClick(View view) {
        switch (view.getId()) {
            case R.id.bottom_PASystems_selector:
                changeFragment(receiversFragment);
                mSelectedFragment = SelectedFragment.RECEIVERS;
                invalidateViews();
                break;
            case R.id.bottom_announcements_selector:
                changeFragment(recordFragment);
                mSelectedFragment = SelectedFragment.ANNOUNCEMENTS;
                invalidateViews();
                break;
            case R.id.bottom_recordings_selector:
                if (statusChanged) {
                    if (selectedReceivers.size() > 0)
                        sendReceiversToScheduleFragment(selectedReceivers);
                    else if (selectedGroup != null)
                        sendReceiversToScheduleFragment(selectedGroup.getReceivers());
                    else
                        sendReceiversToScheduleFragment(new ArrayList<Receiver>());
                }
                mSelectedFragment = SelectedFragment.RECORDINGS;
                invalidateViews();
                changeFragment(scheduleFragment);
                break;
            case R.id.bottom_settings_selector:
                changeFragment(settingsFragment);
                mSelectedFragment = SelectedFragment.SETTINGS;
                invalidateViews();
                break;
        }
    }

    /**
     * Change buttons background and state, depends on {@link SelectedFragment}
     */
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

    /**
     * Called when a receiver from the {@link ReceiversFragment#mListView} is clicked
     *
     * @param receiver which was clicked
     * @see {@link ReceiversFragment#onChildClick(Receiver, int)}
     */
    @Override
    public void onReceiverClicked(Receiver receiver) {
        selectedGroup = null;
        if (receiver.isSelected()) {
            selectedReceivers.add(receiver);
        } else {
            selectedReceivers.remove(receiver);
        }
        statusChanged = true;
    }

    /**
     * Called when a group from the {@link ReceiversFragment#mGroupsRecyclerView} is clicked
     *
     * @param group which was clicked
     * @see {@link ReceiversFragment#onGroupClicked(Group, int)}
     */
    @Override
    public void onGroupClicked(Group group) {
        selectedReceivers.clear();
        selectedGroup = group;
        statusChanged = true;
    }

    private void sendReceiversToScheduleFragment(final ArrayList<Receiver> receivers) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (receivers.size() <= 0) {
                    scheduleFragment.clearLists();
                } else {
                    scheduleFragment.clearLists();
                    scheduleFragment.getAllRecordingsFromServer(receivers);
                }
            }
        }).start();
    }

    /**
     * Helps to keep track of current selected fragment
     */
    public enum SelectedFragment {
        RECEIVERS, ANNOUNCEMENTS, RECORDINGS, SETTINGS
    }


    //------------------- Authentication -------------------//
    /**
     * Receive the response when a login request is sent to server
     */
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

    /**
     * Called when server has a successful response for us
     *
     * @param response json which is received from server
     */
    public void onResponseSucceed(JSONObject response) {
        dismissLoading();
        SonarCloudApp.user = User.build(response);
    }

    /**
     * Called when server can't execute the command or it has sent an empty response
     *
     * @param message either {@link com.softrangers.sonarcloudmobile.R.string#unknown_error}
     *                or a message from server
     */
    public void onCommandFailure(String message) {
        if (message.toLowerCase().contains("identifier and secret combination.")) {
            logout(null);
        } else {
            dismissLoading();
            if (message != null) {
                Snackbar.make(mToolbarTitle, message, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Called when a response occured during parsing response or any other local error
     */
    public void onErrorOccurred() {
        dismissLoading();
//        Snackbar.make(mToolbarTitle, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Logout the current user and delete all personal data from the application
     */
    public void logout(View view) {
        SonarCloudApp.getInstance().clearUserSession(false);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST_CODE);
        SonarCloudApp.getInstance().stopKeepingConnection();
    }


    //------------------- Helpers -------------------//
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(USER_STATE, SonarCloudApp.user);
    }

    /**
     * Class used to show a dialog with details about requested permission
     */
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

    /**
     * Called when lock button from {@link SettingsFragment} is clicked
     */
    public void lockApplication(View view) {
        startActivityForResult(new Intent(this, LockAppActivity.class), 56);
        overridePendingTransition(R.anim.enter, R.anim.exit);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            SonarCloudApp.getInstance().startKeepingConnection();
            stopService(SonarCloudApp.dataSocketIntent);
            unregisterReceiver(mLoginReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //------------------- Notifiers -------------------//

    /**
     * Called when socket connection is established to notify that we can start the login process
     */
    @Override
    public void onSocketConnected() {

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
                        registerReceiver(mLoginReceiver, intentFilter);
                        SonarCloudApp.dataSocketService.restartConnection();
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        showLoading();
                        receiversFragment.onSocketConnected();
                        break;
                    case ScheduleActivity.ACTION_ADD_SCHEDULE:
                        if (recordFragment != null && recordFragment.mRecAdapter != null) {
                            recordFragment.mRecAdapter.refreshList(recordFragment.getRecordedFileList());
                        }
                        break;
                    case ScheduleActivity.ACTION_EDIT_SCHEDULE:
                        if (selectedGroup != null)
                            sendReceiversToScheduleFragment(selectedGroup.getReceivers());
                        else sendReceiversToScheduleFragment(selectedReceivers);
                        break;
                }
                break;
            case RESULT_CANCELED:
                if (action.equals(ACTION_LOGIN)) {
                    SonarCloudApp.getInstance().clearUserSession(true);
                    finish();
                }
                break;
        }
    }

    /**
     * Add observers to the list
     *
     * @param observer which will be notified when add group activity sent the newly created group
     */
    @Override
    public void addObserver(GroupObserver observer) {
        observers.add(observer);
    }

    /**
     * Remove observers from the list
     *
     * @param observer which you want to remove from the list
     */
    @Override
    public void removeObserver(GroupObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all registered observers
     */
    @Override
    public void notifyObservers() {
        for (GroupObserver observer : observers) {
            if (observer != null) {
                observer.update(mGroup);
            }
        }
    }
}
