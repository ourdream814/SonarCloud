package com.softrangers.sonarcloudmobile.utils.ui;

import android.support.v4.app.Fragment;


public abstract class BaseFragment extends Fragment {

    public abstract void onCommandFailure(String message);

    public abstract void onErrorOccurred();
}
