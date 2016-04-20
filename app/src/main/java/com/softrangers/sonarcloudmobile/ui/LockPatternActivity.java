package com.softrangers.sonarcloudmobile.ui;

import com.softrangers.sonarcloudmobile.utils.PatternLockUtils;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;

import java.util.List;

import me.zhanghai.android.patternlock.PatternUtils;
import me.zhanghai.android.patternlock.PatternView;
import me.zhanghai.android.patternlock.SetPatternActivity;

public class LockPatternActivity extends SetPatternActivity {

    @Override
    protected void onSetPattern(List<PatternView.Cell> pattern) {
        super.onSetPattern(pattern);
        BaseActivity.isUnlocked = true;
        PatternLockUtils.setPattern(pattern, this);
    }
}
