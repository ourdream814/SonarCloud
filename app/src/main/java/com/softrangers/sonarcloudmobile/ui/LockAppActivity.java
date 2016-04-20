package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.utils.PatternLockUtils;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import java.util.regex.Pattern;

import me.zhanghai.android.patternlock.PatternUtils;

public class LockAppActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private SwitchCompat mEnableLocking;
    private ItemSelected mItemSelected;
    private RelativeLayout mSetPattern;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_app);
        mEnableLocking = (SwitchCompat) findViewById(R.id.enable_locking_switchButton);
        mSetPattern = (RelativeLayout) findViewById(R.id.lock_activity_setPattern);
        mEnableLocking.setChecked(SonarCloudApp.getInstance().isAppLocked());
        if (mEnableLocking.isChecked()) mSetPattern.setVisibility(View.VISIBLE);
    }

    public void enableAppLocking(View view) {
        mItemSelected = ItemSelected.ENABLE_APP_LOCKING;
        mEnableLocking.setChecked(!mEnableLocking.isChecked());
        SonarCloudApp.getInstance().setAppIsLocked(mEnableLocking.isChecked());
        mSetPattern.setVisibility(mEnableLocking.isChecked() ? View.VISIBLE : View.GONE);
    }

    public void setLockPattern(View view) {
        mItemSelected = ItemSelected.SET_PATTERN;
        if (PatternLockUtils.hasPattern(this)) {
            PatternLockUtils.confirmPatternIfHas(this);
        } else {
            Intent intent = new Intent(this, LockPatternActivity.class);
            startActivity(intent);
        }
    }

    public void onBackButtonClick(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("lock");
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int buttonId = buttonView.getId();
        switch (buttonId) {
            case R.id.enable_locking_switchButton:

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!PatternLockUtils.checkConfirmPatternResult(this, requestCode, resultCode)) {
            switch (mItemSelected) {
                case SET_PATTERN:
                    Intent intent = new Intent(this, LockPatternActivity.class);
                    startActivity(intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    enum ItemSelected {
        ENABLE_APP_LOCKING, SET_PATTERN, ENABLE_TOUCH_ID
    }
}
