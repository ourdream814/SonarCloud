package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.MainViewPagerAdapter;
import com.softrangers.sonarcloudmobile.ui.fragments.ReceiversFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.RecordFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.ScheduleFragment;
import com.softrangers.sonarcloudmobile.ui.fragments.SettingsFragment;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.SonarcloudRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnResponseListener {

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

        if (!SonarCloudApp.getInstance().isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            String identifier = SonarCloudApp.getInstance().getIdentifier();
            JSONObject identify = new JSONObject();
            try {
                identify.put(Api.COMMAND, Api.Command.IDENTIFIER);
                if (SonarCloudApp.NO_IDENTIFIER.equals(identifier)) {
                    identify.put(Api.ACTION, Api.Action.NEW);
                } else {
                    identify.put(Api.ACTION, Api.Action.RENEW);
                    identify.put(Api.IDENTIFIER, identifier);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            SonarcloudRequest request = SonarcloudRequest.getInstance();
            request.setOnResponseListener(this);
            request.execute(identify);
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Sorry")
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .create();
        dialog.show();
    }

    @Override
    public void onError() {

    }
}
