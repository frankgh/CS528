package com.frankgh.wpiparking.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.frankgh.wpiparking.MainActivity;
import com.frankgh.wpiparking.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/20/17.
 */

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "AuthBaseActivity";

    @VisibleForTesting
    public ProgressDialog mProgressDialog;

    // [START declare_auth]
    protected FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
    }

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    protected void startMainActivity() {
        Log.d(TAG, "startMainActivity");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

}

