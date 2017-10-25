package com.frankgh.project3;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        ResultCallback<Status> {

    private static final String TAG = "MainActivity";
    /**
     * Interval of 5 seconds
     */
    private static final int ACTIVITY_RECOGNITION_DETECTION_INTERVAL = 5000;
    private static final float GEO_FENCE_RADIUS = 500.0f; // in meters
    private static final int REQ_PERMISSION = 999;
    private static final int UPDATE_INTERVAL = 1000;
    private static final int FASTEST_INTERVAL = 900;

    private static final LatLng FULLER_COORDS = new LatLng(42.275177, -71.805926);
    private static final LatLng LIBRARY_COORDS = new LatLng(42.274228, -71.806353);

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
    private GoogleMap mMap;
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

            int imageResourceId = -1;
            int textResourceId = -1;
            switch (currentActivityCode) {
                case DetectedActivity.STILL:
                    imageResourceId = R.drawable.still;
                    textResourceId = R.string.activity_still;
                    break;
                case DetectedActivity.WALKING:
                    imageResourceId = R.drawable.walking;
                    textResourceId = R.string.activity_walking;
                    break;
                case DetectedActivity.RUNNING:
                    imageResourceId = R.drawable.running;
                    textResourceId = R.string.activity_running;
                    break;
            }
            if (imageResourceId != -1) {
                mActivityText.setText(textResourceId);
                mActivityText.setVisibility(View.VISIBLE);
                mActivityImage.setImageResource(imageResourceId);
                mActivityImage.setVisibility(View.VISIBLE);
            } else {
                mActivityText.setVisibility(View.GONE);
                mActivityImage.setVisibility(View.GONE);
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

        mActivityText.setVisibility(View.GONE);
        mActivityImage.setVisibility(View.GONE);

        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
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
        startActivityRecognitionService();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        resumeActivityRecognitionService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopActivityRecognitionService();
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

    @Override
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if (status.isSuccess()) {
            drawGeoFence();
        }
    }

    private void startActivityRecognitionService() {
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        mPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, ACTIVITY_RECOGNITION_DETECTION_INTERVAL, mPendingIntent);
    }

    private void resumeActivityRecognitionService() {
        if (mPendingIntent != null) {
            Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates");
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, ACTIVITY_RECOGNITION_DETECTION_INTERVAL, mPendingIntent);
        }
    }

    private void stopActivityRecognitionService() {
        if (mPendingIntent != null) {
            Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates");
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, mPendingIntent);
        }
    }

    private void drawGeoFence() {
        Log.d(TAG, "drawGeoFence()");

        if (geoFenceLimits1 != null)
            geoFenceLimits1.remove();

        if (geoFenceLimits2 != null)
            geoFenceLimits2.remove();

        CircleOptions circleOptions1 = new CircleOptions()
                .center(geoFenceMarker1.getPosition())
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(GEO_FENCE_RADIUS);
        geoFenceLimits1 = map.addCircle(circleOptions1);

        CircleOptions circleOptions2 = new CircleOptions()
                .center(geoFenceMarker2.getPosition())
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(GEO_FENCE_RADIUS);
        geoFenceLimits2 = mMap.addCircle(circleOptions2);
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_PERMISSION
            );

            return;
        }
        mMap.setMyLocationEnabled(true);
        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(FULLER_COORDS).title("Marker in Fuller"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(FULLER_COORDS));
        mMapView.onResume();
    }
}
