package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.SonarcloudRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity implements OnResponseListener {


    private static final String TAG = LoginActivity.class.getSimpleName();

    private EditText mEmail;
    private EditText mPassword;
    private ProgressBar mProgressBar;
    private Button mSignIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
    }

    /**
     * Called when the Sig In button is pressed
     *
     * @param view link to Sign In button instance
     */
    public void sigIn(View view) {
        String email = String.valueOf(mEmail.getText());
        if (email.equals("")) {
            mEmail.setError("Please enter a valid email address");
            return;
        }
        String password = String.valueOf(mPassword.getText());
        if (password.equals("")) {
            mPassword.setError("Please enter a valid password");
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        mProgressBar.setVisibility(View.VISIBLE);
        mSignIn.setEnabled(false);

        // Create a JSON object for server request
        JSONObject request = new JSONObject();
        try {
            request.put(Api.COMMAND, Api.Command.AUTHENTICATE);
            request.put(Api.METHOD, Api.Method.USER);
            request.put(Api.DEVICE, Api.Device.CLIENT);
            request.put(Api.EMAIL, user.getEmail());
            request.put(Api.PASSWORD, user.getPassword());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new SonarcloudRequest(this).execute(request);
    }

    @Override
    public void onResponse(JSONObject response) {
        try {
            String id = response.getString("userID");
            SonarCloudApp.getInstance().saveUserLoginStatus(true, id);
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mSignIn.setEnabled(true);
            }
        });
    }

    @Override
    public void onCommandFailure(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Sorry")
                        .setMessage(message)
                        .setPositiveButton("ok", null)
                        .create();
                dialog.show();
                mProgressBar.setVisibility(View.GONE);
                mSignIn.setEnabled(true);
            }
        });
    }

    @Override
    public void onError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mSignIn.setEnabled(true);
            }
        });
    }
}
