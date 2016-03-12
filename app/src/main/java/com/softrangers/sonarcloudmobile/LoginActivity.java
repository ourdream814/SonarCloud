package com.softrangers.sonarcloudmobile;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private static final String URL = "woodward.parentglue.com";
    private static final int PORT = 6523;

    private EditText mEmail;
    private EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mEmail = (EditText) findViewById(R.id.email_label);
        mEmail.setTypeface(SonarCloudApp.avenirBook);
        mPassword = (EditText) findViewById(R.id.password_label);
        mPassword.setTypeface(SonarCloudApp.avenirBook);
        Button signIn = (Button) findViewById(R.id.signin_button);
        signIn.setTypeface(SonarCloudApp.avenirMedium);
    }

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
        user.setmEmail(email);
        user.setmPassword(password);
        new LoginTask().execute(user);
    }

    class LoginTask extends AsyncTask<User, String, String> {

        Socket socket;

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(LoginActivity.this, Arrays.toString(values), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.onPostExecute(s);
        }

        @Override
        protected String doInBackground(User... params) {
            String response;
            StringBuilder stringBuilder = new StringBuilder();
            try {
                socket = new Socket(URL, PORT);
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                JSONObject data = new JSONObject();
                data.put("command", "authenticate")
                        .put("device", "client")
                        .put("method", "user")
                        .put("email", params[0].getmEmail())
                        .put("password", params[0].getmPassword());
                writer.println(data + "\n");
                writer.flush();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));


                while ((response = reader.readLine()) != null) {
                    stringBuilder.append(response);
                    publishProgress(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return stringBuilder.toString();
        }
    }
}
