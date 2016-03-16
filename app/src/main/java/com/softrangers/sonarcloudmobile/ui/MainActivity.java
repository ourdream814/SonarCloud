package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.MainViewPagerAdapter;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.ui.fragments.ReceiversFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.RecordFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.ScheduleFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.SettingsFragment;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ConnectionReceiver;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;

import org.json.JSONObject;

public class MainActivity extends BaseActivity implements OnResponseListener,
        ConnectionReceiver.OnConnected {

    private TextView mToolbarTitle;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private MainViewPagerAdapter mPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
            if (SonarCloudApp.socketService != null && SonarCloudApp.getInstance().isLoggedIn()) {
                showLoading();
                Request.Builder builder = new Request.Builder();
                builder.command(Api.Command.AUTHENTICATE);
                builder.device(Api.Device.CLIENT).method(Api.Method.IDENTIFIER).identifier(SonarCloudApp.getInstance().getIdentifier())
                        .secret(SonarCloudApp.getInstance().getSavedData()).seq(SonarCloudApp.SEQ_VALUE);
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
        }
    }

    private void setUpTabs() {
        mPagerAdapter.addFragment(new ReceiversFragment(), getString(R.string.select_pa_system));
        mPagerAdapter.addFragment(new RecordFragment(), getString(R.string.make_announcement));
        mPagerAdapter.addFragment(new ScheduleFragment(), getString(R.string.schedule_announcement));
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
        SonarCloudApp.user = User.build(response);
        ResponseReceiver.getInstance().removeOnResponseListener(this);
        setUpTabs();
    }

    @Override
    public void onCommandFailure(String message) {
        dismissLoading();
        alertUserAboutError(getString(R.string.error), message);
    }

    @Override
    public void onError() {
        dismissLoading();
        alertUserAboutError(getString(R.string.error), getString(R.string.unknown_error));
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
        ResponseReceiver.getInstance().removeOnResponseListener(this);
        ConnectionReceiver.getInstance().removeOnResponseListener(this);
    }

    @Override
    public void onSocketConnected() {
        if (SonarCloudApp.socketService != null && SonarCloudApp.getInstance().isLoggedIn()) {
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.AUTHENTICATE);
            builder.device(Api.Device.CLIENT).method(Api.Method.IDENTIFIER).identifier(SonarCloudApp.getInstance().getIdentifier())
                    .secret(SonarCloudApp.getInstance().getSavedData()).seq(SonarCloudApp.SEQ_VALUE);
            SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            showLoading();
        }
    }

    @Override
    public void onBackPressed() {
        if (isLoading()) {
            dismissLoading();
        } else {
            super.onBackPressed();
        }
    }
}
