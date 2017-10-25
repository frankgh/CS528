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
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

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
    private static final long GEO_FENCE_DURATION = 60 * 60 * 1000;
    private static final int GEO_FENCE_REQ_CODE = 0;
    private static final int REQ_PERMISSION = 999;
    private static final int UPDATE_INTERVAL = 1000;
    private static final int FASTEST_INTERVAL = 900;

    private static final LatLng FULLER_COORDS = new LatLng(42.275177, -71.805926);
    private static final LatLng LIBRARY_COORDS = new LatLng(42.274228, -71.806353);

    private static final String FULLER_LABEL = "FULLER";
    private static final String LIBRARY_LABEL = "LIBRARY";

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
    private PendingIntent mActivityPendingIntent;
    private PendingIntent mGeoFencePendingIntent;
    private GoogleApiClient mApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Marker mLocationMarker;
    private List<Marker> mGeoFenceMarkers = new ArrayList<>();
    private List<Circle> mGeoFenceLimits = new ArrayList<>();
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received broadcast message " + intent.getAction());
            if (intent.getAction().equals(ActivityRecognizedService.BROADCAST_ACTION)) {
                handleActivityRecognizedBroadcastAction(context, intent);
            } else if (intent.getAction().equals(GeoFenceTransitionService.BROADCAST_ACTION)) {
                handleGeoFenceTransitionBroadcastAction(context, intent);
            }
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

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver, new IntentFilter(ActivityRecognizedService.BROADCAST_ACTION));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver, new IntentFilter(GeoFenceTransitionService.BROADCAST_ACTION));
        Log.d(TAG, "LocalBroadcastManager.registerReceiver:mMessageReceiver");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startActivityRecognitionService();
        getLastKnownLocation();
        startLocationUpdates();
        createGeoFences();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mApiClient.connect();
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
        stopGeoFenceTransitionService();
        // Disconnect GoogleApiClient when stopping Activity
        mApiClient.disconnect();
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
//            drawGeoFence();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged [" + location + "]");
        mLastLocation = location;
        updateCurrentLocation(location);
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


        if (!checkFineLocationAccessPermission()) {
            requestFineLocationAccessPermission();
            return;
        }

        mMap.setMyLocationEnabled(true);
        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(FULLER_COORDS).title("Marker in Fuller"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(FULLER_COORDS));
        mMapView.onResume();
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastKnownLocation(); // Permission granted
                } else {
                    Log.w(TAG, "permissionsDenied");
                    finishAndRemoveTask(); // Permission denied
                }
                break;
            }
        }
    }

    private void handleActivityRecognizedBroadcastAction(Context context, Intent intent) {
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

    private void handleGeoFenceTransitionBroadcastAction(Context context, Intent intent) {

    }

    private void requestFineLocationAccessPermission() {
        Log.d(TAG, "requestFineLocationAccessPermission");
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }

    // Check for permission to access Location
    private boolean checkFineLocationAccessPermission() {
        Log.d(TAG, "checkFineLocationAccessPermission()");
        // Ask for permission if it wasn't granted yet
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if (checkFineLocationAccessPermission()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
            if (mLastLocation != null) {
                Log.d(TAG, "LasKnown location.  Long: " + mLastLocation.getLongitude() + ", Lat: " + mLastLocation.getLatitude());
                updateCurrentLocation(mLastLocation);
            }
            startLocationUpdates();
        } else requestFineLocationAccessPermission();
    }

    // Start location Updates
    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (checkFineLocationAccessPermission())
            LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient, mLocationRequest, this);
    }

    private void updateCurrentLocation(Location location) {
        Log.i(TAG, "updateCurrentLocation(" + location.getLatitude() + ", " + location.getLongitude() + ")");
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if (mMap != null) {
            if (mLocationMarker != null)
                mLocationMarker.remove();
            mLocationMarker = mMap.addMarker(markerOptions);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
        }
    }

    private void createGeoFences() {
        Log.d(TAG, "createGeoFences");

        clearGeoFences();
        createGeoFence(FULLER_COORDS, FULLER_LABEL);
        createGeoFence(LIBRARY_COORDS, LIBRARY_LABEL);
    }

    private void clearGeoFences() {
        for (int i = mGeoFenceMarkers.size() - 1; i >= 0; i--) mGeoFenceMarkers.remove(i).remove();
        for (int i = mGeoFenceLimits.size() - 1; i >= 0; i--) mGeoFenceLimits.remove(i).remove();
    }

    private void createGeoFence(final LatLng latLng, final String geoFenceRequestId) {
        Log.d(TAG, "createGeoFence");
        if (mMap == null) return;
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        Marker marker = mMap.addMarker(markerOptions);
        mGeoFenceMarkers.add(marker);
        // Create geoFence
        Geofence geoFence = new Geofence.Builder()
                .setRequestId(geoFenceRequestId)
                .setCircularRegion(latLng.latitude, latLng.longitude, GEO_FENCE_RADIUS)
                .setExpirationDuration(GEO_FENCE_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        GeofencingRequest geoFenceRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geoFence)
                .build();

        if (checkFineLocationAccessPermission()) {
            startGeoFenceTransitionService();
            LocationServices.GeofencingApi.addGeofences(mApiClient, geoFenceRequest, mGeoFencePendingIntent).setResultCallback(this);
        }

        CircleOptions circleOptions1 = new CircleOptions()
                .center(marker.getPosition())
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(GEO_FENCE_RADIUS);
        mGeoFenceLimits.add(mMap.addCircle(circleOptions1));
    }

    private void startActivityRecognitionService() {
        Log.d(TAG, "startActivityRecognitionService");
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        mActivityPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, ACTIVITY_RECOGNITION_DETECTION_INTERVAL, mActivityPendingIntent);
    }

    private void resumeActivityRecognitionService() {
        if (mActivityPendingIntent != null) {
            Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates");
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, ACTIVITY_RECOGNITION_DETECTION_INTERVAL, mActivityPendingIntent);
        }
    }

    private void stopActivityRecognitionService() {
        if (mActivityPendingIntent != null) {
            Log.d(TAG, "ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates");
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, mActivityPendingIntent);
        }
    }

    private void startGeoFenceTransitionService() {
        Log.d(TAG, "createGeoFencePendingIntent");
        if (mGeoFencePendingIntent == null) {
            Intent intent = new Intent(this, GeoFenceTransitionService.class);
            mGeoFencePendingIntent = PendingIntent.getService(this, GEO_FENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // startService(intent);
        }
    }

    private void stopGeoFenceTransitionService() {
        if (mGeoFencePendingIntent != null) {
            Log.d(TAG, "LocationServices.GeofencingApi.removeGeofences");
            LocationServices.GeofencingApi.removeGeofences(mApiClient, mGeoFencePendingIntent);
            mGeoFencePendingIntent = null;
        }
    }
}
