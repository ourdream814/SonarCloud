package com.softrangers.sonarcloudmobile.ui.lock;

import com.softrangers.sonarcloudmobile.utils.lock.PatternLockUtils;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;

import java.util.List;

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
