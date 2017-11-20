package com.frankgh.wpiparking;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.frankgh.wpiparking.auth.ChooserActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main";

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showLoginChooser();
        } else {
            startParkingService();
        }
    }
    // [END on_start_check_user]

    private void showLoginChooser() {
        Log.d(TAG, "currentUser is null. Launching ChooserActivity");
        startActivity(new Intent(this, ChooserActivity.class));
        Log.d(TAG, "Finish MainActivity");
        finish();
    }

    private void startParkingService() {
        Log.d(TAG, "startParkingService");
        // Start the Parking Service
        Intent i = new Intent(getApplicationContext(), ParkingService.class);
        getApplicationContext().startService(i);
    }
}
