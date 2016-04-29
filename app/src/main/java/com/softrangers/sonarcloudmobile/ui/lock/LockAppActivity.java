package com.softrangers.sonarcloudmobile.ui.lock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.RelativeLayout;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.lock.PatternLockUtils;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;

public class LockAppActivity extends BaseActivity {

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
        mItemSelected = ItemSelected.UNSELECTED;
        if (mEnableLocking.isChecked()) mSetPattern.setVisibility(View.VISIBLE);
        Intent lockIntent = getIntent();
        if (lockIntent != null) {
            isUnlocked = !lockIntent.getAction().isEmpty();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mItemSelected = ItemSelected.UNSELECTED;
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
        setResult(RESULT_OK, getIntent().setAction("lock"));
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (PatternLockUtils.checkConfirmPatternResult(this, requestCode, resultCode)) {
            setResult(RESULT_CANCELED, getIntent().setAction("lock"));
            isUnlocked = false;
            finish();
        } else {
            isUnlocked = true;
            switch (mItemSelected) {
                case SET_PATTERN:
                    Intent intent = new Intent(this, LockPatternActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    }

    enum ItemSelected {
        ENABLE_APP_LOCKING, SET_PATTERN, ENABLE_TOUCH_ID, UNSELECTED
    }
}
