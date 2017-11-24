package com.projectbored.app;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

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
    private static final int DEFAULT_ZOOM = 19;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private FloatingActionButton exploreButton,addStoryButton,addEventButton;

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
        setContentView(com.projectbored.app.R.layout.activity_maps);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);


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

        mDataRef = FirebaseDatabase.getInstance().getReference();

        exploreButton = (FloatingActionButton)findViewById(R.id.explore);
        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLastKnownLocation != null) {
                    exploreTrails();
                } else {
                    Toast.makeText(MapsActivityCurrentPlace.this,
                            "Unable to get your location. Please check your location settings and try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        addEventButton = (FloatingActionButton)findViewById(R.id.add_event);
        addEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLastKnownLocation != null) {
                    addEvent();
                } else {
                    Toast.makeText(MapsActivityCurrentPlace.this,
                            "Unable to get your location. Please check your location settings and try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        addStoryButton = (FloatingActionButton)findViewById(R.id.add_story);
        addStoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLastKnownLocation != null) {
                    addStory();
                } else {
                    Toast.makeText(MapsActivityCurrentPlace.this,
                            "Unable to get your location. Please check your location settings and try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
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

       SearchManager searchManager =
               (SearchManager) getSystemService(Context.SEARCH_SERVICE);
       final SearchView searchView =
               (SearchView) menu.findItem(R.id.search_hashtags).getActionView();
       searchView.setSearchableInfo(
               searchManager.getSearchableInfo(getComponentName()));
       searchView.setIconifiedByDefault(false);
       searchView.setQueryHint("Hashtags...");
       searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
           @Override
           public boolean onQueryTextSubmit(String query) {
               if (!query.equals("")) {
                   if (query.contains("#")) {
                       query = query.substring(1);
                   }
                   searchHashtags(query);
                   //Hide keyboard
                   View view = MapsActivityCurrentPlace.this.getCurrentFocus();
                   if (view != null) {
                       InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                       imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                   }
               }
               return true;
           }

           @Override
           public boolean onQueryTextChange(String newText) {
               return false;
           }
       });
       searchView.setOnSearchClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               String query = searchView.getQuery().toString();
               if (!query.equals("")) {
                   if (query.contains("#")) {
                       query = query.substring(1);
                   }
                   searchHashtags(query);
                   //Hide keyboard
                   View view = MapsActivityCurrentPlace.this.getCurrentFocus();
                   if (view != null) {
                       InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                       imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                   }
               }
           }
       });

        return true;
        }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logInOption = menu.findItem(R.id.option_log_in);
        MenuItem logOutOption = menu.findItem(R.id.option_log_out);
        MenuItem viewProfileOption = menu.findItem(R.id.option_manage_account);

        if(isLoggedIn()) {
            logInOption.setVisible(false);
            logOutOption.setVisible(true);
            viewProfileOption.setVisible(true);
        } else {
            logInOption.setVisible(true);
            logOutOption.setVisible(false);
            viewProfileOption.setVisible(false);
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
        if(item.getItemId() == R.id.option_filter_stories) {
            filterStories();
        } else if (item.getItemId() == R.id.option_log_in) {
            Intent loginIntent = new Intent(this, Login.class);
            startActivity(loginIntent);
        } else if(item.getItemId() == R.id.option_log_out) {
            Intent logoutIntent = new Intent(this, Logout.class);
            startActivity(logoutIntent);
        } else if(item.getItemId() == R.id.option_reset_map) {
            resetMap();
        } else if(item.getItemId() == R.id.option_manage_account) {
            Intent viewProfile = new Intent(this, UserProfile.class);
            startActivity(viewProfile);
        }
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

                if(mLastKnownLocation.distanceTo(markerLocation) <= 100) {
                    showStoryDetails(marker);
                } else {
                    Toast.makeText(MapsActivityCurrentPlace.this, "You must be within 100 metres of the story to view it.", Toast.LENGTH_SHORT).show();
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


        if(getIntent().getAction() != null) {
            if (getIntent().getAction().equals(Intent.ACTION_VIEW) && getIntent().getData() != null) {
                showSelectedStory(getIntent().getData());
            } else {
                getStories();
            }
        } else {
            //Load stories.
            getStories();
        }
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
        // Get the best and most recent location of the device, which may be null in rare
        // cases when a location is not available.
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        if (mLastKnownLocation == null) {
            Log.d(TAG, "Current location is null. Using defaults.");
            mLastKnownLocation = new Location(LocationManager.GPS_PROVIDER);
            mLastKnownLocation.setLatitude(mDefaultLocation.latitude);
            mLastKnownLocation.setLongitude(mDefaultLocation.longitude);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        boolean opened = getIntent().getBooleanExtra("Opened", false);

        if(!opened) {
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

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */

    private void showSelectedStory(Uri appLinkData) {
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

    private void searchHashtags(String hashtag) {
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
        } else {
            Toast.makeText(this, "Hashtags may not contain spaces or non-word characters.", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterStories() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter stories").setItems(R.array.filter_story_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                mMap.clear();

                if(which == 0) {
                    filterFeaturedStories();
                } else if(which == 1) {
                    if(isLoggedIn()) {
                        filterUnreadStories(getSharedPreferences(PREFS_NAME, 0).getString("Username", ""));
                    } else {
                        Toast.makeText(MapsActivityCurrentPlace.this,
                                "You must log in to use this filter.", Toast.LENGTH_SHORT).show();
                    }
                } else if(which == 2) {
                    filterNearbyStories();
                } else if(which == 3) {
                    if(isLoggedIn()) {
                        filterMyStories(getSharedPreferences(PREFS_NAME, 0).getString("Username", ""));
                    } else {
                        Toast.makeText(MapsActivityCurrentPlace.this,
                                "You must log in to use this filter.", Toast.LENGTH_SHORT).show();
                    }
                } else if(which ==4) {
                    filterTodayStories();
                }
            }
        });

        builder.create().show();
    }

    private void filterTodayStories() {
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
                            //supposed to compare date

                            //gets User date here
                            Date todayDate = Calendar.getInstance().getTime();

                            //gets story date below
                            for(DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.exists()) {
                                    String storyKey = dataSnapshot1.getKey();

                                    // btw I added 1 to the story's day, that way it will be after the user's current day if it is on the same day as the user
                                    int storyDay = 1+ dataSnapshot.child("DateTime").child("date").getValue(Integer.class);
                                    int storyMonth = 1+ dataSnapshot.child("DateTime").child("month").getValue(Integer.class);
                                    int storyYear = 1900 + dataSnapshot.child("DateTime").child("year").getValue(Integer.class);

                                    StringBuilder storyD = new StringBuilder().append(storyDay).append("-").append(storyMonth).append("-").append(storyYear);
                                    String storyDateString = storyD.toString();
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                                    // my guess is that this thing doesn't parse because storyDay and storyMonth are not necessarily 2-digit so the sdf doesn't correspond
                                    /*

                                    Date storyDate = sdf.parse(storyDateString);

                                    if (todayDate.after(storyDateString)) {
                                        Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                                        storyMarker.setTag(storyKey);
                                    }

                                    */
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



    private void filterFeaturedStories() {
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
    }

    private void filterUnreadStories(final String username) {
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

                                    if (!isRead) {
                                        if(mLastKnownLocation.distanceTo(storyLocation) <= 100) {
                                            Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                            storyMarker.setTag(storyKey);
                                        } else {
                                            Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(new LatLng(storyLocation.getLatitude(), storyLocation.getLongitude()))
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                                            storyMarker.setTag(storyKey);
                                        }

                                    }
                                }
                            }
                        } else {

                        }
                    } else {
                        Toast.makeText(MapsActivityCurrentPlace.this, "There are no stories.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void filterNearbyStories() {
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
                                    boolean featured = dataSnapshot1.getValue(boolean.class);
                                    boolean isRead = false;
                                    if (isLoggedIn()) {
                                        String username = getSharedPreferences(PREFS_NAME, 0)
                                                .getString("Username", "");
                                        if (dataSnapshot.child("users").child(username)
                                                .child("ReadStories").child(storyKey).exists()) {
                                            isRead = true;
                                        }
                                    }

                                    if (mLastKnownLocation != null && mLastKnownLocation.distanceTo(storyLocation) <= 100) {
                                        showNearbyStories(storyKey, storyLocation, featured, isRead);
                                    }
                                }
                            }
                        } else {

                        }
                    } else {
                        Toast.makeText(MapsActivityCurrentPlace.this, "There are no stories.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void filterMyStories(String username) {
        mDataRef.child("users").child(username).child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    if(ds.exists()) {
                        String storyKey = ds.getKey();

                        String[] locationArray = ds.getValue(String.class).split(",");
                        Marker storyMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(locationArray[0]), Double.parseDouble(locationArray[1]))));
                        storyMarker.setTag(storyKey);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void exploreTrails() {
        Intent intent = new Intent(MapsActivityCurrentPlace.this, Trails.class);
        startActivity(intent);
    }

    private void addEvent() {
        Intent intent = new Intent(MapsActivityCurrentPlace.this, EventUpload.class);
        startActivity(intent);
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
        mDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.child("locations").getChildren()) {
                    if(ds.exists()) {
                        if(ds.getChildrenCount() == 1) {
                            for (DataSnapshot dataSnapshot1 : ds.getChildren()) {
                                if(dataSnapshot1.exists()) {
                                    String storyKey = dataSnapshot1.getKey();
                                    String[] locationArray = ds.getKey().replace('d', '.').split(",");
                                    boolean featured = dataSnapshot1.getValue(boolean.class);

                                    Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
                                    storyLocation.setLatitude(Double.parseDouble(locationArray[0]));
                                    storyLocation.setLongitude(Double.parseDouble(locationArray[1]));

                                    boolean isRead = false;
                                    if (isLoggedIn()) {
                                        String username = getSharedPreferences(PREFS_NAME, 0)
                                                .getString("Username", "");
                                        if (dataSnapshot.child("users").child(username)
                                                .child("ReadStories").child(storyKey).exists()) {
                                            isRead = true;
                                        }
                                    }

                                    if (mLastKnownLocation != null && mLastKnownLocation.distanceTo(storyLocation) <= 100) {
                                        showNearbyStories(storyKey, storyLocation, featured, isRead);
                                    } else {
                                        showFarStories(storyKey, storyLocation, featured, isRead);
                                    }
                                }
                            }
                        } else {

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

    public void showNearbyStories(String storyKey, Location storyLocation, boolean featured, boolean isRead) {
        Marker storyMarker;
        if (featured) {
            storyMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(storyLocation.getLatitude(),
                                storyLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            storyMarker.setTag(storyKey);
        } else if (isRead) {
            storyMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(storyLocation.getLatitude(),
                                storyLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker()));
            storyMarker.setTag(storyKey);
        } else {
            storyMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(storyLocation.getLatitude(),
                                storyLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
            storyMarker.setTag(storyKey);
        }
    }

    public void showFarStories(final String storyKey, final Location storyLocation, boolean featured, boolean isRead) {
        Marker storyMarker;
        if(featured) {
            storyMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(storyLocation.getLatitude(),
                            storyLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            storyMarker.setTag(storyKey);
        } else if(isRead) {
            storyMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(storyLocation.getLatitude(),
                            storyLocation.getLongitude())));
            storyMarker.setTag(storyKey);
        } else {
            storyMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(storyLocation.getLatitude(),
                            storyLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            storyMarker.setTag(storyKey);
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker){
        LatLng markerPosition = marker.getPosition();
        Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
        markerLocation.setLatitude(markerPosition.latitude);
        markerLocation.setLongitude(markerPosition.longitude);

        if(mLastKnownLocation.distanceTo(markerLocation) <= 100) {
            showStoryDetails(marker);
        } else {
            Toast.makeText(this, "You must be within 100 metres of the story to view it.", Toast.LENGTH_SHORT).show();
        }
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

    private void resetMap() {
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
            // TODO: Why do you need the line below?
            // mLastKnownLocation = null;
            AlertDialog.Builder locationPermissionPrompt = new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("BORED! requires location permission to run.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MapsActivityCurrentPlace.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }
                    });
            locationPermissionPrompt.create().show();
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

}
