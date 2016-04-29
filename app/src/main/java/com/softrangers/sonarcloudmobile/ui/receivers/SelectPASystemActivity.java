package com.softrangers.sonarcloudmobile.ui.receivers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ReceiverListAdapter;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.utils.lock.PatternLockUtils;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import java.util.ArrayList;

public class SelectPASystemActivity extends BaseActivity implements
        ReceiverListAdapter.OnItemClickListener, ExpandableListView.OnGroupClickListener {

    public static final String PA_BUNDLE_KEY = "key for PAs from bundle";
    public static final String RECEIVERS_RESULT = "key for receivers list from result";

    private ArrayList<Receiver> mReceivers;
    private ReceiverListAdapter mAdapter;
    private AnimatedExpandableListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pasystem);
        Intent intent = getIntent();
        mReceivers = new ArrayList<>();
        ArrayList<PASystem> PASystems = intent.getExtras().getParcelableArrayList(PA_BUNDLE_KEY);
        mListView = (AnimatedExpandableListView) findViewById(R.id.select_pa_activityList);
        addSelectedReceivers(PASystems);
        mAdapter = new ReceiverListAdapter(this, PASystems);
        mAdapter.setOnItemClickListener(this);

        assert mListView != null;
        mListView.setAdapter(mAdapter);
        mListView.setOnGroupClickListener(this);
        isUnlocked = true;
    }

    private void addSelectedReceivers(ArrayList<PASystem> systems) {
        for (PASystem system : systems) {
            for (Receiver receiver : system.getReceivers()) {
                if (receiver.isSelected()) {
                    mReceivers.add(receiver);
                }
            }
        }
    }

    public void save(View view) {
        Intent intent = new Intent();
        intent.putExtra(RECEIVERS_RESULT, mReceivers);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void cancel(View view) {
        Intent intent = new Intent();
        intent.putExtra("cancel", "cancel");
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (PatternLockUtils.checkConfirmPatternResult(this, requestCode, resultCode)) {
            finish();
        } else {
            isUnlocked = true;
        }
    }

    @Override
    public void onChildClick(Receiver receiver, int position) {
        receiver.setIsSelected(!receiver.isSelected());
        mAdapter.notifyDataSetChanged();

        if (receiver.isSelected())
            mReceivers.add(receiver);
        else
            mReceivers.remove(receiver);
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        // We call collapseGroupWithAnimation(int) and
        // expandGroupWithAnimation(int) to animate group
        // expansion/collapse.
        if (mListView.isGroupExpanded(groupPosition)) {
            mListView.collapseGroupWithAnimation(groupPosition);
        } else {
            mListView.expandGroupWithAnimation(groupPosition);
        }
        return true;
    }
}
