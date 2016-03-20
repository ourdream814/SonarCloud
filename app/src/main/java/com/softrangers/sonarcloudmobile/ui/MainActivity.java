package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
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
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ConnectionReceiver;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends BaseActivity implements OnResponseListener,
        ConnectionReceiver.OnConnected, Observable<GroupObserver> {

    private static final String USER_STATE = "user_state";
    private static ArrayList<GroupObserver> observers;
    private static boolean isConnected;
    public static boolean statusChanged;
    private Group mGroup;

    private TextView mToolbarTitle;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private MainViewPagerAdapter mPagerAdapter;
    private Bundle mBundle;

    // set selected items to send the record to them
    public static ArrayList<Receiver> selectedReceivers = new ArrayList<>();
    public static Group selectedGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        observers = new ArrayList<>();
        mPagerAdapter = new MainViewPagerAdapter(this, getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.main_activity_viewPager);
        mTabLayout = (TabLayout) findViewById(R.id.main_activity_tabLayout);
        mToolbarTitle = (TextView) findViewById(R.id.main_activity_toolbarTitle);
        assert mToolbarTitle != null;
        mToolbarTitle.setTypeface(SonarCloudApp.avenirMedium);

        ConnectionReceiver.getInstance().addOnConnectedListener(this);
        ResponseReceiver.getInstance().addOnResponseListener(this);
        if (!SonarCloudApp.getInstance().isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            if (savedInstanceState != null) {
                mBundle = savedInstanceState;
                SonarCloudApp.user = savedInstanceState.getParcelable(USER_STATE);
            } else {
                if (SonarCloudApp.socketService != null && !isConnected) {
                    SonarCloudApp.socketService.restartConnection();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(USER_STATE, SonarCloudApp.user);
    }

    private void setUpTabs() {
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
                        scheduleFragment.getAllRecordingsFromServer(selectedGroup);
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
    }

    public void setToolbarTitle(@NonNull String title) {
        mToolbarTitle.setText(title);
    }

    @Override
    public void onResponse(JSONObject response) {
        dismissLoading();
        SonarCloudApp.user = User.build(response);
        ResponseReceiver.getInstance().clearResponseListenersList();
        setUpTabs();

    }

    @Override
    public void onCommandFailure(String message) {
        dismissLoading();
        ResponseReceiver.getInstance().clearResponseListenersList();
        alertUserAboutError(getString(R.string.error), message);
    }

    @Override
    public void onError() {
        dismissLoading();
        ResponseReceiver.getInstance().clearResponseListenersList();
        Snackbar.make(mViewPager, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }

    public void logout(View view) {
        SonarCloudApp.getInstance().clearUserSession();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isLoading()) dismissLoading();
        ResponseReceiver.getInstance().clearResponseListenersList();
        ConnectionReceiver.getInstance().removeOnResponseListener(this);
    }

    @Override
    public void onSocketConnected() {
        if (SonarCloudApp.socketService != null && SonarCloudApp.getInstance().isLoggedIn()) {
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.AUTHENTICATE);
            builder.device(Api.Device.CLIENT).method(Api.Method.IDENTIFIER).identifier(SonarCloudApp.getInstance().getIdentifier())
                    .secret(SonarCloudApp.getInstance().getSavedData()).seq(SonarCloudApp.SEQ_VALUE);
            ResponseReceiver.getInstance().addOnResponseListener(this);
            SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            isConnected = true;
            showLoading();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                if (data.getAction().equals(Api.ACTION_ADD_GROUP)) {
                    mGroup = data.getExtras().getParcelable(AddGroupActivity.GROUP_RESULT_BUNDLE);
                    statusChanged = true;
                    Toast.makeText(this, getString(R.string.group_saved), Toast.LENGTH_SHORT).show();
                    notifyObservers();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
        isConnected = false;
        selectedGroup = null;
        selectedReceivers.clear();
    }
}
