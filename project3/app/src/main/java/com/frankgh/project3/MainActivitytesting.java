package com.frankgh.project3;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivitytesting extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        ResultCallback<Status>, SensorEventListener



{

    @BindView(R.id.imageView)
    ImageView mActivityImage;
    @BindView(R.id.fullerLabsGeoFenceText)
    TextView mFullerLabsGeoFenceText;
    @BindView(R.id.libraryGeoFenceText)
    TextView mLibraryGeoFenceText;
    @BindView(R.id.activityText)
    TextView mActivityText;


    private int previousActivityCode = DetectedActivity.UNKNOWN;
    private long previousActivityStart = System.currentTimeMillis();
    private PendingIntent mPendingIntent;
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


    private static final String TAG = MainActivitytesting.class.getSimpleName();

    private GoogleMap map;
    private GoogleApiClient mApiClient;
    private Location lastLocation;

    private TextView textLat, textLong;
    public static TextView textLib, textLab;

    private MapFragment mapFragment;

    private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";
    // Create a Intent send by the notification
    int steps = 0;
    boolean entered = false;
    private SensorManager sensorManager;
    private Sensor accel;

    public static Intent makeNotificationIntent(Context context, String msg) {

        Intent intent = new Intent( context, MainActivitytesting.class );
        intent.putExtra( NOTIFICATION_MSG, msg );
        return intent;
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

//        mActivityText.setVisibility(View.GONE);
//        mActivityImage.setVisibility(View.GONE);

//        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
//
//
//        initGMaps();
//
//        // create GoogleApiClient
//        createGoogleApi();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("ActivityRecognizedService#ActivityChange"));
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "entered onReceive");
            updateUI(intent);
        }
    };


    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if ( mApiClient == null ) {
            mApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    private void updateUI(Intent intent) {
        Log.d(TAG, "entered updateUI");

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Call GoogleApiClient connection when starting the Activity
        mApiClient.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.main_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case R.id.geofence: {
                startGeofence();
                return true;
            }
            case R.id.clear: {
                clearGeofence();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private final int REQ_PERMISSION = 999;

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    // Asks for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                REQ_PERMISSION
        );
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case REQ_PERMISSION: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
        // TODO close app and warn user
    }

    // Initialize GoogleMaps
    private void initGMaps(){
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Callback called when Map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        map = googleMap;
        //map.setOnMapClickListener(this);


        //map.setOnMarkerClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick("+latLng +")");
        markerForGeofence(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition() );
        return false;
    }

    private LocationRequest locationRequest;
    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL =  1000;
    private final int FASTEST_INTERVAL = 900;

    // Start location Updates
    private void startLocationUpdates(){
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if ( checkPermission() )
            LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged ["+location+"]");
        lastLocation = location;
        writeActualLocation(location);
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if ( checkPermission() ) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
            if ( lastLocation != null ) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());
                writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        }
        else askPermission();
    }

    private void writeActualLocation(Location location) {
        textLat.setText( "Lat: " + location.getLatitude() );
        textLong.setText( "Long: " + location.getLongitude() );

        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }



    private void writeLastLocation() {
        writeActualLocation(lastLocation);
    }

    private Marker locationMarker;
    private void markerLocation(LatLng latLng) {
        Log.i(TAG, "markerLocation("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if ( map!=null ) {
            if ( locationMarker != null )
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);
        }
    }


    private Marker geoFenceMarker1;
    private Marker geoFenceMarker2;

    private void markerForGeofence(LatLng latLng) {
        Log.i(TAG, "markerForGeofence("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if ( map!=null ) {
            // Remove last geoFenceMarker
            if (geoFenceMarker1 != null)
                geoFenceMarker1.remove();

            geoFenceMarker1 = map.addMarker(markerOptions);

            if (geoFenceMarker2 != null)
                geoFenceMarker2.remove();

            geoFenceMarker2 = map.addMarker(markerOptions);

        }
    }

    private void markersForGeofence(LatLng latLng1, LatLng latLng2) {
        Log.i(TAG, "markerForGeofence("+latLng1+latLng2+")");
        String title1 = latLng1.latitude + ", " + latLng1.longitude;
        // Define marker options
        MarkerOptions markerOptions1 = new MarkerOptions()
                .position(latLng1)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title1);
        String title2 = latLng2.latitude + ", " + latLng2.longitude;
        // Define marker options
        MarkerOptions markerOptions2 = new MarkerOptions()
                .position(latLng2)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title2);
        if ( map!=null ) {
            // Remove last geoFenceMarker
            if (geoFenceMarker1 != null)
                geoFenceMarker1.remove();

            geoFenceMarker1 = map.addMarker(markerOptions1);  //Fuller Labs

            if (geoFenceMarker2 != null)
                geoFenceMarker2.remove();

            geoFenceMarker2 = map.addMarker(markerOptions2); //Gordon Lib
        }

    }


    // Start Geofence creation process
    private void startGeofence() {
        Log.i(TAG, "startGeofence()");

        //Geofence geofence2 = createGeofence( fullerLabs, GEOFENCE_RADIUS );
        //GeofencingRequest geofenceRequest2 = createGeofenceRequest( geofence2 );
        //addGeofence( geofenceRequest2 );
        if( geoFenceMarker1 != null ) {
            String geofence_req_id_1 = "Fuller Labs";
            String geofence_req_id_2 = "Gordon Lib";
            Geofence geofence1 = createGeofence( geoFenceMarker1.getPosition(), GEOFENCE_RADIUS, geofence_req_id_1 );
            GeofencingRequest geofenceRequest1 = createGeofenceRequest( geofence1 );
            addGeofence( geofenceRequest1 );

            Geofence geofence2 = createGeofence( geoFenceMarker2.getPosition(), GEOFENCE_RADIUS, geofence_req_id_2 );
            GeofencingRequest geofenceRequest2 = createGeofenceRequest( geofence2 );
            addGeofence( geofenceRequest2 );
            //fuller labs lat: 42.274978 , long: -71.806632
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }

    private static final long GEO_DURATION = 60 * 60 * 1000;

    private static final float GEOFENCE_RADIUS = 500.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius, String geofence_req_id ) {
        Log.d(TAG, "createGeofence" + latLng);
        return new Geofence.Builder()
                .setRequestId(geofence_req_id)
                .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration( GEO_DURATION )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest( Geofence geofence ) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;
    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, GeofenceTrasitionService.class);
        startService(intent);
        registerReceiver(broadcastReceiver, new IntentFilter(GeofenceTrasitionService.BROADCAST_ACTION));
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    mApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if ( status.isSuccess() ) {
            //saveGeofence();
            drawGeofence();
        } else {
            // inform about fail
        }
    }

    // Draw Geofence circle on GoogleMap
    private Circle geoFenceLimits1;
    private Circle geoFenceLimits2;
    private void drawGeofence() {
        Log.d(TAG, "drawGeofence()");

        if ( geoFenceLimits1 != null )
            geoFenceLimits1.remove();

        if ( geoFenceLimits2 != null )
            geoFenceLimits2.remove();

        CircleOptions circleOptions1 = new CircleOptions()
                .center( geoFenceMarker1.getPosition())
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(100, 150,150,150) )
                .radius( GEOFENCE_RADIUS );
        geoFenceLimits1 = map.addCircle( circleOptions1 );

        CircleOptions circleOptions2 = new CircleOptions()
                .center( geoFenceMarker2.getPosition())
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(100, 150,150,150) )
                .radius( GEOFENCE_RADIUS );
        geoFenceLimits2 = map.addCircle( circleOptions2 );
    }

    private final String KEY_GEOFENCE_LAT = "GEOFENCE LATITUDE";
    private final String KEY_GEOFENCE_LON = "GEOFENCE LONGITUDE";

    // Saving GeoFence marker with prefs mng
    private void saveGeofence() {
        Log.d(TAG, "saveGeofence()");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putLong( KEY_GEOFENCE_LAT, Double.doubleToRawLongBits( geoFenceMarker1.getPosition().latitude ));
        editor.putLong( KEY_GEOFENCE_LON, Double.doubleToRawLongBits( geoFenceMarker1.getPosition().longitude ));
        editor.putLong( KEY_GEOFENCE_LAT, Double.doubleToRawLongBits( geoFenceMarker2.getPosition().latitude ));
        editor.putLong( KEY_GEOFENCE_LON, Double.doubleToRawLongBits( geoFenceMarker2.getPosition().longitude ));
        editor.apply();
    }

    // Recovering last Geofence marker
    private void recoverGeofenceMarker() {
        Log.d(TAG, "recoverGeofenceMarker");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );

        if ( sharedPref.contains( KEY_GEOFENCE_LAT ) && sharedPref.contains( KEY_GEOFENCE_LON )) {
            double lat = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LAT, -1 ));
            double lon = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LON, -1 ));
            LatLng latLng = new LatLng( lat, lon );
            markerForGeofence(latLng);
            drawGeofence();
        }
    }

    // Clear Geofence
    private void clearGeofence() {
        Log.d(TAG, "clearGeofence()");
        LocationServices.GeofencingApi.removeGeofences(
                mApiClient,
                createGeofencePendingIntent()
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if ( status.isSuccess() ) {
                    // remove drawing
                    removeGeofenceDraw();
                }
            }
        });
    }

    private void removeGeofenceDraw() {
        Log.d(TAG, "removeGeofenceDraw()");
        if ( geoFenceMarker1 != null)
            geoFenceMarker1.remove();
        if ( geoFenceMarker2 != null)
            geoFenceMarker2.remove();
        if ( geoFenceLimits1 != null )
            geoFenceLimits1.remove();
        if ( geoFenceLimits2 != null )
            geoFenceLimits2.remove();
    }








    public static float sum(float[] array) {
        float summed = 0;
        for (int i = 0; i < array.length; i++) {
            summed += array[i];
        }
        return summed;
    }
    public static float norm(float[] array) {
        float normed = 0;
        for (int i = 0; i < array.length; i++) {
            normed += array[i] * array[i];
        }
        return (float) Math.sqrt(normed);
    }


    public static float dot(float[] a, float[] b) {
        float dotted = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return dotted;
    }

    private static final int accelSamples = 50;
    private static final int VEL_RING_SIZE = 5;
    private static final float stepSensitivity = 20f;

    private static final int stepTime = 200000000;

    private int accelCounter = 0;
    private float[] accelX = new float[accelSamples];
    private float[] accelY = new float[accelSamples];
    private float[] accelZ = new float[accelSamples];
    private int velRingCounter = 0;
    private float[] velRing = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

    public void updateAccel(long timeNs, float x, float y, float z) {
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        accelCounter++;
        accelX[accelCounter % accelSamples] = currentAccel[0];
        accelY[accelCounter % accelSamples] = currentAccel[1];
        accelZ[accelCounter % accelSamples] = currentAccel[2];

        float[] worldZ = new float[3];
        worldZ[0] = sum(accelX) / Math.min(accelCounter, accelSamples);
        worldZ[1] = sum(accelY) / Math.min(accelCounter, accelSamples);
        worldZ[2] = sum(accelZ) / Math.min(accelCounter, accelSamples);

        float normalization_factor = norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        float currentZ = dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

        float velocityEstimate = sum(velRing);

        String lab_counter = "lab_counter";
        String lib_counter = "lib_counter";
        Log.d(TAG, lab_counter);
        Log.d(TAG, lib_counter);


        if (velocityEstimate > stepSensitivity && oldVelocityEstimate <= stepSensitivity
                && (timeNs - lastStepTimeNs > stepTime)) {
            steps += 1;
            TextView textLib = (TextView) findViewById(R.id.libraryGeoFenceText);
            TextView textLab = (TextView) findViewById(R.id.fullerLabsGeoFenceText);
            textLab.setText("Visits to Fuller labs geoFence:" + Integer.toString(steps));
            textLib.setText("Visits to Library geoFence: " + lib_counter);
            lastStepTimeNs = timeNs;
        }
        oldVelocityEstimate = velocityEstimate;
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            updateAccel(
                    sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
