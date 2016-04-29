package com.softrangers.sonarcloudmobile.ui.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Group;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.lock.PatternLockUtils;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.DataSocketService;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class AddGroupActivity extends BaseActivity {

    public static final String GROUP_EDIT_BUNDLE = "bundle key for editable group";
    public static final String PA_SYSTEMS_BUNDLE = "bundle key for PA systems list";
    public static final String GROUP_RESULT_BUNDLE = "bundle key for group result";

    private static String command = Api.Command.CREATE_GROUP;

    private TextView mToolbarTitle;

    private EditText mNameEditText;
    private EditText mPinEditText;
    private EditText mPinConfirmEditText;
    private Button mSelectPAButton;
    private String mAction;

    private Group mGroup;
    private ArrayList<PASystem> mPASystems;
    private ArrayList<Receiver> mReceivers;
    public static DataSocketService dataSocketService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        Intent socketIntent = new Intent(this, DataSocketService.class);
        bindService(socketIntent, mDataServiceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter intentFilter = new IntentFilter(Api.Command.UPDATE_GROUP);
        intentFilter.addAction(Api.Command.CREATE_GROUP);
        intentFilter.addAction(Api.EXCEPTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
        instantiateAllViews();

        Intent intent = getIntent();
        if (intent == null) return;
        mPASystems = intent.getExtras().getParcelableArrayList(PA_SYSTEMS_BUNDLE);
        mAction = intent.getAction();
        switch (mAction) {
            case Api.ACTION_ADD_GROUP:
                command = Api.Command.CREATE_GROUP;
                mGroup = new Group();
                clearPASelections(mPASystems);
                isUnlocked = true;
                break;
            case Api.ACTION_EDIT_GROUP:
                command = Api.Command.UPDATE_GROUP;
                mGroup = intent.getExtras().getParcelable(GROUP_EDIT_BUNDLE);
                assert mGroup != null;
                mGroup.setIsSelected(true);
                mReceivers = mGroup.getReceivers();
                setSelectedReceivers(mReceivers);
                mToolbarTitle.setText(getString(R.string.edit_group));
                mNameEditText.setText(mGroup.getName());
                isUnlocked = true;
                mSelectPAButton.setText(mGroup.getReceivers().size() + " " + getString(R.string.pa_systems));
                break;
        }
    }

    // needed to bind DataSocketService to current class
    protected ServiceConnection mDataServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // get the service instance
            dataSocketService = ((DataSocketService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // remove service instance
            dataSocketService = null;
        }
    };

    private void setSelectedReceivers(ArrayList<Receiver> destination) {
        for (Receiver r : destination) {
            for (int i = 0; i < mPASystems.size(); i++) {
                for (Receiver receiver : mPASystems.get(i).getReceivers()) {
                    if (receiver.getReceiverId() == r.getReceiverId()) {
                        receiver.setIsSelected(true);
                    }
                }
            }
        }
    }

    private void clearPASelections(ArrayList<PASystem> systems) {
        for (int i = 0; i < systems.size(); i++) {
            for (Receiver receiver : systems.get(i).getReceivers()) {
                receiver.setIsSelected(false);
            }
        }

    }

    private void instantiateAllViews() {
        mToolbarTitle = (TextView) findViewById(R.id.add_edit_group_toolbarTitle);

        ImageButton closeButton = (ImageButton) findViewById(R.id.select_pa_cancelButton);
        assert closeButton != null;
        closeButton.setOnClickListener(mOnCloseClickListener);

        ImageButton saveButton = (ImageButton) findViewById(R.id.select_pa_saveButton);
        assert saveButton != null;
        saveButton.setOnClickListener(mOnSaveClickListener);

        mNameEditText = (EditText) findViewById(R.id.add_edit_group_nameEditText);
        mPinEditText = (EditText) findViewById(R.id.add_edit_group_pinEditText);
        mPinConfirmEditText = (EditText) findViewById(R.id.add_edit_group_pin_confirmEditText);

        mSelectPAButton = (Button) findViewById(R.id.add_edit_group_selectPAButton);
        assert mSelectPAButton != null;
        mSelectPAButton.setOnClickListener(mOnSelectPAClickListener);
    }

    private View.OnClickListener mOnCloseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    private View.OnClickListener mOnSaveClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String groupName = String.valueOf(mNameEditText.getText());
            if (groupName.equals("")) {
                mNameEditText.setError(getString(R.string.enter_group_name));
                return;
            }

            String pinString = String.valueOf(mPinEditText.getText());
            if (!isPinValid(pinString)) {
                mPinEditText.setError(getString(R.string.enter_valid_pin));
                return;
            }

            String pinConfirmString = String.valueOf(mPinConfirmEditText.getText());
            if (!pinConfirmString.equals(pinString)) {
                mPinConfirmEditText.setError(getString(R.string.does_not_match));
                return;
            }

            if (mReceivers == null || mReceivers.size() <= 0) {
                alertUserAboutError(getString(R.string.save_error), getString(R.string.please_select_pa));
                return;
            }

            int pin = Integer.parseInt(String.valueOf(mPinEditText.getText()));
            mGroup.setName(groupName);
            mGroup.setPin(pin);
            mGroup.setReceivers(mReceivers);

            Request.Builder builder = new Request.Builder();
            builder.command(command);
            if (mAction.equals(Api.ACTION_EDIT_GROUP)) {
                builder.receiverGroupID(mGroup.getGroupID());
            }
            builder.pin(mGroup.getPin());
            builder.name(mGroup.getName());

            ArrayList<Integer> receivers = new ArrayList<>();
            for (Receiver receiver : mReceivers) {
                receivers.add(receiver.getReceiverId());
            }
            builder.receivers(receivers);
            JSONObject object = builder.build().toJSON();
            dataSocketService.sendRequest(object);
            showLoading();
        }
    };

    private boolean isPinValid(String pin) {
        if (pin.equals("")) {
            mPinConfirmEditText.setError(getString(R.string.enter_pin));
            return false;
        }
        if (pin.length() < 1) {
            mPinConfirmEditText.setError(getString(R.string.short_pin));
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (PatternLockUtils.checkConfirmPatternResult(this, requestCode, resultCode)) {
            finish();
            return;
        } else {
            isUnlocked = true;
        }
        if (data != null) {
            switch (resultCode) {
                case RESULT_OK:
                    mReceivers = data.getExtras().getParcelableArrayList(SelectPASystemActivity.RECEIVERS_RESULT);
                    if (mReceivers == null) mReceivers = new ArrayList<>();
                    updatePAReceivers(mReceivers);
                    mSelectPAButton.setText(mReceivers.size() + " " + getString(R.string.pa_systems));
                    break;
            }
        } else {
            finish();
        }
    }

    private void updatePAReceivers(ArrayList<Receiver> receivers) {
        for (Receiver receiver : receivers) {
            for (PASystem system : mPASystems) {
                for (Receiver r : system.getReceivers()) {
                    if (receiver.getReceiverId() == r.getReceiverId()) {
                        r.setIsSelected(receiver.isSelected());
                    }
                }
            }
        }
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                boolean success = jsonResponse.optBoolean("success", false);
                if (!success) {
                    String message = jsonResponse.optString("message", getString(R.string.unknown_error));
                    onCommandFailure(message);
                    return;
                }
                switch (action) {
                    case Api.Command.CREATE_GROUP:
                        onResponseSucceed(jsonResponse);
                        break;
                    case Api.Command.UPDATE_GROUP:
                        onResponseSucceed(jsonResponse);
                        break;
                }
            } catch (Exception e) {
                onErrorOccurred();
            }
        }
    };

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddGroupActivity.this, MainActivity.class);
        intent.setAction("Add group canceled");
        setResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mDataServiceConnection);
        unregisterReceiver(mBroadcastReceiver);
    }

    private void onResponseSucceed(JSONObject response) {
        dismissLoading();
        mGroup = Group.buildSingle(response);
        Intent intent = new Intent();
        intent.setAction(Api.ACTION_ADD_GROUP);
        intent.putExtra(GROUP_RESULT_BUNDLE, mGroup);
        setResult(RESULT_OK, intent);
        unregisterReceiver(mBroadcastReceiver);
        finish();
    }

    private void onCommandFailure(String message) {
        dismissLoading();
        Snackbar.make(mSelectPAButton, message, Snackbar.LENGTH_SHORT).show();
    }

    private void onErrorOccurred() {
        dismissLoading();
        Snackbar.make(mSelectPAButton, getString(R.string.unknown_error),
                Snackbar.LENGTH_SHORT).show();
    }

    private View.OnClickListener mOnSelectPAClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(AddGroupActivity.this, SelectPASystemActivity.class);
            intent.putExtra(SelectPASystemActivity.PA_BUNDLE_KEY, mPASystems);
            startActivityForResult(intent, 1);
        }
    };
}
