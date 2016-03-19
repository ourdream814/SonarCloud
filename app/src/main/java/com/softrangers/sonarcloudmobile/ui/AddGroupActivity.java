package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Group;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;

import org.json.JSONObject;

import java.util.ArrayList;

public class AddGroupActivity extends BaseActivity {

    public static final String GROUP_EDIT_BUNDLE = "bundle key for editable group";
    public static final String PA_SUSTEMS_BUNDLE = "bundle key for PA systems list";
    public static final String GROUP_RESULT_BUNDLE = "bundle key for group result";

    private TextView mToolbarTitle;

    private EditText mNameEditText;
    private EditText mPinEditText;
    private EditText mPinConfirmEditText;
    private Button mSelectPAButton;
    private String mAction;

    private Group mGroup;
    private ArrayList<PASystem> mPASystems;
    private ArrayList<Receiver> mReceivers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);

        instantiateAllViews();

        Intent intent = getIntent();
        if (intent == null) return;
        mPASystems = intent.getExtras().getParcelableArrayList(PA_SUSTEMS_BUNDLE);
        mAction = intent.getAction();
        switch (mAction) {
            case Api.ACTION_ADD_GROUP:
                mGroup = new Group();
                Toast.makeText(this, "Started to add new group", Toast.LENGTH_SHORT).show();
                break;
            case Api.ACTION_EDIT_GROUP:
                mGroup = intent.getExtras().getParcelable(GROUP_EDIT_BUNDLE);
                mReceivers = mGroup.getReceivers();
                mToolbarTitle.setText(getString(R.string.edit_group));
                mNameEditText.setText(mGroup.getName());
                mSelectPAButton.setText(mGroup.getReceivers().size() + " " + getString(R.string.pa_systems));
                break;
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
            setResult(RESULT_CANCELED);
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
            builder.command(Api.Command.CREATE_GROUP);
            builder.pin(mGroup.getPin());
            builder.name(mGroup.getName());

            ArrayList<Integer> receivers = new ArrayList<>();
            for (Receiver receiver : mReceivers) {
                receivers.add(receiver.getReceiverId());
            }
            builder.receivers(receivers);
            ResponseReceiver.getInstance().clearResponseListenersList();
            ResponseReceiver.getInstance().addOnResponseListener(new AddGroupListener());
            SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
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
        switch (resultCode) {
            case RESULT_OK:
                mReceivers = data.getExtras().getParcelableArrayList(SelectPASystemActivity.RECEIVERS_RESULT);
                if (mReceivers == null) mReceivers = new ArrayList<>();
                mSelectPAButton.setText(mReceivers.size() + " " + getString(R.string.pa_systems));
                break;
        }
    }

    class AddGroupListener implements OnResponseListener {

        @Override
        public void onResponse(JSONObject response) {
            mGroup = Group.buildSingle(response);
            Intent intent = new Intent();
            intent.setAction(Api.ACTION_ADD_GROUP);
            intent.putExtra(GROUP_RESULT_BUNDLE, mGroup);
            setResult(RESULT_OK, intent);
            dismissLoading();
            finish();
        }

        @Override
        public void onCommandFailure(String message) {
            dismissLoading();
            Snackbar.make(mSelectPAButton, message, Snackbar.LENGTH_SHORT).show();
        }

        @Override
        public void onError() {
            dismissLoading();
            Snackbar.make(mSelectPAButton, getString(R.string.unknown_error),
                    Snackbar.LENGTH_SHORT).show();
        }
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
