package com.frankgh.project3;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    @BindView(R.id.imageView)
    ImageView mActivityImage;
    @BindView(R.id.fullerLabsGeoFenceText)
    TextView mFullerLabsGeoFenceText;
    @BindView(R.id.libraryGeoFenceText)
    TextView mLibraryGeoFenceText;
    @BindView(R.id.activityText)
    TextView mActivityText;
    @BindView(R.id.mapView)
    MapView mMapView;
    private int previousActivityCode = DetectedActivity.UNKNOWN;
    private long previousActivityStart = System.currentTimeMillis();
    private PendingIntent mPendingIntent;
    private GoogleApiClient mApiClient;
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int currentActivityCode = intent.getIntExtra("currentActivityCode", DetectedActivity.UNKNOWN);

            Log.d(TAG, "Received: " + currentActivityCode + ", Current: " + previousActivityCode);

            if (currentActivityCode == previousActivityCode)
                return;

            switch (currentActivityCode) {
                case DetectedActivity.STILL:
                    mActivityImage.setImageResource(R.drawable.still);
                    mActivityText.setText(R.string.activity_still);
                    mActivityText.setVisibility(View.VISIBLE);
                    mActivityImage.setVisibility(View.VISIBLE);
                    break;
                case DetectedActivity.WALKING:
                    mActivityImage.setImageResource(R.drawable.walking);
                    mActivityText.setText(R.string.activity_walking);
                    mActivityText.setVisibility(View.VISIBLE);
                    mActivityImage.setVisibility(View.VISIBLE);
                    break;
                case DetectedActivity.RUNNING:
                    mActivityImage.setImageResource(R.drawable.running);
                    mActivityText.setText(R.string.activity_running);
                    mActivityText.setVisibility(View.VISIBLE);
                    mActivityImage.setVisibility(View.VISIBLE);
                    break;
                case DetectedActivity.UNKNOWN:
                    mActivityText.setText("");
                    mActivityText.setVisibility(View.GONE);
                    mActivityImage.setVisibility(View.GONE);
                    break;
            }
            // Get data for the intent
            int toastTextId = -1;

            switch (previousActivityCode) {
                case DetectedActivity.STILL:
                    toastTextId = R.string.activity_summary_still;
                    break;
                case DetectedActivity.WALKING:
                    toastTextId = R.string.activity_summary_walking;
                    break;
                case DetectedActivity.RUNNING:
                    toastTextId = R.string.activity_summary_running;
                    break;
            }

            // Whenever a user switches to a new activity,
            // a toast pops up displaying how long the last activity lasted.
            // For instance, if the user was walking and became still,
            // a toast may pop up announcing "You have just walked for 1 min, 36 seconds".
            if (toastTextId != -1) {
                long elapsed = (System.currentTimeMillis() - previousActivityStart) / 1000;
                String text = getString(toastTextId, elapsed / 60, elapsed % 60);
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                Log.d(TAG, text);
            }

            previousActivityCode = currentActivityCode;
            previousActivityStart = System.currentTimeMillis();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                LatLng sydney = new LatLng(-33.867, 151.206);

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        map.setMyLocationEnabled(true);
                // Add a marker in Sydney and move the camera
                googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                mMapView.onResume();
            }
        });

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("ActivityRecognizedService#ActivityChange"));
        Log.d(TAG, "LocalBroadcastManager.registerReceiver:mMessageReceiver");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        mPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 0, mPendingIntent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mPendingIntent != null) {
            Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates");
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 0, mPendingIntent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPendingIntent != null) {
            Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates");
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, mPendingIntent);
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.d(TAG, "LocalBroadcastManager.unregisterReceiver:mMessageReceiver");
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
}
