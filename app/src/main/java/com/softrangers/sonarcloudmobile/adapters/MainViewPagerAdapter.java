package com.softrangers.sonarcloudmobile.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.softrangers.sonarcloudmobile.ui.fragments.ReceiversFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eduard on 3/12/16.
 */
public class MainViewPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> mFragmentList = new ArrayList<>();
    private List<String> mTitles = new ArrayList<>();
    private FragmentManager mFragmentManager;
    private Context mContext;


    public MainViewPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mFragmentManager = fm;
        mContext = context;
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof ReceiversFragment) {
            mFragmentManager.beginTransaction().remove((Fragment) object).commit();
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }

    @Override
    public Fragment getItem(int position) {
        return Fragment.instantiate(mContext, mFragmentList.get(position).getClass().getName(),
                mFragmentList.get(position).getArguments());
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public String getTitle(int position) {
        return mTitles.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return null;
    }

    public void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
        mTitles.add(title);
        notifyDataSetChanged();
    }

    public void clearLists() {
        mFragmentList.clear();
        mTitles.clear();
    }
}
