package com.frankgh.wpiparking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.frankgh.wpiparking.auth.ChooserActivity;
import com.frankgh.wpiparking.models.ParkingLot;
import com.frankgh.wpiparking.services.GeofenceErrorMessages;
import com.frankgh.wpiparking.services.GeofenceTransitionsIntentService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.makeramen.roundedimageview.RoundedTransformationBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        ParkingLotCallbacks,
        NavigationView.OnNavigationItemSelectedListener,
        OnCompleteListener<Void> {

    private static final String TAG = "Main";

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 118;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;
    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;
    /**
     * The Google Maps map instance
     */
    private GoogleMap mMap;
    /**
     * Provides the authenticated user for Firebase.
     */
    private FirebaseAuth mAuth;
    /**
     * The marker of the current location on the map.
     */
    private Marker mLocationMarker;
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;
    /**
     * Provides the entry point to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;
    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;
    /**
     * The Adapter for the Parking Lots in the Firebase database.
     */
    private ParkingLotAdapter mAdapter;
    /**
     * The Firebase database reference for parking lots.
     */
    private DatabaseReference mParkingLotsReference;
    /**
     * The View for the list of parking lots.
     */
    private RecyclerView mParkingLotsRecycler;
    /**
     * Keeps a HashMap of markers for the parking lot.
     */
    private Map<String, Marker> mMarkerMap;
    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;
    /**
     * The list of geofences used in this sample.
     */
    private List<Geofence> mGeofenceList;
    private EditText mDebugEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            showLoginChooser();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                updateNavigationUI();
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // [START initialize_map]
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // [END initialize_map]

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();
        populateGeofenceList();

        mMarkerMap = new HashMap<>();
        mParkingLotsRecycler = findViewById(R.id.recycler_lots);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);

        mParkingLotsReference = firebaseDatabase.getReference().child("lots");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        mParkingLotsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mGeofencingClient = LocationServices.getGeofencingClient(this);

        mDebugEditText = findViewById(R.id.debugEditText);

        findViewById(R.id.directionsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDebugEditText.getVisibility() == View.VISIBLE) {
                    mDebugEditText.setVisibility(View.GONE);
                } else {
                    mDebugEditText.setVisibility(View.VISIBLE);

                    Map<String, ?> map = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .getAll();

                    String text = "";
                    for (String key : map.keySet()) {
                        if (key.indexOf(Constants.GEOFENCES_ADDED_KEY) == 0) {
                            text += key + ": " + map.get(key).toString() + "\n";
                        }
                    }
                    mDebugEditText.setText(text);

                }
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Check if user is signed in (non-null) and update UI accordingly.
        if (mAuth.getCurrentUser() == null) {
            showLoginChooser();
        } else {
            if (!ApplicationUtils.checkPermissions(this)) {
                requestPermissions();
            } else {
                getLastLocation();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ApplicationUtils.checkPermissions(this)) {
            configureDataAdapter();
            startLocationUpdates();
            performPendingGeofenceTask();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
        // Clean up comments listener
        if (mAdapter != null) mAdapter.cleanupListener();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_camera:
                // Handle the camera action
                break;
            case R.id.nav_gallery:

                break;
            case R.id.nav_slideshow:

                break;
            case R.id.nav_manage:

                break;
            case R.id.nav_disconnect:
                revokeAccess();
                break;
            case R.id.nav_logout:
                logout();
                break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        mMap = googleMap;
        zoomToLocation(Constants.WPI_AREA_LANDMARKS.get(Constants.LATLNG_WPI), 14f);
        if (BuildConfig.DEBUG) {
            addMarkerMap(Constants.LATLNG_WPI, getString(R.string.app_name),
                    Constants.GEOFENCE_RADIUS_IN_METERS,
                    Constants.WPI_AREA_LANDMARKS.get(Constants.LATLNG_WPI));
        }
    }

    /**
     * Runs when the result of calling {@link #addGeofences()} is available.
     *
     * @param task the resulting Task, containing either a result or error.
     */
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            Log.d(TAG, getString(R.string.geofences_added));
            updateGeofencesAdded(System.currentTimeMillis());
//            Toast.makeText(this, R.string.geofences_added, Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
        }
    }

    /**
     * Revoke access to Google provider.
     */
    private void revokeAccess() {
        if (mAdapter != null) mAdapter.cleanupListener();
        FirebaseUser user = mAuth.getCurrentUser();
        List<? extends UserInfo> data = user.getProviderData();
        for (UserInfo info : data) {
            Log.d(TAG, "Logging out Provider: " + info.getProviderId() + " ....");
            if (FirebaseAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                // Sign out from Firebase
                mAuth.signOut();
            } else if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder().build();
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
                // Google revoke access
                mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                showLoginChooser();
                            }
                        });
            }
        }
    }

    /**
     * Log out from all providers.
     */
    private void logout() {
        if (mAdapter != null) mAdapter.cleanupListener();
        FirebaseUser user = mAuth.getCurrentUser();
        List<? extends UserInfo> data = user.getProviderData();
        boolean noWait = true;
        for (UserInfo info : data) {
            Log.d(TAG, "Logging out Provider: " + info.getProviderId() + " ....");
            if (FirebaseAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                // Sign out from Firebase
                mAuth.signOut();
            } else if (FacebookAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                LoginManager.getInstance().logOut();
            } else if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                noWait = false;
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder().build();
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
                mGoogleSignInClient.signOut().addOnCompleteListener(this,
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                showLoginChooser();
                            }
                        });
            }
        }
        if (noWait) showLoginChooser();
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Initialize the adapter for the firebase database.
     */
    private void configureDataAdapter() {
        if (mAdapter == null) {
            // Listen for Firebase DB parking lot events
            mAdapter = new ParkingLotAdapter(this, mParkingLotsReference).
                    registerCallback(this);
            mParkingLotsRecycler.setAdapter(mAdapter);
        } else {
            mAdapter.startListener();
        }
    }

    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     * <p>
     * Note: this method should be called after location permission has been granted.
     */
    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        Log.d(TAG, "getLastLocation invoked");
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mCurrentLocation = task.getResult();
                            zoomToLocation(mCurrentLocation, 14f);
                        } else {
                            Log.w(TAG, "getLastLocation:exception", task.getException());
                            showSnackbar(getString(R.string.no_location_detected));
                        }
                    }
                });
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
//                Log.d(TAG, "onLocationResult invoked");
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                updateLocationUI();
            }
        };
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null && mMap != null) {
            if (mLocationMarker != null)
                mLocationMarker.remove();

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                    .title(mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude());
            mLocationMarker = mMap.addMarker(markerOptions);
        }
    }

    /**
     * Sets the value of the UI fields for the user account on the Navigation Drawer.
     */
    private void updateNavigationUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            TextView displayNameTextView = findViewById(R.id.display_name_textview);
            if (displayNameTextView.getTag() != null)
                return;

            TextView emailTextView = findViewById(R.id.email_textview);
            List<? extends UserInfo> data = user.getProviderData();
            String displayName = null, email = null;
            Uri photoUrl = null;
            boolean hasGoogleProvider = false;
            for (UserInfo info : data) {
                displayName = TextUtils.isEmpty(displayName) ? info.getDisplayName() : displayName;
                email = TextUtils.isEmpty(email) ? info.getEmail() : email;
                photoUrl = photoUrl == null ? info.getPhotoUrl() : photoUrl;
                hasGoogleProvider = hasGoogleProvider || GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId());
            }

            displayNameTextView.setText(displayName);
            displayNameTextView.setTag(true);
            if (TextUtils.isEmpty(email)) {
                emailTextView.setVisibility(View.GONE);
            } else {
                emailTextView.setText(email);
            }
            if (photoUrl != null) {
                Transformation transformation = new RoundedTransformationBuilder()
                        .borderColor(Color.BLACK)
                        .borderWidthDp(0)
                        .cornerRadiusDp(25)
                        .oval(true)
                        .build();

                ImageView imageView = findViewById(R.id.profile_imageview);
                Picasso.with(this)
                        .load(photoUrl)
                        .fit()
                        .centerInside()
                        .transform(transformation)

                        .into(imageView);
            }
            if (!hasGoogleProvider) {
                // Hide disconnect menu item
                NavigationView nv = findViewById(R.id.nav_view);
                nv.getMenu().findItem(R.id.nav_disconnect).setVisible(false);
            }
        }
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates invoked");
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

//                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
//                                mRequestingLocationUpdates = false;
                        }

//                        updateUI();
                    }
                });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates invoked");
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    public void onParkingLotAdded(ParkingLot lot) {
        LatLng latLng = new LatLng(lot.getLatitude(), lot.getLongitude());
        addMarkerMap(lot.getName(), lot.getDisplayName(), lot.getRadius(), latLng);
    }

    private void addMarkerMap(String key, String title, int radius, LatLng latLng) {
        if (mMap == null) return;
        synchronized (mMarkerMap) {
            if (mMarkerMap.containsKey(key)) {
                Marker m = mMarkerMap.remove(key);
                Circle c = (Circle) m.getTag();

                c.remove();
                m.remove();
            }

            // Define marker options
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .title(title);
            Marker marker = mMap.addMarker(markerOptions);
            mMarkerMap.put(key, marker);

            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .strokeColor(ContextCompat.getColor(this, R.color.geofence_stroke_color)) //Color.argb(50, 70, 70, 70))
                    .fillColor(ContextCompat.getColor(this, R.color.geofence_fill_color)) //Color.argb(100, 150, 150, 150))
                    .radius(radius);
            Circle circle = mMap.addCircle(circleOptions);
            marker.setTag(circle);
        }
    }

    private void showLoginChooser() {
        Log.d(TAG, "currentUser is null. Launching ChooserActivity");
        startActivity(new Intent(this, ChooserActivity.class));
        Log.d(TAG, "Finish MainActivity");
        finish();
    }

    private void zoomToLocation(Location location, float zoom) {
        zoomToLocation(new LatLng(location.getLatitude(), location.getLongitude()), zoom);
    }

    private void zoomToLocation(LatLng latLng, float zoom) {
        if (mMap == null)
            return;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        mMap.animateCamera(cameraUpdate);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    private void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.WPI_AREA_LANDMARKS.entrySet()) {
            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)

                    // Sets the delay between GEOFENCE_TRANSITION_ENTER AND GEOFENCE_TRANSITION_DWELLING
                    // in milliseconds.
                    .setLoiteringDelay(Constants.GEOFENCE_LOITERING_DELAY)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_DWELL |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
        }
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     */
    private void performPendingGeofenceTask() {
        if (shouldAddGeofences()) {
            addGeofences();
        }
    }

    /**
     * Check whether geofence needs to be added
     */
    private boolean shouldAddGeofences() {
        long added = PreferenceManager.getDefaultSharedPreferences(this).getLong(
                Constants.GEOFENCES_ADDED_KEY, -1);
        return System.currentTimeMillis() - added > Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS;
    }

    /**
     * Stores whether geofences were added ore removed in {@link SharedPreferences};
     *
     * @param added Whether geofences were added or removed.
     */
    private void updateGeofencesAdded(long added) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putLong(Constants.GEOFENCES_ADDED_KEY, added)
                .apply();
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        if (!ApplicationUtils.checkPermissions(this)) {
            showSnackbar(getString(R.string.insufficient_permissions));
            return;
        }

        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER |
                GeofencingRequest.INITIAL_TRIGGER_DWELL);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        Log.d(TAG, "getGeofencePendingIntent invoked");
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.");
                configureDataAdapter();
                getLastLocation();
                startLocationUpdates();
                performPendingGeofenceTask();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    /**
     * Cache of the children views for a parking lot list item.
     */
    private static class ParkingLotViewHolder extends RecyclerView.ViewHolder {
        //        @BindVie/w(R.id.lot_name_textview)
        private TextView lotNameTextView;
        //        @BindView(R.id.lot_availability_textview)
        private TextView lotAvailabilityTextView;

        ParkingLotViewHolder(View view) {
            super(view);
            lotNameTextView = itemView.findViewById(R.id.lot_name_textview);
            lotAvailabilityTextView = itemView.findViewById(R.id.lot_availability_textview);
        }
    }

    private static class ParkingLotAdapter extends RecyclerView.Adapter<ParkingLotViewHolder> {
        private final Map<String, Integer> mParkingLotIds;
        private final List<ParkingLot> mParkingLots;
        private final List<ParkingLotCallbacks> mCallbacks;
        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;

        public ParkingLotAdapter(final Context context, final DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;
            mParkingLotIds = new HashMap<>();
            mParkingLots = new ArrayList<>();
            mCallbacks = new ArrayList<>();

            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new lot has been added, add it to the displayed list
                    ParkingLot lot = ApplicationUtils.getLotFromDataSnapshot(dataSnapshot);

                    if (!mParkingLotIds.containsKey(lot.getName())) {
                        Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());
                        // [START_EXCLUDE]
                        // Update RecyclerView
                        mParkingLots.add(lot);
                        mParkingLotIds.put(lot.getName(), mParkingLots.size() - 1);
                        notifyItemInserted(mParkingLots.size() - 1);
                        // [END_EXCLUDE]

                        for (ParkingLotCallbacks callback : mCallbacks) {
                            callback.onParkingLotAdded(lot);
                        }
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                    // A lot has changed, use the key to determine if we are displaying this
                    // lot and if so displayed the changed lot.
                    ParkingLot updatedLot = ApplicationUtils.getLotFromDataSnapshot(dataSnapshot);
                    Integer index = mParkingLotIds.get(updatedLot.getName());
                    if (index != null) {
                        mParkingLots.set(index, updatedLot);
                        notifyItemChanged(index);

                        for (ParkingLotCallbacks callback : mCallbacks) {
                            callback.onParkingLotAdded(updatedLot);
                        }
                    } else {
                        Log.w(TAG, "onChildChanged:unknown_child:" + updatedLot.getName());
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    String name = dataSnapshot.getKey();
                    Integer index = mParkingLotIds.get(name);
                    if (index != null) {
                        // Remove data from the list
                        mParkingLots.remove(index);
                        mParkingLotIds.remove(index);

                        // Update the RecyclerView
                        notifyItemRemoved(index);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + name);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                    Toast.makeText(mContext, "Failed to load parking lots.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;

            startListener();
        }

        @Override
        public ParkingLotViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_lot, parent, false);
            return new ParkingLotViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ParkingLotViewHolder holder, int position) {
            ParkingLot lot = mParkingLots.get(position);
            holder.lotNameTextView.setText(lot.getDisplayName());

            if (lot.isFull()) {
                holder.lotAvailabilityTextView.setText(R.string.parking_lot_full);
            } else if (lot.getAvailable() == 1) {
                holder.lotAvailabilityTextView.setText(R.string.one_spot);
            } else {
                holder.lotAvailabilityTextView.setText(mContext.getString(R.string.x_spots, lot.getAvailable()));
            }
        }

        @Override
        public int getItemCount() {
            return mParkingLots.size();
        }

        private void startListener() {
            Log.d(TAG, "startListener invoked");
            if (mChildEventListener != null) {
                mDatabaseReference.addChildEventListener(mChildEventListener);
            }
        }

        private void cleanupListener() {
            Log.d(TAG, "cleanupListener invoked");
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

        private List<ParkingLot> getParkingLots() {
            return mParkingLots;
        }

        private ParkingLotAdapter registerCallback(@NonNull ParkingLotCallbacks callback) {
            mCallbacks.add(callback);
            return this;
        }
    }
}
