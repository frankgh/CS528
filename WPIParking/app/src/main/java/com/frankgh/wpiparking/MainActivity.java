package com.frankgh.wpiparking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.frankgh.wpiparking.auth.ChooserActivity;
import com.frankgh.wpiparking.models.ParkingLot;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, ParkingLotCallbacks {

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

        // [START initialize_map]
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // [END initialize_map]

        mMarkerMap = new HashMap<>();
        mParkingLotsRecycler = findViewById(R.id.recycler_lots);
        mParkingLotsReference = FirebaseDatabase.getInstance().getReference().child("lots");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        mParkingLotsRecycler.setLayoutManager(new LinearLayoutManager(this));
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
//                startParkingService();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ApplicationUtils.checkPermissions(this)) {
            startLocationUpdates();

            if (mAdapter == null) {
                // Listen for Firebase DB parking lot events
                mAdapter = new ParkingLotAdapter(this, mParkingLotsReference).
                        registerCallback(this);
                mParkingLotsRecycler.setAdapter(mAdapter);
            } else {
                mAdapter.startListener();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Clean up comments listener
        if (mAdapter != null) mAdapter.cleanupListener();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        mMap = googleMap;
        zoomToLocation(Constants.WPI_AREA_LANDMARKS.get(Constants.LATLNG_WPI), 14f);
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
        if (mMap == null)
            return;

        LatLng latLng = new LatLng(lot.getLatitude(), lot.getLongitude());
        synchronized (mMarkerMap) {
            if (mMarkerMap.containsKey(lot.getName())) {
                Marker m = mMarkerMap.remove(lot.getName());
                Circle c = (Circle) m.getTag();

                c.remove();
                m.remove();
            }

            // Define marker options
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .title(lot.getDisplayName());
            Marker marker = mMap.addMarker(markerOptions);
            mMarkerMap.put(lot.getName(), marker);

            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .strokeColor(ContextCompat.getColor(this, R.color.geofence_stroke_color)) //Color.argb(50, 70, 70, 70))
                    .fillColor(ContextCompat.getColor(this, R.color.geofence_fill_color)) //Color.argb(100, 150, 150, 150))
                    .radius(lot.getRadius());
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
//                startParkingService();
                getLastLocation();
                startLocationUpdates();
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

        public void cleanupListener() {
            Log.d(TAG, "cleanupListener invoked");
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

        public ParkingLotAdapter registerCallback(@NonNull ParkingLotCallbacks callback) {
            mCallbacks.add(callback);
            return this;
        }
    }
}
