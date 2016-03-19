package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ReceiverListAdapter;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import java.util.ArrayList;

public class SelectPASystemActivity extends BaseActivity implements
        ReceiverListAdapter.OnItemClickListener {

    public static final String PA_BUNDLE_KEY = "key for PAs from bundle";
    public static final String RECEIVERS_RESULT = "key for receivers list from result";

    private ArrayList<PASystem> mPASystems;
    private ArrayList<Receiver> mReceivers;
    private ReceiverListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pasystem);
        Intent intent = getIntent();
        mReceivers = new ArrayList<>();
        mPASystems = intent.getExtras().getParcelableArrayList(PA_BUNDLE_KEY);
        AnimatedExpandableListView listView = (AnimatedExpandableListView) findViewById(R.id.select_pa_activityList);
        mAdapter = new ReceiverListAdapter(this, mPASystems);
        mAdapter.setOnItemClickListener(this);

        assert listView != null;
        listView.setAdapter(mAdapter);
    }

    public void save(View view) {
        Intent intent = new Intent();
        intent.putExtra(RECEIVERS_RESULT, mReceivers);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void cancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onChildClick(Receiver receiver, int position) {
        receiver.setIsSelected(true);
        mAdapter.notifyDataSetChanged();
        mReceivers.add(receiver);
    }
}
