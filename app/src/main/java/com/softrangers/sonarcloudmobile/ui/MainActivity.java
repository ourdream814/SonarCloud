package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.MainViewPagerAdapter;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.ui.fragments.ReceiversFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.RecordFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.ScheduleFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.SettingsFragment;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;
import com.softrangers.sonarcloudmobile.utils.api.SocketService;

import org.json.JSONObject;

public class MainActivity extends BaseActivity implements OnResponseListener {

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
        setUpTabs();

        ResponseReceiver.getInstance().addOnResponseListener(this);

        if (!SonarCloudApp.getInstance().isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setUpTabs() {
        mPagerAdapter.addFragment(new ReceiversFragment(), getString(R.string.select_pa_system));
        mPagerAdapter.addFragment(new RecordFragment(), getString(R.string.make_announcement));
        mPagerAdapter.addFragment(new ScheduleFragment(), getString(R.string.schedule_announcement));
        mPagerAdapter.addFragment(new SettingsFragment(), getString(R.string.settings));
        mViewPager.setAdapter(mPagerAdapter);
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

    }

    @Override
    public void onCommandFailure(String message) {
        alertUserAboutError(message);
    }

    @Override
    public void onError() {
        alertUserAboutError("Something went wrong, please try again");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unbindService();
    }

    OnResponseListener authListener = new OnResponseListener() {
        @Override
        public void onResponse(JSONObject response) {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.command(Api.Command.RECEIVERS);
            requestBuilder.userId(SonarCloudApp.getInstance().userId());
            socketService.sendRequest(requestBuilder.build().toJSON());
        }

        @Override
        public void onCommandFailure(String message) {
            alertUserAboutError(message);
        }

        @Override
        public void onError() {
            alertUserAboutError("Something went wrong, please try again");
        }
    };

    @Override
    public void onServiceBound(SocketService socketService) {
        if (socketService != null && SonarCloudApp.getInstance().isLoggedIn()) {
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.AUTHENTICATE);
            builder.device(Api.Device.CLIENT).method(Api.Method.USER).identifier(SonarCloudApp.getInstance().getIdentifier())
                    .secret(SonarCloudApp.getInstance().getSavedData());
            socketService.sendRequest(builder.build().toJSON());
        }
    }
}
