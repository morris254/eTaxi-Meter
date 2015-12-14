package com.iotaconcepts.distancecalc;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{
    private GoogleMap mMap;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    Button startWalking, save_fare;
    TextView lat_lon_disp, distanceDisplay, fare_display;
    CheckBox fare_checkbox;
    EditText et_fare_rate;
    FloatingActionButton fab;

    GPSTracker gps;
    PolylineOptions line;

    Double lat_1, lon_1, L1, L2;
    int count, a, fare_value;
    float distanceTravelled;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        startWalking = (Button)findViewById(R.id.bt_startWalking);
        distanceDisplay = (TextView)findViewById(R.id.tv_distance);
        fare_checkbox = (CheckBox)findViewById(R.id.cb_fare_checkbox);
        et_fare_rate = (EditText)findViewById(R.id.et_fare_rate);
        save_fare = (Button)findViewById(R.id.bt_save_fare);
        fare_display = (TextView)findViewById(R.id.tv_fare);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        distanceTravelled = 0;
        count = 0;
        a = 0;

        if (checkPlayServices())
        {
            buildGoogleApiClient();
            createLocationRequest();
        }

        try
        {
            initilizeMap();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        fare_checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                et_fare_rate.setVisibility(View.VISIBLE);
                save_fare.setVisibility(View.VISIBLE);
            }
        });

        save_fare.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                save_fare.setVisibility(View.INVISIBLE);
                et_fare_rate.setVisibility(View.INVISIBLE);
                fare_display.setVisibility(View.VISIBLE);
                fare_value = Integer.parseInt(et_fare_rate.getText().toString());
            }
        });

        startWalking.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                gps = new GPSTracker(MapsActivity.this);
                if(gps.canGetLocation())
                {
                    lat_1 = gps.getLatitude();
                    lon_1 = gps.getLongitude();
                }
                else
                {
                    //       gps.showSettingsAlert();
                }

                if (a == 0)
                {
                    // Snackbar
                    Snackbar snack = Snackbar.make(v, "Started measuring distance travelled.", Snackbar.LENGTH_LONG).setAction("Action", null);
                    ViewGroup group = (ViewGroup) snack.getView();
                    group.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.floroGreen));
                    snack.show();
                    distanceDisplay.setVisibility(View.VISIBLE);
                    fare_checkbox.setVisibility(View.VISIBLE);
                    a = 1;
                }
                else if(a == 1)
                {
                    // Snackbar
                    Snackbar snack = Snackbar.make(v, "Stoped measuring distance travelled.", Snackbar.LENGTH_LONG).setAction("Action", null);
                    ViewGroup group = (ViewGroup) snack.getView();
                    group.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.floroGreen));
                    snack.show();
                    a = 0;
                }

                // Plot First PolyLine
                line = new PolylineOptions().add(new LatLng(lat_1, lon_1), new LatLng(lat_1, lon_1)).width(13).color(getApplicationContext().getResources().getColor(R.color.floroGreen));
                mMap.addPolyline(line);

                // Watchout for location updates
                togglePeriodicLocationUpdates();
            }
        });



    } // On create


    /**
     * function to load map. If map is not created it will create it for you
     * */
    private void initilizeMap()
    {
        if (mMap == null)
        {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setMyLocationEnabled(true);

            gps = new GPSTracker(MapsActivity.this);
            if(gps.canGetLocation())
            {
                L1 = gps.getLatitude();
                L2 = gps.getLongitude();
            }
            else
            {
                //       gps.showSettingsAlert();
            }

            LatLng coordinate = new LatLng(L1, L2);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 14);
            mMap.animateCamera(yourLocation);


            // check if map is created successfully or not
            if (mMap == null)
            {
                Toast.makeText(getApplicationContext(), "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Method to display the location on UI ************************************************************ F L A G ************
     * */
    private void displayLocation()
    {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null)
        {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            String sTemp = String.format("%.2f", distanceTravelled);

            distanceDisplay.setText("Distace travelled: " + sTemp + " km");

            float temp = distanceTravelled * fare_value;
            fare_display.setText("Total fare: " + temp);

            count++;
        }
        else
        {
            lat_lon_disp.setText("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }


    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates()
    {

        if (!mRequestingLocationUpdates)
        {
            // Changing the button text
            //startWalking.setText(getString(R.string.btn_stop_location_updates));

            startWalking.setBackgroundColor(getResources().getColor(R.color.floroGreen));
            startWalking.setTextColor(getResources().getColor(R.color.colorPrimary));
            startWalking.setText("STOP JOURNEY");




            mRequestingLocationUpdates = true;

            startLocationUpdates();
        }
        else
        {
            // Changing the button text
            //startWalking.setText(getString(R.string.btn_start_location_updates));


            startWalking.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            startWalking.setTextColor(getResources().getColor(R.color.floroGreen));
            startWalking.setText("START JOURNEY");


            mRequestingLocationUpdates = false;
            // Stopping the location updates
            stopLocationUpdates();
        }
    }


    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates()
    {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onConnected(Bundle arg0)
    {
        // Once connected with google api, get the location
        displayLocation();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    /**
     * On Location Change ******************************************************************************* F L A G ************
     */
    @Override
    public void onLocationChanged(Location location)
    {
        // Assign the new location
        mLastLocation = location;

        Double lat_2, lon_2;
        lat_2 = location.getLatitude();
        lon_2 = location.getLongitude();

        //Toast.makeText(getApplicationContext(), "Location changed!" + lat_2 +"   "+lon_2, Toast.LENGTH_SHORT).show();

        // Plot new Polyline on map
        line = new PolylineOptions().add(new LatLng(lat_1, lon_1), new LatLng(lat_2, lon_2)).width(13).color(getApplicationContext().getResources().getColor(R.color.floroGreen));
        mMap.addPolyline(line);

        // Calculating distance between new location and previous location
        float[] results = new float[1];
        Location.distanceBetween(lat_1, lon_1, lat_2, lon_2, results);
        distanceTravelled = distanceTravelled + (results[0] / 1000);


        lat_1 = lat_2;
        lon_1 = lon_2;

        // Displaying the new location on UI
        displayLocation();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}

