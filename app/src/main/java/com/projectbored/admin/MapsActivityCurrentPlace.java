package com.projectbored.admin;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapsActivityCurrentPlace extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
                GoogleApiClient.ConnectionCallbacks,
                GoogleApiClient.OnConnectionFailedListener {

    private static final String PREFS_NAME = "UserDetails";

    private static final String TAG = MapsActivityCurrentPlace.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(1.346313, 103.841332);
    private static final int DEFAULT_ZOOM = 18;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private SearchView hashtagSearch;

    // Used for selecting the current place.
    //private final int mMaxEntries = 5;
    //private String[] mLikelyPlaceNames = new String[mMaxEntries];
    //private String[] mLikelyPlaceAddresses = new String[mMaxEntries];
    //private String[] mLikelyPlaceAttributions = new String[mMaxEntries];
    //private LatLng[] mLikelyPlaceLatLngs = new LatLng[mMaxEntries];

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(com.projectbored.admin.R.layout.activity_maps);

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();

        hashtagSearch = (SearchView) findViewById(R.id.hashtag_search);
        hashtagSearch.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = hashtagSearch.getQuery().toString();
                if (!query.equals("")) {
                    if (query.contains("#")) {
                        query = query.substring(1);
                    }
                    searchHashtags(query);
                }
            }
        });
        hashtagSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.equals("")) {
                    if (query.contains("#")) {
                        query = query.substring(1);
                    }
                    searchHashtags(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    mDataRef = FirebaseDatabase.getInstance().getReference();
}

    private void handleIntent(Intent intent) {
        String appLinkAction = intent.getAction();
        Uri appLinkData = intent.getData();
        if(Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null) {
            final String storyKey = appLinkData.getLastPathSegment();

            mMap.clear();
            mDataRef.child("stories").child(storyKey).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String[] locationArray = dataSnapshot.child("Location").getValue(String.class).split(",");
                    LatLng storyPosition = new LatLng(Double.parseDouble(locationArray[0]),
                            Double.parseDouble(locationArray[1]));

                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(storyPosition)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    marker.setTag(storyKey);

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(storyPosition));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }



    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(com.projectbored.admin.R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }

    /**
     * Sets up the options menu.
     * @param menu The options menu.
     * @return Boolean.
     */

   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.projectbored.admin.R.menu.current_place_menu, menu);
        return true;
        }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logInOption = menu.findItem(R.id.option_log_in);
        MenuItem logOutOption = menu.findItem(R.id.option_log_out);

        if(isLoggedIn()) {
            logInOption.setVisible(false);
            logOutOption.setVisible(true);
        } else {
            logInOption.setVisible(true);
            logOutOption.setVisible(false);
        }
        return true;
    }


    /**
     * Handles a click on the menu option to get a place.
     * @param item The menu item to handle.
     * @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_add_story) {
            addStory();
        }

        if (item.getItemId() == R.id.option_log_in) {
            Intent loginIntent = new Intent(this, Login.class);
            startActivity(loginIntent);
        }

        if(item.getItemId() == R.id.option_log_out) {
            Intent logoutIntent = new Intent(this, Logout.class);
            startActivity(logoutIntent);
        }

        /*if(item.getItemId() == R.id.option_clean_database) {
            cleanDatabase();
        }*/

        return true;
    }


    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LatLng markerPosition = marker.getPosition();
                Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
                markerLocation.setLatitude(markerPosition.latitude);
                markerLocation.setLongitude(markerPosition.longitude);
                showStoryDetails(marker);
                return true;
            }
        });

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        /*
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(com.projectbored.app.R.layout.custom_info_contents,
                        (FrameLayout)findViewById(com.projectbored.app.R.id.map), false);

                TextView title = ((TextView) infoWindow.findViewById(com.projectbored.app.R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(com.projectbored.app.R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });
        */

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        //Load stories.
        getStories();

        handleIntent(getIntent());
    }

    //Close app when back button is pressed, instead of returning to splash screen
    @Override
    public void onBackPressed() {
        Intent closeApp = new Intent(Intent.ACTION_MAIN);
        closeApp.addCategory(Intent.CATEGORY_HOME);
        startActivity(closeApp);
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void searchHashtags(String hashtag) {

        mDataRef.child("hashtags").child(hashtag).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    mMap.clear();
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        String storyKey = ds.getKey();
                        String[] locationArray = ds.getValue(String.class).split(",");
                        LatLng storyPosition = new LatLng(Double.parseDouble(locationArray[0]),
                                Double.parseDouble(locationArray[1]));

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(storyPosition)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                        marker.setTag(storyKey);
                    }
                } else {
                    Toast.makeText(MapsActivityCurrentPlace.this, "There are no stories with that hashtag.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void addStory() {
        AlertDialog.Builder storyPrompt = new AlertDialog.Builder(this);
        storyPrompt.setTitle(R.string.add_story)
                .setItems(R.array.add_story_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0) {
                            Intent intent = new Intent(MapsActivityCurrentPlace.this, StoryUpload.class);
                            Bundle storySettings = new Bundle();
                            storySettings.putDouble("Latitude", mLastKnownLocation.getLatitude());
                            storySettings.putDouble("Longitude", mLastKnownLocation.getLongitude());
                            storySettings.putBoolean("FromCamera", true);
                            storySettings.putBoolean("Logged in", isLoggedIn());
                            intent.putExtras(storySettings);
                            startActivity(intent);
                        } else if(which == 1) {
                            Intent intent = new Intent(MapsActivityCurrentPlace.this, StoryUpload.class);
                            Bundle storySettings = new Bundle();
                            storySettings.putDouble("Latitude", mLastKnownLocation.getLatitude());
                            storySettings.putDouble("Longitude", mLastKnownLocation.getLongitude());
                            storySettings.putBoolean("FromCamera", false);
                            storySettings.putBoolean("Logged in", isLoggedIn());
                            intent.putExtras(storySettings);
                            startActivity(intent);
                        }
                    }
                });
        storyPrompt.create().show();

        /*if (mMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission")
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
                    int i = 0;
                    mLikelyPlaceNames = new String[mMaxEntries];
                    mLikelyPlaceAddresses = new String[mMaxEntries];
                    mLikelyPlaceAttributions = new String[mMaxEntries];
                    mLikelyPlaceLatLngs = new LatLng[mMaxEntries];
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        // Build a list of likely places to show the user. Max 5.
                        mLikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
                        mLikelyPlaceAddresses[i] = (String) placeLikelihood.getPlace().getAddress();
                        mLikelyPlaceAttributions[i] = (String) placeLikelihood.getPlace()
                                .getAttributions();
                        mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                        i++;
                        if (i > (mMaxEntries - 1)) {
                            break;
                        }
                    }
                    // Release the place likelihood buffer, to avoid memory leaks.
                    likelyPlaces.release();

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    openPlacesDialog();
                }
            });
        } else {
            // Add a default marker, because the user hasn't selected a place.
            mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));
        } */
    }

    public void getStories() {
        mDataRef.child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if(ds.exists() && ds.hasChild("Location")) {
                        String storyKey = ds.getKey();
                        String[] locationArray = ds.child("Location").getValue(String.class).split(",");

                        Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                        storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                        storyLocation.setLongitude(Double.parseDouble(locationArray[1]));

                        boolean flagged = ds.child("Flagged").getValue(boolean.class);
                        if(flagged){
                            showFlaggedStories(storyKey, storyLocation);
                        } else {
                            showNormalStories(storyKey, storyLocation);
                        }
                    } else {
                        Toast.makeText(MapsActivityCurrentPlace.this, "There are no stories.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapsActivityCurrentPlace.this, "Failed to load stories.", Toast.LENGTH_SHORT).show();
            }
        });

    }


    /*public void showOwnStories(String username) {
        mDataRef.child("users").child(username).child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren())
                {
                    String storyKey = ds.getValue(String.class);
                    String locationFromDatabase = ds.getKey();
                    String locationString = locationFromDatabase.replace("d", ".");
                    String [] locationArray = locationString.split(",");

                    LatLng storyLocation = new LatLng(Double.parseDouble(locationArray[0]), Double.parseDouble(locationArray[1]));

                    Marker storyMarker = mMap.addMarker(new MarkerOptions().position(storyLocation));
                    storyMarker.setTag(storyKey);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/

    public void showFlaggedStories(final String storyKey, final Location storyLocation) {

        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Marker storyMarker;
                    storyMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    storyMarker.setTag(storyKey);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

             }
        });

    }

    public void showNormalStories(final String storyKey, final Location storyLocation) {

        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Marker storyMarker;
                    storyMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    storyMarker.setTag(storyKey);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }




    @Override
    public boolean onMarkerClick(Marker marker){
        showStoryDetails(marker);
        return true;
    }

    public void showStoryDetails(Marker marker) {
        Intent intent = new Intent(this, ShowStory.class);
        String key = (String)marker.getTag();
        Bundle storyDetails = new Bundle();
        storyDetails.putString("key", key);
        storyDetails.putBoolean("Logged in", isLoggedIn());
        storyDetails.putDouble("Latitude", marker.getPosition().latitude);
        storyDetails.putDouble("Longitude", marker.getPosition().longitude);
        intent.putExtras(storyDetails);

        startActivity(intent);
    }

    /*
      Displays a form allowing the user to select a place from a list of likely places.
     */
    /*private void openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The "which" argument contains the position of the selected item.
                        LatLng markerLatLng = mLikelyPlaceLatLngs[which];
                        String markerSnippet = mLikelyPlaceAddresses[which];
                        if (mLikelyPlaceAttributions[which] != null) {
                            markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                        }
                        // Add a marker for the selected place, with an info window
                        // showing information about that place.
                        mMap.addMarker(new MarkerOptions()
                                .title(mLikelyPlaceNames[which])
                                .position(markerLatLng)
                                .snippet(markerSnippet));

                        // Position the map's camera at the location of the marker.
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                                DEFAULT_ZOOM));
                    }
                };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();
    } */

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
    }

    private boolean isLoggedIn() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean loggedIn = settings.getBoolean("Logged in", false);
        return loggedIn;
    }

    /*private String getUsername() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String username = settings.getString("Username", "");
        return username;
    }*/

    /*private void cleanDatabase() {
        mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    final String user = ds.getKey();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/

    /*private void cleanDatabase() {
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.child("stories").getChildren()) {
                    ds.child("User").getRef().removeValue();
                    ds.child("Viewers").getRef().removeValue();
                    ds.child("Upvoters").getRef().removeValue();
                    ds.child("Downvoters").getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/

}
