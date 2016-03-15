package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity implements OnResponseListener {

    private EditText mEmail;
    private EditText mPassword;
    private ProgressBar mProgressBar;
    private Button mSignIn;
    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Register current activity as a response handler
//        ResponseReceiver.getInstance().addOnResponseListener(this);

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
    }

    /**
     * Called when the Sig In button is pressed
     * @param view link to Sign In button instance
     */
    public void sigIn(View view) throws JSONException {
        String email = String.valueOf(mEmail.getText());

        // Check email field for null value
        if (email.equals("")) {
            mEmail.setError("Please enter a valid email address");
            return;
        }

        // Check password field for null value
        String password = String.valueOf(mPassword.getText());
        if (password.equals("")) {
            mPassword.setError("Please enter a valid password");
            return;
        }

        // Save email and password to User object
        mUser.setEmail(email);
        mUser.setPassword(password);

        // Show visual progress
        mProgressBar.setVisibility(View.VISIBLE);

        // Disable Sign In button while loading
        mSignIn.setEnabled(false);

        // Create a JSON request for server
        JSONObject req = new Request.Builder().command(Api.Command.AUTHENTICATE)
                .method(Api.Method.USER)
                .device(Api.Device.CLIENT)
                .email(mUser.getEmail())
                .password(mUser.getPassword())
                .build().toJSON();

        ResponseReceiver.getInstance().removeOnResponseListener(identifierListener);
        ResponseReceiver.getInstance().addOnResponseListener(this);
        // Send the request
        SonarCloudApp.socketService.sendRequest(req);
    }


    /**
     * Called by ResponseReceiver to notify about server response
     * @param response object which contains desired data
     */
    @Override
    public void onResponse(JSONObject response) {
        try {
            // Get user id from the response object
            String id = response.getString("userID");

            // Save user id to preferences for future uses
            SonarCloudApp.getInstance().saveUserLoginStatus(true, id);

            // Get user identifier from preferences
            String identifier = SonarCloudApp.getInstance().getIdentifier();
            mUser.setId(id);

            // Start building a request to either create a new or renew existing identifier
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.command(Api.Command.IDENTIFIER);
            if (SonarCloudApp.NO_IDENTIFIER.equals(identifier)) {
                requestBuilder.action(Api.Action.NEW);
            } else {
                requestBuilder.action(Api.Action.RENEW);
                requestBuilder.identifier(identifier);
            }

            // Remove curent response listener and register the identifier listener
            ResponseReceiver.getInstance().removeOnResponseListener(this);
            ResponseReceiver.getInstance().addOnResponseListener(identifierListener);

            // Send the created request to server
            SonarCloudApp.socketService.sendRequest(requestBuilder.build().toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called by ResponseReceiver to notify about server error
     * @param message about the occurred error
     */
    @Override
    public void onCommandFailure(final String message) {
        // show an alert dialog to user with server message
        alertUserAboutError(getString(R.string.login_error), message);
        SonarCloudApp.socketService.restartConnection();
        // hide loading ui
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mProgressBar.setVisibility(View.GONE);
                mSignIn.setEnabled(true);
            }
        });
    }

    /**
     * Called by ResponseReceiver to notify any other errors such exceptions and so on
     */
    @Override
    public void onError() {
        // show an alert dialog to user that something went wrong
        alertUserAboutError(getString(R.string.error), getString(R.string.unknown_error));
        // hide loading progress
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mSignIn.setEnabled(true);
            }
        });
    }

    OnResponseListener identifierListener = new OnResponseListener() {

        /**
         * Called by ResponseReceiver to notify about server response
         * @param response object which contains desired data
         */
        @Override
        public void onResponse(JSONObject response) {
            // hide loading ui
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                    mSignIn.setEnabled(true);
                }
            });

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
                startActivity(main);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                alertUserAboutError(getString(R.string.error), getString(R.string.unknown_error));
            }

        }

        /**
         * Called by ResponseReceiver to notify about server error
         * @param message about the occurred error
         */
        @Override
        public void onCommandFailure(final String message) {
            // show an alert dialog to user with server message
            alertUserAboutError(getString(R.string.error), message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                    mSignIn.setEnabled(true);
                }
            });
        }

        /**
         * Called by ResponseReceiver to notify any other errors such exceptions and so on
         */
        @Override
        public void onError() {
            // show an alert dialog to user that something went wrong
            alertUserAboutError(getString(R.string.error), getString(R.string.unknown_error));
            // hide loading progress
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                    mSignIn.setEnabled(true);
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove all listeners from this activity
        ResponseReceiver.getInstance().removeOnResponseListener(this);
        ResponseReceiver.getInstance().removeOnResponseListener(identifierListener);
    }
}
