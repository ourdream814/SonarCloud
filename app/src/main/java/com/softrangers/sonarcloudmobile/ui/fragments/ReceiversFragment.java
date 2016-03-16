package com.softrangers.sonarcloudmobile.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ReceiverListAdapter;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.ReceiverObserver;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReceiversFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        OnResponseListener, SwipeRefreshLayout.OnRefreshListener,
        ReceiverListAdapter.OnItemClickListener, ReceiverObserver {

    private AnimatedExpandableListView mListView;
    private RadioButton mReceivers;
    private RadioButton mGroups;
    private MainActivity mActivity;
    private SwipeRefreshLayout mRefreshLayout;
    private ReceiverListAdapter mReceiverListAdapter;
    private ArrayList<PASystem> mPASystems;
    private static boolean sent;

    public ReceiversFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receivers, container, false);
        mActivity = (MainActivity) getActivity();
        mListView = (AnimatedExpandableListView) view.findViewById(R.id.receivers_expandableListView);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.pa_list_swipeRefresh);
        mRefreshLayout.setOnRefreshListener(this);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.pa_list_selector);
        mReceivers = (RadioButton) view.findViewById(R.id.receivers_button);
        mReceivers.setChecked(true);
        mReceivers.setTextColor(getResources().getColor(R.color.colorPrimary));
        mGroups = (RadioButton) view.findViewById(R.id.groups_button);
        radioGroup.setOnCheckedChangeListener(this);
        mPASystems = new ArrayList<>();
        PASystem.addObserverToList(this);
        setUpListView(mPASystems);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // build a request
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.command(Api.Command.ORGANISATIONS);
        requestBuilder.userId(SonarCloudApp.getInstance().userId());
        requestBuilder.seq(SonarCloudApp.SEQ_VALUE);
        ResponseReceiver.getInstance().addOnResponseListener(this);
        // send request to server
        SonarCloudApp.socketService.sendRequest(requestBuilder.build().toJSON());
    }

    private void setUpListView(ArrayList<PASystem> paSystems) {
        mReceiverListAdapter = new ReceiverListAdapter(getActivity(), paSystems);
        mReceiverListAdapter.setOnItemClickListener(this);
        mListView.setAdapter(mReceiverListAdapter);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.receivers_button:
                mReceivers.setTextColor(getResources().getColor(R.color.colorPrimary));
                mGroups.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case R.id.groups_button:
                mReceivers.setTextColor(getResources().getColor(android.R.color.white));
                mGroups.setTextColor(getResources().getColor(R.color.colorPrimary));
                break;
        }
    }

    @Override
    public void onResponse(JSONObject response) {

        ResponseReceiver.getInstance().removeOnResponseListener(this);
        mPASystems = PASystem.build(response);

        SynchronousQueue<PASystem> queue = new SynchronousQueue<>();

        Executor executor = Executors.newFixedThreadPool(mPASystems.size());

        Log.e(this.getClass().getSimpleName(), "Started getting receivers");
        for (PASystem system : mPASystems) {
            SonarCloudApp.SEQ_VALUE += 1;
            system.setSeqValue(SonarCloudApp.SEQ_VALUE);
            executor.execute(new ReceiverRunnable(system, queue));
            Log.e(this.getClass().getSimpleName(), "Started a new thread for " + system.getName());
        }

        Log.e(this.getClass().getSimpleName(), "Done getReceivers loop");
    }

    class ReceiverRunnable implements Runnable {

        PASystem mPASystem;
        SynchronousQueue<PASystem> mQueue;

        public ReceiverRunnable(PASystem system, SynchronousQueue<PASystem> queue) {
            mPASystem = system;
            mQueue = queue;
        }

        @Override
        public void run() {
            try {
                while (sent) {
//                    Log.e(this.getClass().getSimpleName(), "waiting");
                }

                Log.e(this.getClass().getSimpleName(), "Building request");
                Request request = new Request.Builder().command(Api.Command.RECEIVERS)
                        .organisationId(mPASystem.getOrganisationId()).seq(mPASystem.getSeqValue()).build();

                ResponseReceiver.getInstance().addOnResponseListener(
                        new ReceiversGet(mQueue));

                Log.e(this.getClass().getSimpleName(), "Send request");
                SonarCloudApp.socketService.sendRequest(request.toJSON());
                Log.e(this.getClass().getSimpleName(), "Request sent");
                sent = true;
                mQueue.put(mPASystem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class ReceiversGet implements OnResponseListener {

        SynchronousQueue<PASystem> mQueue;

        public ReceiversGet(SynchronousQueue<PASystem> queue) {
            mQueue = queue;
        }

        @Override
        public void onResponse(JSONObject response) {
            try {
                Log.e(this.getClass().getSimpleName(), "Started onResponse");
                PASystem system = mQueue.take();
                ArrayList<Receiver> receivers = Receiver.build(response);
                for (Receiver receiver : receivers) {
                    for (PASystem sys : mPASystems) {
                        if (receiver.getSeqValue() == sys.getSeqValue()) {
                            sys.setReceivers(receivers);
                            Log.e(this.getClass().getSimpleName(), "Added receivers to " + sys.getName());
                            sent = false;
                            mActivity.dismissLoading();
                            return;
                        }
                        Log.e(this.getClass().getSimpleName(), "if condition ignored for " + sys.getName());
                    }
                }
                Log.e(this.getClass().getSimpleName(), "Loop finished");
                sent = false;
                mActivity.dismissLoading();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCommandFailure(String message) {
            mActivity.dismissLoading();
        }

        @Override
        public void onError() {
            mActivity.dismissLoading();
        }
    }

    @Override
    public void onCommandFailure(String message) {
        mActivity.alertUserAboutError(mActivity.getString(R.string.error), message);
        mActivity.dismissLoading();
    }

    @Override
    public void onError() {
        mActivity.alertUserAboutError(mActivity.getString(R.string.error), mActivity.getString(R.string.unknown_error));
        mActivity.dismissLoading();
    }

    @Override
    public void onRefresh() {
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onChildClick(Receiver receiver, int position) {
        if (receiver.isSelected()) {
            receiver.setIsSelected(false);
            Snackbar.make(mListView, receiver.getName() + " deselected, Id = " + receiver.getReceiverId(), Snackbar.LENGTH_SHORT).show();
        } else {
            receiver.setIsSelected(true);
            Snackbar.make(mListView, receiver.getName() + " selected, Id = " + receiver.getReceiverId(), Snackbar.LENGTH_SHORT).show();
        }

        mReceiverListAdapter.notifyDataSetChanged();
    }

    @Override
    public void update(PASystem paSystem, ArrayList<Receiver> receivers) {
        Log.e(this.getClass().getSimpleName(), paSystem.getName() + ": " + receivers.size());
        mReceiverListAdapter.addItem(paSystem);
    }
}
