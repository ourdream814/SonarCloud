package com.softrangers.sonarcloudmobile.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ConnectionReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {

    private EditText mEmail;
    private EditText mPassword;
    private ProgressBar mProgressBar;
    private Button mSignIn;
    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        IntentFilter intentFilter = new IntentFilter(Api.Command.AUTHENTICATE);
        intentFilter.addAction(Api.Command.IDENTIFIER);
        intentFilter.addAction(Api.EXCEPTION);
        registerReceiver(mLoginReceiver, intentFilter);

        ConnectionReceiver.getInstance().removeAllListeners();

        // Instantiate email input field
        mEmail = (EditText) findViewById(R.id.email_label);
        assert mEmail != null;
        mEmail.setTypeface(SonarCloudApp.avenirBook);

        // Instantiate password input field
        mPassword = (EditText) findViewById(R.id.password_label);
        assert mPassword != null;
        mPassword.setTypeface(SonarCloudApp.avenirBook);

        // Instantiate progress bar to show on the UI while loading
        mProgressBar = (ProgressBar) findViewById(R.id.login_progressBar);

        // Instantiate the sign in button
        mSignIn = (Button) findViewById(R.id.signin_button);
        assert mSignIn != null;
        mSignIn.setTypeface(SonarCloudApp.avenirMedium);
        mUser = new User();
        String email = SonarCloudApp.getInstance().getUserEmail();
        String password = SonarCloudApp.getInstance().getUserPass();
        if (email != null && password != null) {
            mEmail.setText(email);
            mPassword.setText(password);
        }
    }

    BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
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
                    case Api.Command.AUTHENTICATE:
                        onResponseSucceed(jsonResponse);
                        break;
                    case Api.Command.IDENTIFIER:
                        onIdentifierReady(jsonResponse);
                        break;
                }
            } catch (Exception e) {
                onErrorOccurred();
            }
        }
    };

    /**
     * Called when the Sig In button is pressed
     * @param view link to Sign In button instance
     */
    public void sigIn(View view) throws JSONException {
        String email = String.valueOf(mEmail.getText());

        // Check email field for null value
        if (email.equals("")) {
            mEmail.setError("Please left_to_right a valid email address");
            return;
        }

        // Check password field for null value
        String password = String.valueOf(mPassword.getText());
        if (password.equals("")) {
            mPassword.setError("Please left_to_right a valid password");
            return;
        }

        // Save email and password to User object
        mUser.setEmail(email);
        mUser.setPassword(password);

        // Show visual progress
        mProgressBar.setVisibility(View.VISIBLE);

        // Disable Sign In button while loading
        mSignIn.setClickable(false);
        mSignIn.setVisibility(View.INVISIBLE);

        // Create a JSON request for server
        JSONObject req = new Request.Builder().command(Api.Command.AUTHENTICATE)
                .method(Api.Method.USER)
                .device(Api.Device.CLIENT)
                .email(mUser.getEmail())
                .password(mUser.getPassword())
                .seq(SonarCloudApp.SEQ_VALUE)
                .build().toJSON();
        // Send the request
        SonarCloudApp.dataSocketService.sendRequest(req);
    }


    /**
     * Called to notify about server response
     * @param response object which contains desired data
     */
    public void onResponseSucceed(JSONObject response) {
        try {
            // Get user id from the response object
            String id = response.getString("userID");

            mUser.setId(id);
            SonarCloudApp.getInstance().saveUserLoginDate(mUser.getEmail(), mUser.getPassword());

            // request identifier from server
            SonarCloudApp.getInstance().requestUserIdentifier();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called to notify about server error
     * @param message about the occurred error
     */
    public void onCommandFailure(final String message) {
        // show an alert dialog to user with server message
        alertUserAboutError(getString(R.string.login_error), message);
        SonarCloudApp.dataSocketService.restartConnection();
        // hide loading ui
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mSignIn.setClickable(true);
                mSignIn.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Called to notify any other errors such exceptions and so on
     */
    public void onErrorOccurred() {
        // show an alert dialog to user that something went wrong
        Snackbar.make(mSignIn, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
        // hide loading progress
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mSignIn.setClickable(true);
                mSignIn.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onIdentifierReady(JSONObject response) {
        try {
            // Save identifier and secret to app preferences
            SonarCloudApp.getInstance().saveIdentifier(response.getString(Api.IDENTIFIER));
            SonarCloudApp.getInstance().saveUserSecret(response.getString(Api.SECRET));

            // save identifier and secret to user object for this session
            mUser.setIdentifier(SonarCloudApp.getInstance().getIdentifier());
            mUser.setSecret(SonarCloudApp.getInstance().getSavedData());

            // set the shared user object
            SonarCloudApp.user = mUser;

            // start MainActivity and finish current
            Intent main = new Intent(LoginActivity.this, MainActivity.class);
            main.setAction(MainActivity.ACTION_LOGIN);
            setResult(RESULT_OK, main);
            SonarCloudApp.getInstance().saveUserLoginStatus(true, mUser.getId());
            finish();
        } catch (Exception e) {
            Snackbar.make(mSignIn, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_LOGIN);
        setResult(RESULT_CANCELED, intent);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLoginReceiver);
    }
}
