package com.frankgh.wpiparking;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.frankgh.wpiparking.auth.ChooserActivity;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Map;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/9/17.
 */

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    /**
     * Provides the authenticated user for Firebase.
     */
    protected FirebaseAuth mAuth;
    /**
     * Provides access to the Awareness API.
     */
    protected FenceClient mFenceClient;
    /**
     * The progress dialog
     */
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            showLoginChooser();
            return;
        }

        // Get a reference to the awareness fence client
        mFenceClient = Awareness.getFenceClient(this);
    }

    public void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

    /**
     * Shows the login screen
     */
    protected void showLoginChooser() {
        if (mFenceClient != null && Constants.WPI_AREA_LANDMARKS.size() > 0) {
            FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();
            for (Map.Entry<String, LatLng> entry : Constants.WPI_AREA_LANDMARKS.entrySet()) {
                builder.removeFence(entry.getKey() + "-dwell");
                builder.removeFence(entry.getKey() + "-exiting");
            }

            mFenceClient.updateFences(builder.build())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            close();
                        }
                    });
        } else {
            close();
        }
    }

    /**
     * Start ChooserActivity and finish this Activity
     */
    private void close() {
        startActivity(new Intent(BaseActivity.this, ChooserActivity.class));
        Log.d(TAG, "Finish BaseActivity");
        finish();
    }

    protected String getUid() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }
}
