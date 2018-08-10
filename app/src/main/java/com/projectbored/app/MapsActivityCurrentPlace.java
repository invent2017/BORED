package com.projectbored.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.getbase.floatingactionbutton.FloatingActionButton;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_SEARCH;
import static android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH;

/**
 * An activity that displays a map showing the place at the device's current location.
 * Features included:
 * Get stories (colour-coded)
 * Filter stories: hashtag, read, nearby, today
 * Add-to-map actions: Add story, add event, multisquawk
 * Other actions: view profile, FAQs, contact us, log out
 */

public class MapsActivityCurrentPlace extends AppCompatActivity
        implements OnMapReadyCallback,
                GoogleApiClient.ConnectionCallbacks,
                GoogleApiClient.OnConnectionFailedListener {

    private static final String PREFS_NAME = "UserDetails";

    private static final String TAG = MapsActivityCurrentPlace.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is not granted.
    private final LatLng mDefaultLocation = new LatLng(1.346313, 103.841332);
    private static final int DEFAULT_ZOOM = 19;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    /** The geographical location where the device is currently located. That is, the last-known
    location retrieved by the Fused Location Provider. */
    private Location mLastKnownLocation;
    private LocationManager locationManager;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private FloatingActionButton exploreButton,addStoryButton,addEventButton;
    private HashtagSearchBar searchView;
    private TextView displayedUsername;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ListView mDrawerList;
    private String[] mDrawerItems;

    private String username;

    //leave in case we need to do something
    // Used for selecting the current place.
    //private final int mMaxEntries = 5;
    //private String[] mLikelyPlaceNames = new String[mMaxEntries];
    //private String[] mLikelyPlaceAddresses = new String[mMaxEntries];
    //private String[] mLikelyPlaceAttributions = new String[mMaxEntries];
    //private LatLng[] mLikelyPlaceLatLngs = new LatLng[mMaxEntries];

    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;
    private ValueEventListener storyListener;

    //private boolean isMapLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(com.projectbored.app.R.layout.activity_maps);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mNavigationView = findViewById(R.id.navigation);
        mDrawerList = findViewById(R.id.options_list);
        mDrawerItems = getResources().getStringArray(R.array.maps_drawer_options);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.item_row, mDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

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

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        username = mAuth.getCurrentUser().getUid();
        displayedUsername =  findViewById(R.id.my_username);
        mDataRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String actualUsername = dataSnapshot.child("Username").getValue(String.class);
                displayedUsername.setText(actualUsername);

                mDataRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        /* username = getSharedPreferences(PREFS_NAME, 0).getString("Username", "");
        if(username.equals("")) {
            Intent login = new Intent(this, Login.class);
            startActivity(login);
            finish();
        } else {

        } */

        // Triggers MultiSquawk where pictures are uploaded using geotags instead of current location
        exploreButton = findViewById(R.id.explore);
        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLastKnownLocation != null) {
                    multiSquawk();
                } else {
                    SingleToast.show(MapsActivityCurrentPlace.this,
                            "Unable to get your location. Please check your location settings and try again.",
                            Toast.LENGTH_SHORT);
                }
            }
        });

        // Triggers AddEvent where stories expire and indicate activities rather than passive scenes
        addEventButton = findViewById(R.id.add_event);
        addEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLastKnownLocation != null) {
                    addEvent();

                } else {
                    SingleToast.show(MapsActivityCurrentPlace.this,
                            "Unable to get your location. Please check your location settings and try again.",
                            Toast.LENGTH_SHORT);
                }
            }
        });

        //Triggers AddStory were stories are added using user's location
        addStoryButton = findViewById(R.id.add_story);
        addStoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLastKnownLocation != null) {
                    addStory();
                } else {
                    SingleToast.show(MapsActivityCurrentPlace.this,
                            "Unable to get your location. Please check your location settings and try again.",
                            Toast.LENGTH_SHORT);
                }
            }
        });
    }

    /*@Override
    protected void onResume() {
        if(isMapLoaded) {
            resetMap();
        }
        super.onResume();
    }*/

    //Created a ValueEventListener object so it can be detached
    private void initialiseDatabaseListener() {
        storyListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.child("locations").getChildren()) {
                    if(ds.exists()) {

                        String[] locationArray = ds.getKey().replace('d', '.').split(",");
                        Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                        storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                        storyLocation.setLongitude(Double.parseDouble(locationArray[1]));

                        if(ds.getChildrenCount() == 1) {
                            for (DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.exists()) {
                                    String storyKey = dataSnapshot1.getKey();
                                    int type = dataSnapshot1.getValue(Integer.class);
                                    boolean isRead = false;
                                    boolean isNotInterested = false;
                                    if (isLoggedIn()) {
                                        if (dataSnapshot.child("users").child(username)
                                                .child("ReadStories").hasChild(storyKey)) {
                                            isRead = true;
                                        } else if(dataSnapshot.child("users").child(username)
                                                .child("EventsNotInterested").hasChild(storyKey)) {
                                            isNotInterested = true;
                                        }
                                    }


                                    if(isRead || isNotInterested) {

                                    } else {
                                        if (mLastKnownLocation != null && mLastKnownLocation.distanceTo(storyLocation) <= 500) {
                                            showNearbyStories(storyKey, storyLocation, type);
                                        } else {
                                            showFarStories(storyKey, storyLocation, type);
                                        }
                                    }
                                }
                            }
                        } else {
                            StringBuilder storyKeys = new StringBuilder();
                            for(DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.getValue(Integer.class) == 0) {
                                    String storyKey = dataSnapshot1.getKey();

                                    boolean isRead = false;
                                    if (isLoggedIn()) {
                                        if (dataSnapshot.child("users").child(username)
                                                .child("ReadStories").child(storyKey).exists()) {
                                            isRead = true;
                                        }
                                    }

                                    if(!isRead) {
                                        if (storyKeys.toString().equals("")) {
                                            storyKeys.append(storyKey);
                                        } else {
                                            storyKeys.append(",").append(storyKey);
                                        }
                                    }
                                }
                            }

                            /*boolean featured = false;
                            for(DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                featured = dataSnapshot1.getValue(boolean.class);

                                if(featured) {
                                    break;
                                }
                            }*/

                            if(!storyKeys.toString().equals("")) {

                                if (mLastKnownLocation != null && mLastKnownLocation.distanceTo(storyLocation) <= 500) {
                                    showNearbyStories(storyKeys.toString(), storyLocation, 0);
                                } else {
                                    showFarStories(storyKeys.toString(), storyLocation, 0);
                                }
                            }
                        }
                    } else {
                        SingleToast.show(MapsActivityCurrentPlace.this, "There are no squawks.", Toast.LENGTH_SHORT);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                SingleToast.show(MapsActivityCurrentPlace.this, "Failed to load squawks.", Toast.LENGTH_SHORT);
            }
        };
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
                .findFragmentById(com.projectbored.app.R.id.map);
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
        getMenuInflater().inflate(com.projectbored.app.R.menu.current_place_menu, menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

       searchView = (HashtagSearchBar) menu.findItem(R.id.search_hashtags).getActionView();;
       initialiseSearch(searchView);

       return true;
   }

    /**
     * Handles a click on the menu option to get a place.
     * @param item The menu item to handle.
     * @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.option_reset_map) {
            resetMap();
        }
        return true;
    }

    /** Profile drawer with various options
     * case 0: view profile
     * case 1: filter stories
     * case 2: FAQs
     * case 3: contact us
     * case 4: log out
     */
    private void selectItem(int position) {
        switch (position) {
            case 0:
                Intent viewProfile = new Intent(this, UserProfile.class);
                startActivity(viewProfile);
                break;
            case 1:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Filter stories").setItems(R.array.filter_story_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        filterStories(which);
                    }
                });

                builder.create().show();

                break;
            case 2:
                Intent viewMyStories = new Intent(this, UserStories.class);
                startActivity(viewMyStories);
                break;
            case 3:
                Intent viewBookmarkedStories = new Intent(this, BookmarkedStories.class);
                startActivity(viewBookmarkedStories);
                break;
            case 4:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://projectboredinc.wordpress.com/faqs/"));
                SingleToast.show(MapsActivityCurrentPlace.this, "redirecting to FAQs on our website", Toast.LENGTH_SHORT);
                startActivity(browserIntent);
                break;
            case 5:
                Intent contactUs = new Intent(this, ContactUs.class);
                startActivity(contactUs);
                break;
            case 6:
                Intent logoutIntent = new Intent(this, Logout.class);
                startActivity(logoutIntent);
                finish();
                break;
        }

        mDrawerLayout.closeDrawer(mNavigationView);
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
            public boolean onMarkerClick(final Marker marker) {
                if(mLastKnownLocation == null) {
                    Toast.makeText(MapsActivityCurrentPlace.this,
                            "Unable to get your location. Please check your location settings and refresh the map.",
                            Toast.LENGTH_LONG).show();

                } else {

                    mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            LatLng markerPosition = marker.getPosition();
                            Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
                            markerLocation.setLatitude(markerPosition.latitude);
                            markerLocation.setLongitude(markerPosition.longitude);

                            int markerDistance = Math.round(mLastKnownLocation.distanceTo(markerLocation));
                            if (markerDistance <= 500) {
                                nearMarkerClicked(marker, dataSnapshot, markerDistance);
                            } else {
                                farMarkerClicked(marker, dataSnapshot, markerDistance);
                            }

                            mDataRef.removeEventListener(this);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

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


        // Displays stories on the map: showing all stories or filtered stories

        if(getIntent().getAction() != null) {
            if (getIntent().getAction().equals(Intent.ACTION_VIEW) && getIntent().getData() != null) {
                showSelectedStory(getIntent().getData().getLastPathSegment());
            } else {
                initialiseDatabaseListener();
                getStories();
            }

        } else {
            initialiseDatabaseListener();
            getStories();
        }

        //isMapLoaded = true;
    }

    private void nearMarkerClicked(Marker marker, DataSnapshot dataSnapshot, int markerDistance) {
        String[] storyDetails = ((String)marker.getTag()).split("/");
        String storyKey = storyDetails[0];
        int type = Integer.parseInt(storyDetails[1]);

        switch (type) {
            case 0:
                if(storyKey.contains(",")) {
                    showStoryDetails(marker);

                } else {
                    if (dataSnapshot.child("users").child(username).child("IgnoredStories").hasChild(storyKey)) {
                        showStoryDetails(marker);

                    } else {
                        String keywords = dataSnapshot.child("stories").child(storyKey).child("Keywords").getValue(String.class);
                        if (keywords == null) {
                            Toast.makeText(this,
                                    "There is a squwawk in " + markerDistance + " metres. Tap again to open!",
                                    Toast.LENGTH_SHORT).show();
                            mDataRef.child("users").child(username).child("IgnoredStories").child(storyKey).setValue(storyKey);
                        } else {
                            Toast.makeText(this,
                                    "In " + markerDistance + " metres, a squawk contains " + keywords + ". Tap again to open!",
                                    Toast.LENGTH_LONG).show();
                            mDataRef.child("users").child(username).child("IgnoredStories").child(storyKey).setValue(storyKey);
                        }
                    }
                }

                break;
            case 2:
                if(dataSnapshot.child("users").child(username).child("EventsInterested").hasChild(storyKey)) {
                    showStoryDetails(marker);
                } else {
                    if(storyKey.contains(",")) {
                        showStoryDetails(marker);
                    } else {
                        if(dataSnapshot.child("users").child(username).child("EventsNotInterested").hasChild(storyKey)){
                            showStoryDetails(marker);
                        } else {
                            String eventTitle = dataSnapshot.child("events").child(storyKey).child("Title").getValue(String.class);
                            long timeNow = Calendar.getInstance().getTimeInMillis();
                            long expiryTime = dataSnapshot.child("events").child(storyKey)
                                    .child("ExpiryTime").getValue(Long.class);
                            String timeToExpiry = new TimeDifferenceGenerator(timeNow, expiryTime).getDifference();

                            if (eventTitle == null) {
                                Toast.makeText(this, "There is an event in " + markerDistance + " metres," +
                                                " expiring in " + timeToExpiry + " from now. Tap again to open!",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "There is an event, " + eventTitle + "," +
                                                " in " + markerDistance + " metres," +
                                                " expiring in " + timeToExpiry + " from now. Tap again to open!",
                                        Toast.LENGTH_LONG).show();
                            }

                            mDataRef.child("users").child(username).child("EventsNotInterested")
                                    .child(storyKey).setValue(false);
                        }
                    }
                }
                break;
        }
    }

    private void farMarkerClicked(Marker marker, DataSnapshot dataSnapshot, int markerDistance) {
        String[] storyDetails = ((String)marker.getTag()).split("/");
        String storyKey = storyDetails[0];
        int type = Integer.parseInt(storyDetails[1]);

        switch (type) {
            case 0:
                if(storyKey.contains(",")) {
                    Toast.makeText(this,
                            "In " + markerDistance + " metres, there are multiple squawks.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String keywords = dataSnapshot.child("stories").child(storyKey).child("Keywords").getValue(String.class);
                    if (keywords == null) {
                        Toast.makeText(this,
                                "There is a squwawk in " + markerDistance + " metres.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "In " + markerDistance + "metres, a squawk contains " + keywords + ".",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;

            case 2:
                if(storyKey.contains(",")) {
                    Toast.makeText(this,
                            "In " + markerDistance + " metres, there are multiple events.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String eventTitle = dataSnapshot.child("events").child(storyKey).child("Title").getValue(String.class);
                    long timeNow = Calendar.getInstance().getTimeInMillis();
                    long expiryTime = dataSnapshot.child("events").child(storyKey).child("ExpiryTime").getValue(Long.class);
                    String timeToExpiry = new TimeDifferenceGenerator(timeNow, expiryTime).getDifference();

                    if(eventTitle == null) {
                        Toast.makeText(this, "There is an event in " + markerDistance + " metres, expiring in "
                                        + timeToExpiry + " from now.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "There is an event, " + eventTitle +"," +
                                        " in " + markerDistance + " metres," +
                                        " expiring in " + timeToExpiry + " from now.",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    //Closes app when back button is pressed, instead of returning to splash screen
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
        if(mLastKnownLocation != null) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }

            // Get the best and most recent location of the device, which may be null in rare
            // cases when a location is not available.
            if (mLocationPermissionGranted) {
                mLastKnownLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);

                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        mLastKnownLocation = location;
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }
                };

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
            }

        /*if (mLastKnownLocation == null) {
            Log.d(TAG, "Current location is null. Using defaults.");
            mLastKnownLocation = new Location(LocationManager.GPS_PROVIDER);
            mLastKnownLocation.setLatitude(mDefaultLocation.latitude);
            mLastKnownLocation.setLongitude(mDefaultLocation.longitude);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }*/

            boolean opened = getIntent().getBooleanExtra("Opened", false);

            if (!opened) {
                // Set the map's camera position to the current location of the device.
                if (mCameraPosition != null) {
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));


                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                }
                getIntent().putExtra("Opened", true);
            }
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
                    resetMap();  //refresh map to show location

                    updateLocationUI();
                }
            }
        }

    }

    //Hashtag search bar
    private void initialiseSearch(final AutoCompleteTextView searchView) {
        searchView.setHint("Search hashtags...                     ");
        searchView.setInputType(TYPE_CLASS_TEXT);
        searchView.setImeOptions(IME_ACTION_SEARCH);
        searchView.setMaxLines(1);

        mDataRef.child("hashtags").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final ArrayList<String> popularHashtags = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    if(ds.getChildrenCount() >= 2) {
                        popularHashtags.add(ds.getKey());
                    }
                }

                searchView.setAdapter(new SearchAdapter(MapsActivityCurrentPlace.this, popularHashtags));

                searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if(i > 0 && i < 5) {
                            filterStories(i - 1);
                        } else if(i > 5) {
                            String query = popularHashtags.get(i - 6);
                            searchHashtags(view, query);
                        }

                        //Dismisses keyboard after item chosen
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                        searchView.clearFocus();
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        searchView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(i == KEYCODE_SEARCH || i == KEYCODE_ENTER) {
                    String query = searchView.getText().toString();
                    if (!query.equals("")) {
                        if (query.contains("#")) {
                            query = query.substring(1);
                        }
                        searchHashtags(view, query);
                    }

                    return true;
                } else {
                    return false;
                }
            }
        });

    }


    private void searchHashtags(final View view, String hashtag) {
        String legitInput= "\\w+";
        Matcher matcher = Pattern.compile(legitInput).matcher(hashtag);

        if (matcher.matches()) {
            mDataRef.child("hashtags").child(hashtag).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        mMap.clear();
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            String storyKey = ds.getKey();
                            String[] locationArray = ds.getValue(String.class).split(",");
                            LatLng storyPosition = new LatLng(Double.parseDouble(locationArray[0]),
                                    Double.parseDouble(locationArray[1]));

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(storyPosition)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                            marker.setTag(storyKey + "/" +0);

                            //Hide keyboard
                            if (view != null) {
                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                        }
                    } else {
                        SingleToast.show(MapsActivityCurrentPlace.this, "There are no stories with that hashtag.", Toast.LENGTH_SHORT);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            SingleToast.show(this, "Hashtags may not contain spaces or non-word characters.", Toast.LENGTH_SHORT);
        }
    }

    // Method for only showing stories with the indicated characteristics
    private void showSelectedStory(final String storyKey) {

        mMap.clear();
        mDataRef.child("stories").child(storyKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null) {
                    String[] locationArray = dataSnapshot.child("Location").getValue(String.class).split(",");
                    LatLng storyPosition = new LatLng(Double.parseDouble(locationArray[0]),
                            Double.parseDouble(locationArray[1]));

                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(storyPosition)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    marker.setTag(storyKey + "/" + 0);

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(storyPosition));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // Filter Stories choice
    private void filterStories(int which) {
        mMap.clear();

        if(which == 0) {
            filterNearbyStories();
        } else if(which == 1) {
            filterTodayStories();
        } else if(which == 2) {
            filterMyStories(username);
        }  else if(which == 3) {
            filterReadStories(username);
        }


    }

    // Only stories posted within 24h will be shown
    private void filterTodayStories() {
        mDataRef.removeEventListener(storyListener);
        mDataRef.child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    if(ds.exists()) {
                        String[] locationArray = ds.child("Location").getValue(String.class).split(",");
                        Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                        storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                        storyLocation.setLongitude(Double.parseDouble(locationArray[1]));


                        //supposed to compare date

                        //gets User date here
                        long todayDate = Calendar.getInstance().getTimeInMillis();

                        //gets story date below

                        String storyKey = ds.getKey();
                        long storyDate = ds.child("Time").getValue(Long.class);

                        if(todayDate - storyDate <= 86400000) {
                            Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                            storyMarker.setTag(storyKey+ "/"+ 0);
                        }

                    }
                }
                mDataRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /*private void filterFeaturedStories() {
        mDataRef.child("locations").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    if(ds.exists()) {
                        String[] locationArray = ds.getKey().replace('d', '.').split(",");
                        Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                        storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                        storyLocation.setLongitude(Double.parseDouble(locationArray[1]));

                        if(ds.getChildrenCount() == 1) {
                            for(DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.exists()) {
                                    String storyKey = dataSnapshot1.getKey();
                                    boolean featured = dataSnapshot1.getValue(boolean.class);

                                    if(featured) {
                                        if(mLastKnownLocation.distanceTo(storyLocation) <= 100) {
                                            Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                            storyMarker.setTag(storyKey);
                                        } else {
                                            Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                            storyMarker.setTag(storyKey);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/

    // Only stories the user has read will be shown
    private void filterReadStories(final String username) {
        mDataRef.removeEventListener(storyListener);
        mDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.child("locations").getChildren()) {
                    if(ds.exists()) {
                        String[] locationArray = ds.getKey().replace('d', '.').split(",");
                        Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                        storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                        storyLocation.setLongitude(Double.parseDouble(locationArray[1]));

                        if(ds.getChildrenCount() == 1) {
                            for (DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.exists()) {
                                    String storyKey = dataSnapshot1.getKey();
                                    boolean isRead = false;
                                    if (isLoggedIn()) {
                                        if (dataSnapshot.child("users").child(username)
                                                .child("ReadStories").child(storyKey).exists()) {
                                            isRead = true;
                                        }
                                    }

                                    if (isRead) {
                                        if(mLastKnownLocation.distanceTo(storyLocation) <= 500) {
                                            Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                            storyMarker.setTag(storyKey+ "/"+ 0);
                                        } else {
                                            Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                                            storyMarker.setTag(storyKey+ "/"+ 0);
                                        }

                                    }
                                }
                            }
                        } else {
                            StringBuilder storyKeys = new StringBuilder();
                            for(DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.getValue(Integer.class) == 0) {
                                    String storyKey = dataSnapshot1.getKey();

                                    boolean isRead = false;
                                    if (isLoggedIn()) {
                                        if (dataSnapshot.child("users").child(username)
                                                .child("ReadStories").child(storyKey).exists()) {
                                            isRead = true;
                                        }
                                    }

                                    if(isRead) {
                                        if (storyKeys.toString().equals("")) {
                                            storyKeys.append(storyKey);
                                        } else {
                                            storyKeys.append(",").append(storyKey);
                                        }
                                    }
                                }
                            }

                            if(mLastKnownLocation.distanceTo(storyLocation) <= 500) {
                                Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                storyMarker.setTag(storyKeys.toString()+ "/"+ 0);
                            } else {
                                Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                                storyMarker.setTag(storyKeys+ "/"+ 0);
                            }
                        }
                    } else {
                        SingleToast.show(MapsActivityCurrentPlace.this, "There are no stories.", Toast.LENGTH_SHORT);
                    }
                }
                mDataRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // Stories withing clicking distance (500m) will be shown
    private void filterNearbyStories() {
        mDataRef.removeEventListener(storyListener);
        mDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.child("locations").getChildren()) {
                    if(ds.exists()) {
                        String[] locationArray = ds.getKey().replace('d', '.').split(",");
                        Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                        storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                        storyLocation.setLongitude(Double.parseDouble(locationArray[1]));

                        if(ds.getChildrenCount() == 1) {
                            for (DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.exists()) {
                                    String storyKey = dataSnapshot1.getKey();
                                    int type = dataSnapshot1.getValue(Integer.class);

                                    if (mLastKnownLocation != null && mLastKnownLocation.distanceTo(storyLocation) <= 500) {
                                        showNearbyStories(storyKey, storyLocation, type);
                                    }
                                }
                            }
                        } else {
                            StringBuilder storyKeys = new StringBuilder();
                            for(DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.getValue(Integer.class) == 0) {
                                    String storyKey = dataSnapshot1.getKey();

                                    if (storyKeys.toString().equals("")) {
                                        storyKeys.append(storyKey);
                                    } else {
                                        storyKeys.append(",").append(storyKey);
                                    }

                                }
                            }

                            if(mLastKnownLocation.distanceTo(storyLocation) <= 500) {
                                showNearbyStories(storyKeys.toString(), storyLocation, 0);
                            }
                        }
                    } else {
                        SingleToast.show(MapsActivityCurrentPlace.this, "There are no stories.", Toast.LENGTH_SHORT);
                    }
                }
                mDataRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // Stories the user has posted will be shown
    private void filterMyStories(final String username) {
        mDataRef.removeEventListener(storyListener);
        mDataRef.child("users").child(username).child("stories").addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    if(ds.exists()) {
                        String storyKey = ds.getKey();

                        String[] locationArray = ds.getValue(String.class).split(",");
                        Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(locationArray[0]), Double.parseDouble(locationArray[1]))));
                        storyMarker.setTag(storyKey + "/"+ 0);
                    }
                }

                mDataRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    // Bundles necessary information and then opens multiSquawk activity
    private void multiSquawk() {
        Intent intent = new Intent(MapsActivityCurrentPlace.this, MultiSquawk.class);
        Bundle storySettings = new Bundle();
        storySettings.putBoolean("FromCamera", false);
        storySettings.putBoolean("Logged in", isLoggedIn());
        intent.putExtras(storySettings);
        startActivity(intent);
    }

    // Bundles necessary information and then opens addEvent activity
    private void addEvent() {
        Intent intent = new Intent(MapsActivityCurrentPlace.this, EventUpload.class);

        Bundle extras = new Bundle();
        extras.putDouble("Latitude", mLastKnownLocation.getLatitude());
        extras.putDouble("Longitude", mLastKnownLocation.getLongitude());
        intent.putExtras(extras);
        startActivity(intent);
    }

    // Bundles necessary information and then opens addStory activity
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

    // Displays all stories on map. Nearby stories are defined at 500m.
    public void getStories() {
        mDataRef.addValueEventListener(storyListener);

    }

    // following two methods determine marker colours based on user location
    // Nearby story: Yellow
    // Nearby event: Green
    // Far event: Blue
    // Far story: Purple
    public void showNearbyStories(String storyKey, Location storyLocation, int type) {
        Marker storyMarker;
        if (type == 2) {
            storyMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(storyLocation.getLatitude(),
                                storyLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            storyMarker.setTag(storyKey + "/" + "2");
        } else {
            storyMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(storyLocation.getLatitude(),
                                storyLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
            storyMarker.setTag(storyKey + "/" + 0);
        }
    }

    public void showFarStories(final String storyKey, final Location storyLocation, int type) {
        Marker storyMarker;
        if(type == 2) {
            storyMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(storyLocation.getLatitude(),
                            storyLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            storyMarker.setTag(storyKey + "/" + 2);
        } else {
            storyMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(storyLocation.getLatitude(),
                            storyLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            storyMarker.setTag(storyKey + "/" + 0);
        }
    }

    // Opens story details in new activity
    public void showStoryDetails(Marker marker) {
        Intent intent;
        String info = (String)marker.getTag();
        if(info != null){
            String[] storyInfo = info.split("/");
            String key = storyInfo[0];
            int type = Integer.parseInt(storyInfo[1]);

            if(key == null) {
                SingleToast.show(this, "You have already read this squawk.", Toast.LENGTH_SHORT);
                marker.remove();
            } else {

                //Checks what type of squawk is contained.
                // 0: Story
                // 2: Event
                if (key != null && key.contains(",")) {
                    intent = new Intent(this, ShowMultipleStories.class);
                } else {
                    switch(type) {
                        case 0:
                            intent = new Intent(this, ShowStory.class);
                            break;
                        case 2:
                            intent = new Intent(this, ViewEvent.class);
                            break;

                        default:
                            intent = new Intent(this, ShowStory.class);
                    }

                }

                Bundle storyDetails = new Bundle();
                storyDetails.putString("key", key);
                storyDetails.putBoolean("Logged in", isLoggedIn());
                storyDetails.putDouble("Latitude", marker.getPosition().latitude);
                storyDetails.putDouble("Longitude", marker.getPosition().longitude);
                intent.putExtras(storyDetails);

                startActivity(intent);
            }
        } else
        {
            SingleToast.show(this, "Squawk has been deleted. Please refresh map.", Toast.LENGTH_SHORT);
        }
    }

    // Map is reset to the current location and all markers will be shown
    private void resetMap() {
        if(storyListener != null) {
            mDataRef.removeEventListener(storyListener);
        }
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                checkEvents(dataSnapshot);
                mDataRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Intent reload = new Intent(this, MapsActivityCurrentPlace.class);
        finish();
        startActivity(reload);
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
f
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
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);

        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            mLastKnownLocation = null;
            AlertDialog.Builder locationPermissionPrompt = new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("See GO requires location permission to run.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateLocationUI();
                        }
                    });
            locationPermissionPrompt.create().show();
        }
    }

    // Checks if user is logged in
    private boolean isLoggedIn() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean loggedIn = settings.getBoolean("Logged in", false);
        return loggedIn;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            selectItem(i);
        }
    }

    // Toast method helps to reduce toast accumulation
    public static class SingleToast {

        private static Toast mToast;

        public static void show(Context context, String text, int duration) {
            if (mToast != null) mToast.cancel();
            mToast = Toast.makeText(context, text, duration);
            mToast.show();
        }
    }

    private void checkEvents(DataSnapshot dataSnapshot) {

        for(DataSnapshot ds : dataSnapshot.child("events").getChildren()) {
            long expiryTime = ds.child("ExpiryTime").getValue(Long.class);
            long timeNow = Calendar.getInstance().getTimeInMillis();

            if(timeNow >= expiryTime) {
                String key = ds.getKey();
                String location = ds.child("Location").getValue(String.class).replace('.', 'd');
                mDataRef.child("locations").child(location).child(key).removeValue();
                ds.getRef().removeValue();

                for(DataSnapshot dataSnapshot1 : dataSnapshot.child("users").getChildren()) {
                    for(DataSnapshot dataSnapshot2 : dataSnapshot1.child("EventsInterested").getChildren()) {
                        if(key.equals(dataSnapshot2.getKey())) {
                            dataSnapshot2.getRef().removeValue();
                        }
                    }

                    for(DataSnapshot dataSnapshot2 : dataSnapshot1.child("EventsNotInterested").getChildren()) {
                        if(key.equals(dataSnapshot2.getKey())) {
                            dataSnapshot2.getRef().removeValue();
                        }
                    }
                }
            }
        }
    }

}


/* Random Code for See Read Stories

private void seeReadStories(final String username) {
        mDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.child("locations").getChildren()) {
                    if(ds.exists()) {
                        String[] locationArray = ds.getKey().replace('d', '.').split(",");
                        Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                        storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                        storyLocation.setLongitude(Double.parseDouble(locationArray[1]));

                        if(ds.getChildrenCount() == 1) {
                            for (DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.exists()) {
                                    String storyKey = dataSnapshot1.getKey();
                                    boolean isRead = false;
                                    if (dataSnapshot.child("users").child(username)
                                            .child("ReadStories").child(storyKey).exists()) {
                                    }
                                    if (!isRead) {
                                        Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                                        storyMarker.setTag(storyKey);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

 */
