package com.softrangers.sonarcloudmobile.ui.lock;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;

import com.softrangers.sonarcloudmobile.utils.lock.PatternLockUtils;

import java.util.List;

import me.zhanghai.android.patternlock.ConfirmPatternActivity;
import me.zhanghai.android.patternlock.PatternView;

public class ConfirmLockPattern extends ConfirmPatternActivity {

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        mRightButton.setVisibility(View.GONE);
    }

    @Override
    protected boolean isPatternCorrect(List<PatternView.Cell> pattern) {
        return PatternLockUtils.isPatternCorrect(pattern, this);
    }

    @Override
    protected void onForgotPassword() {
        // Finish with RESULT_FORGOT_PASSWORD.
        super.onForgotPassword();
    }
}
