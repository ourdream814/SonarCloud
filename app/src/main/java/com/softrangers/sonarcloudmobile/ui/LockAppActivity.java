package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;

public class LockAppActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private SwitchCompat mEnableLocking;
    private SwitchCompat mEnableTouchID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_app);
        mEnableLocking = (SwitchCompat) findViewById(R.id.enable_locking_switchButton);
        mEnableLocking.setOnCheckedChangeListener(this);
        mEnableTouchID = (SwitchCompat) findViewById(R.id.enable_touch_switchButton);
        mEnableTouchID.setOnCheckedChangeListener(this);
    }

    public void enableAppLocking(View view) {
        mEnableLocking.setChecked(!mEnableLocking.isChecked());
    }

    public void setLockPattern(View view) {

    }

    public void enableTouchId(View view) {
        mEnableTouchID.setChecked(!mEnableTouchID.isChecked());
    }

    public void onBackButtonClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("lock");
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int buttonId = buttonView.getId();
        switch (buttonId) {
            case R.id.enable_locking_switchButton:
                if (isChecked) {
                    // TODO: 3/28/16 enable app locking
                    Toast.makeText(this, "Enabled app locking", Toast.LENGTH_SHORT).show();
                } else {
                    // TODO: 3/28/16 disable app locking
                    Toast.makeText(this, "Disabled app locking", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.enable_touch_switchButton:
                if (isChecked) {
                    // TODO: 3/28/16 enable touch id if it is possible
                    Toast.makeText(this, "Enabled touch ID", Toast.LENGTH_SHORT).show();
                } else {
                    // TODO: 3/28/16 disable touch id if it is possible
                    Toast.makeText(this, "Disabled touch ID", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
