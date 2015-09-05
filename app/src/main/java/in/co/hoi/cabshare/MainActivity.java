package in.co.hoi.cabshare;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import org.apache.commons.codec.DecoderException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class MainActivity extends ActionBarActivity implements android.location.LocationListener, GoogleMap.OnMapClickListener, LoaderCallbacks<Cursor>, GoogleMap.OnMarkerDragListener {

    /*
     * Constants
     */
    private final static int BASECHARGE = 30;
    private static String authenticationHeader;
    private final static String BASE_SERVER_URL = "http://www.hoi.co.in/api/";
    public static final int NOTIFICATION_ID = 1;

    /*
     * Markers for Google Map
     */
    private GoogleMap mGoogleMap;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private Marker cabMarker;
    private String[] sAddress = new String[2];
    private String[] dAddress = new String[2];

    /*
     *   variables for navigation drawer
     */
    RecyclerView mRecyclerView;                           // Declaring RecyclerView
    RecyclerView.Adapter mAdapter;                        // Declaring Adapter For Recycler View
    RecyclerView.LayoutManager mLayoutManager;            // Declaring Layout Manager as a linear layout manager
    DrawerLayout Drawer;                                  // Declaring DrawerLayout
    ActionBarDrawerToggle mDrawerToggle;                  // Declaring Action Bar Drawer Toggle

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    List<DrawerItem> dataList;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;

    private DetailCar carDetails;
    private DetailCurrentRide currentRideDetails;
    private DetailDriver driverDetails;
    private DetailUser userDetails;
    private boolean inRide;
    private boolean awaitingRide;
    private boolean ratingPending;
    private int rideRequestId;
    private int cabArrivalDuration;
    private int cabDistanceAway;
    CabBookingDetails cabBookingDetails;
    private int availableinr;
    boolean rideBooked = false;
    boolean openRide = false;



    /*
     * Miscellaneous variables
     */
    ObscuredSharedPreferences prefs;
    private LatLng currentLocationCoordinate;
    private LatLng cabLatestCoordinate;
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    int defaultView = 1;
    Fragment displayFragment;
    boolean cabNearCheck = true;
    boolean setup = false;
    ProgressDialog mProgress;
    Handler mHandler;
    private String tmpResponse = "";
    private int currentFragmentId = -1;
    private LatLng nextCabDestination = null;
    private Marker nextStopMarker;

    /*
     * Variable for fare calculation
     */
    private float billingRate = 10f;
    private int stateCounter = 0;
    private int refund = 0;
    private float distanceTravelled = 0f;
    private int duration = 0;
    private float costIncurred = 0f;


    /*
     * variable for user and driver rating
     */
    List<CoPassenger> coPassengers;

    /*
     * Boolean check variables
     */

    private boolean checkTrackRide = false;
    private boolean checkUpdateRide = false;
    private boolean checkRideDetails = false;

    /*
     * Wallet Variables
     */
    List<TransactionItem> creditList = new ArrayList<TransactionItem>();
    List<TransactionItem> debitList = new ArrayList<TransactionItem>();

    ObscuredSharedPreferences sharedPreferences;
    public static final String HOI_OBSCURED_PREFERENCES = "Hoi Cabs";

    /*
     * Methods for Activity State
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        cabDistanceAway = 10000;
        cabArrivalDuration = 10000;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = ObscuredSharedPreferences.getPrefs(this, "Hoi Cabs", Context.MODE_PRIVATE);
        String TITLES[] = {"Home", "Wallet", "Change Password", "Contact Us", "Feedback", "LogOut"};
        int ICONS[] = {R.drawable.ic_home, R.drawable.ic_wallet, R.drawable.ic_edit, R.drawable.ic_contact, R.drawable.ic_comp, R.drawable.ic_logout};

        String regID = prefs.getString("regId", "");

        if (regID != "")
            try {
                new HttpRequestTask((new JSONObject()).accumulate("regID", regID)).execute("http://www.hoi.co.in/api/updateregid");
            } catch (JSONException e) {
                Log.e("EXCEPTION", e.getMessage());
            }


        //Login details
        Bundle loginBundle = getIntent().getExtras();
        userDetails = new DetailUser(loginBundle.getString("name"), loginBundle.getString("phone"), loginBundle.getString("username"), loginBundle.getString("displaypic"));
        inRide = loginBundle.getBoolean("inride");
        awaitingRide = loginBundle.getBoolean("awaitingride");
        ratingPending = loginBundle.getBoolean("ratingpending");
        rideRequestId = loginBundle.getInt("genriderequestid");
        availableinr = (int)loginBundle.getDouble("availableinr");

        if (getIntent().hasExtra("authenticationHeader"))
            authenticationHeader = getIntent().getStringExtra("authenticationHeader");

        /*
         * Updating the user state if user already booked the ride
         */
        // Initializing Navigation Drawer
        try {
            mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView); // Assigning the RecyclerView Object to the xml View
            mRecyclerView.setHasFixedSize(true);                            // Letting the system know that the list objects are of fixed size
            mAdapter = new MyAdapter(TITLES, ICONS, userDetails.name, userDetails.phone, userDetails.displayPic, getApplicationContext());       // Creating the Adapter of MyAdapter class(which we are going to see in a bit)

            // And passing the titles,icons,header view name, header view email,
            // and header view profile picture
            mRecyclerView.setAdapter(mAdapter);                              // Setting the adapter to RecyclerView
            final GestureDetector mGestureDetector = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
            });
            mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                    View child = recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
                    if (child != null && mGestureDetector.onTouchEvent(motionEvent)) {
                        Drawer.closeDrawers();
                        try {
                            drawerItemSelectedAction(recyclerView.getChildPosition(child));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //Toast.makeText(MainActivity.this,"The Item Clicked is: "+recyclerView.getChildPosition(child),Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                }

                @Override
                public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                }

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                }
            });
            mLayoutManager = new LinearLayoutManager(this);                 // Creating a layout Manager
            mRecyclerView.setLayoutManager(mLayoutManager);                 // Setting the layout Manager
            Drawer = (DrawerLayout) findViewById(R.id.drawer_layout);        // Drawer object Assigned to the view
            mDrawerToggle = new ActionBarDrawerToggle(this, Drawer,
                    R.drawable.ic_drawer, R.string.drawer_open,
                    R.string.drawer_close) {
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mTitle);
                    invalidateOptionsMenu(); // creates call to
                    // onPrepareOptionsMenu()
                }

                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mDrawerTitle);
                    invalidateOptionsMenu(); // creates call to
                    // onPrepareOptionsMenu()
                }
            };
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            Drawer.setDrawerListener(mDrawerToggle); // Drawer Listener set to the Drawer toggle
            mDrawerToggle.syncState();               // Finally we set the drawer toggle sync State

        } catch (Exception e) {
            Log.e("Exception", "Activity On Create Failed: Navigation Drawer" + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Activity Creation failed", Toast.LENGTH_SHORT).show();
            restartApp();
        }
        try {
            //request data from server to setup views
            if (inRide) {
                //User is inside cab


            } else if (awaitingRide) {
                //User is waiting for cab
                openRide = prefs.getBoolean("openride", false);
                if(!openRide){
                    if (!(isConnected() && getRideDetails())) {
                        Toast.makeText(getApplicationContext(), "Connection to server failed", Toast.LENGTH_LONG).show();
                        restartApp();
                    }
                }

            } else if (ratingPending) {
                //User has to rate driver

            } else {
                //User has to book cab..... do nothing
            }
            setMapFragment(true);
            drawerItemSelectedAction(defaultView);
        } catch (Exception e) {
            Log.e("Exception", "Activity On Create Failed, Connection Error " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Activity Creation failed", Toast.LENGTH_SHORT).show();
            restartApp();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
    }

    /*
     * Methods for create user-state dependent view
     */

    private void setStateInRide() throws ExecutionException, InterruptedException {
        if (mGoogleMap != null)
            mGoogleMap.clear();

        setup = false;

        //boolean variables for ride state
        awaitingRide = false;
        inRide = true;
        ratingPending = false;

        mGoogleMap.setMyLocationEnabled(true);
        if (currentLocationCoordinate != null) {
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
            mGoogleMap.animateCamera(yourLocation);
        }

        displayFragment = new FragmentMain();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putBoolean("awaitingride", awaitingRide);
        args.putBoolean("inride", inRide);
        args.putBoolean("ratingpending", ratingPending);

        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame, displayFragment).commit();
        fm.executePendingTransactions();
        setTitle("In Ride");


        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Code to execute in other thread
                    Boolean check = false;
                    if(isConnected() && getCopassengerInfo())
                        setup = true;

                    //Code to execute in main thread
                    mHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mProgress.dismiss();
                            if (setup) {
                                ((FragmentMain) displayFragment).setCopassengerLayout(coPassengers.size());
                            }
                            else{
                                restartApp();
                            }
                        }
                    };
                    mHandler.postDelayed(myRunnable, 2000);
                    Log.d("CODE", "Setting up In-Ride");

                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                    mProgress.dismiss();
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "StateSetup Failed", Toast.LENGTH_SHORT).show();
                    restartApp();
                }

            }
        }).start();
    }

    private void setStateBookRide() {
        if (mGoogleMap != null) {
            mGoogleMap.clear();
        }

        //user state boolean variables
        inRide = false;
        awaitingRide = false;
        ratingPending = false;

        cabNearCheck = true;
        setup = false;

        if (currentLocationCoordinate != null) {
            sAddress = getAddress(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude);
            IconGenerator iconFactory = new IconGenerator(this);
            iconFactory.setColor(Color.GREEN);
            sourceMarker = addIcon(iconFactory, 1, new LatLng(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude));
            sourceMarker.isDraggable();

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
            mGoogleMap.animateCamera(yourLocation);
        } else {
            if (isGPSactive()) {
                //todo search for location
                setMapFragment(false);
                if (currentLocationCoordinate != null) {
                    sAddress = getAddress(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude);
                    IconGenerator iconFactory = new IconGenerator(this);
                    iconFactory.setColor(Color.GREEN);
                    sourceMarker = addIcon(iconFactory, 1, new LatLng(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude));
                    sourceMarker.isDraggable();

                    CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
                    mGoogleMap.animateCamera(yourLocation);
                } else {
                    restartApp();
                }
            } else
                restartApp();
        }


        displayFragment = new FragmentMain();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putBoolean("awaitingride", awaitingRide);
        args.putBoolean("inride", inRide);
        args.putBoolean("ratingpending", ratingPending);
        args.putString("pickupaddress", sAddress[0] + ", " + sAddress[1]);

        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame, displayFragment).commit();
        fm.executePendingTransactions();

        setTitle("Book Ride");

        mGoogleMap.setMyLocationEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Code to execute in other thread
                    String res = new HttpRequestTask().execute("http://www.hoi.co.in/api/checkbalance").get();
                    availableinr = (new JSONObject(res)).getInt("balance");
                    //Code to execute in main thread
                    Log.d("CODE", "Setting up Book-Ride");
                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                }
            }
        }).start();

        setup = true;
    }

    private void setStateAwaitingRide() {

        mGoogleMap.setOnMarkerDragListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setMyLocationEnabled(false);

        //user state boolean variables
        awaitingRide = true;
        inRide = false;
        ratingPending = false;

        setup = false;
        if (sourceMarker != null) sourceMarker.setDraggable(false);
        if (destinationMarker != null) destinationMarker.setDraggable(false);


        displayFragment = new FragmentMain();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putBoolean("awaitingride", awaitingRide);
        args.putBoolean("inride", inRide);
        args.putBoolean("ratingpending", ratingPending);
        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame, displayFragment).commit();
        fm.executePendingTransactions();
        setTitle("Awaiting Ride");

        try {
            if (!openRide)
                ((FragmentMain) displayFragment).setDriverDetails();
            initializeTimerTask();
            startTimer();
        } catch (Exception e) {
            restartApp();
        }
    }

    public void setStateRatingPending() throws ExecutionException, InterruptedException {
        mGoogleMap.clear();
        inRide = false;
        awaitingRide = false;
        ratingPending = true;
        setup = false;
        displayFragment = new FragmentMain();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putBoolean("awaitingride", awaitingRide);
        args.putBoolean("inride", inRide);
        args.putBoolean("ratingpending", ratingPending);

        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame, displayFragment).commit();
        fm.executePendingTransactions();

        setTitle("Summary");
        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Code to execute in other thread
                    if (!isConnected()) {
                        Toast.makeText(getApplicationContext(), "Error : No Connection", Toast.LENGTH_SHORT).show();
                        restartApp();
                    }
                    String res = (new HttpRequestTask()).execute(BASE_SERVER_URL + "closeride/" + rideRequestId).get(300000, TimeUnit.MILLISECONDS);
                    final JSONObject rideSummary = new JSONObject(res);
                    //Code to execute in main thread
                    mHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mProgress.dismiss();
                            ((FragmentMain) displayFragment).setRideSummary(rideSummary);
                        }
                    };
                    mHandler.postDelayed(myRunnable, 2000);
                    Log.d("CODE", "Setting up rating pending");
                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                    mProgress.dismiss();
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Setup failed", Toast.LENGTH_SHORT).show();
                    restartApp();
                }
            }
        }).start();
    }

    private void drawerItemSelectedAction(int childPosition) throws InterruptedException, ExecutionException, JSONException {

        if (!(childPosition == 3 || childPosition == 5 || childPosition == 6) && (currentFragmentId == childPosition))
        {
            Drawer.closeDrawers();
            return;
        }
        currentFragmentId = childPosition;
        if (sourceMarker != null) sourceMarker.remove();
        if (destinationMarker != null) destinationMarker.remove();

        switch (childPosition) {
            case 0: //Todo show the user profile
                break;
            case 1: //Todo check for connectivity for Google map api
                Drawer.closeDrawers();
                if (inRide)
                    setStateInRide();
                else if (awaitingRide)
                    setStateAwaitingRide();
                else if (ratingPending)
                    setStateRatingPending();
                else
                    setStateBookRide();
                break;
            case 2:
                Drawer.closeDrawers();
                setUpWallet();
                break;
            case 3:
                Drawer.closeDrawers();
                createChangePassDialog();
                break; //Todo change password dialog
            case 4:
                Drawer.closeDrawers();
                setUpContact();
                break;
            case 5:
                Drawer.closeDrawers();
                createFeedbackDialog();
                break;
            case 6:
                Drawer.closeDrawers();
                applicationLogout();
                break;
        }
    }

    /*
     * Miscelleaneous Methods
     */

    private void restartApp() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
        MainActivity.this.finish();
    }

    private boolean isGPSactive() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /*
     * Methods for button press action
     */

    public void requestBookRide(View view) throws ExecutionException, InterruptedException {
        int minFare = BASECHARGE;
        int maxFare = BASECHARGE;



        if (sourceMarker != null && destinationMarker != null) {
            final Double sLatitude = sourceMarker.getPosition().latitude;
            final Double sLongitude = sourceMarker.getPosition().longitude;
            final Double dLatitude = destinationMarker.getPosition().latitude;
            final Double dLongitude = destinationMarker.getPosition().longitude;
            float distancesource = calculateDistance(sLatitude, sLongitude, 28.6132, 77.2295);
            float distancedest = calculateDistance(dLatitude, dLongitude, 28.6132, 77.2295);

            if(distancesource > 50.0 || distancedest > 50.0) {
                createAlertDialog("Sorry Request Failed", "Atleast one of pickup/drop location is not serviced by us currently");
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            float distanceTobe = calculateDistance(sLatitude, sLongitude, dLatitude, dLongitude);
            if (distanceTobe < 0.5f) {
                createAlertDialog("Location Nearby", "Please set drop atleast half km away!");
                return;
            }
            int durationTobe = (int) calculateTime(sLatitude, sLongitude, dLatitude, dLongitude);

            int tmp = (int) Math.max(Math.ceil(6 * distanceTobe
                    + 0.5 * durationTobe), minFare);
            if (tmp > availableinr) {
                createAlertDialog("Insufficient Balance!", "Your balance is \u20B9" + availableinr);
                return;
            }

            minFare = (int) Math.max(Math.ceil(4 * distanceTobe
                    + 0.5 * durationTobe), minFare);
            maxFare = (int) Math.max(Math.ceil(10 * distanceTobe
                    + 0.5 * durationTobe), maxFare);
            // Get the layout inflater
            LayoutInflater inflater = this.getLayoutInflater();
            view = inflater.inflate(R.layout.dialog_confirm, null);
            // Add the buttons
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            setup = false;
                            if (!isConnected()) {
                                Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_LONG).show();
                                return;
                            }
                            mProgress = ProgressDialog.show(MainActivity.this, "",
                                    "Please Wait..", true);
                            mProgress.setCancelable(false);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    //Code to execute in other thread
                                    JSONObject jObject = new JSONObject();
                                    cabBookingDetails = new CabBookingDetails(sLatitude, sLongitude, sAddress[0], sAddress[1], dLatitude,
                                            dLongitude, dAddress[0], dAddress[1]);

                                    CabBookingTask bookCab = new CabBookingTask();
                                    try {
                                        String res = bookCab.execute("http://www.hoi.co.in/api/createriderequest").get(120000, TimeUnit.MILLISECONDS);
                                        jObject = new JSONObject(res);

                                        if (jObject.getDouble("billingrate") > 0.0) {
                                            currentRideDetails = new DetailCurrentRide(bookCab.getCabBookingData(), jObject);
                                            carDetails = new DetailCar(jObject.getJSONObject("carinfo"));
                                            driverDetails = new DetailDriver(jObject);
                                            rideRequestId = jObject.getInt("riderequestid");
                                            cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"), jObject.getDouble("ridelastlong"));
                                            rideBooked = true;
                                            setup = true;
                                            Log.d("Billing Rate", "" + jObject.getDouble("billingrate"));
                                        } else {
                                            tmpResponse = jObject.getString("drivername");
                                            rideRequestId = jObject.getInt("riderequestid");
                                        }
                                    } catch (Exception e) {
                                        Log.e("Exception", e.getMessage());
                                    }

                                    //Code to execute in main thread
                                    mHandler = new Handler(getApplicationContext().getMainLooper());
                                    Runnable myRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            mProgress.dismiss();
                                            if (setup) {
                                                if (sourceMarker != null) sourceMarker.remove();
                                                if (destinationMarker != null)
                                                    destinationMarker.remove();

                                                setStateAwaitingRide();
                                            } else {
                                                if (tmpResponse != "") {
                                                    createOpenRideDialog(1);
                                                    tmpResponse = "";
                                                } else {
                                                    createAlertDialog("Connection Problem", "Couldn't connect to server!");
                                                }
                                            }
                                        }
                                    };
                                    mHandler.postDelayed(myRunnable, 2000);
                                }
                            }).start();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            // Set other dialog properties
            // Create the AlertDialog
            AlertDialog dialog = builder.create();


            TextView source = (TextView) view.findViewById(R.id.source);
            source.setText(sAddress[0] + ", " + sAddress[1]);
            TextView destination = (TextView) view.findViewById(R.id.destination);
            destination.setText(dAddress[0] + ", " + dAddress[1]);
            TextView fare = (TextView) view.findViewById(R.id.fare);
            fare.setText("\u20B9" + minFare + " - \u20B9" + maxFare);
            dialog.show();

        } else {

            Toast.makeText(getApplicationContext(), "PickUp or Drop Location not set!!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void requestCancelRide(View view) throws ExecutionException, InterruptedException, JSONException, ParseException {


        float cost = BASECHARGE;

        if (openRide) {
            createOpenRideDialog(2);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date currentTime = sdf.parse(sdf.format(new Date()));
        if (currentRideDetails.bookingTime.contains("/"))
            sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        Date bookingTime = sdf.parse((currentRideDetails.bookingTime));
        long difference = currentTime.getTime() - bookingTime.getTime();
        int min = (int) (difference) / (1000 * 60);


        float distanceTobe = calculateDistance(currentRideDetails.sourceCoordinates.latitude,
                currentRideDetails.sourceCoordinates.longitude, cabLatestCoordinate.latitude,
                cabLatestCoordinate.longitude);
        int durationTobe = (int) calculateTime(currentRideDetails.sourceCoordinates.latitude,
                currentRideDetails.sourceCoordinates.longitude, cabLatestCoordinate.latitude,
                cabLatestCoordinate.longitude);

        if (min < 5.0)
            cost = 0;
        else if (durationTobe < 10)
            cost = 100;
        else
            cost = 30;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_cancel_ride, null);
        // Add the buttons

        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // todo when user cancels ride

                        dialog.dismiss();
                        setup = false;
                        mProgress = ProgressDialog.show(MainActivity.this, "",
                                "Please Wait..", true);
                        mProgress.setCancelable(false);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //Code to execute in other thread
                                try {
                                    HttpRequestTask cancelRide = new HttpRequestTask();
                                    String cancelRideResult = cancelRide.execute("http://www.hoi.co.in/api/cancelride/" + rideRequestId).get();
                                    JSONObject jsonObject = new JSONObject(cancelRideResult);
                                    tmpResponse = "" + jsonObject.getInt("refund");
                                    //if (refAmount <= -30)
                                    //balance += refAmount;
                                    setup = true;
                                } catch (Exception e) {
                                    Log.e("Exception", e.getMessage());
                                }

                                //Code to execute in main thread
                                mHandler = new Handler(getApplicationContext().getMainLooper());
                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgress.dismiss();
                                        if (setup) {
                                            stoptimertask();
                                            setStateBookRide();
                                            int cancelCharge = Integer.parseInt(tmpResponse);
                                            createAlertDialog("Ride Cancelled", "\u20B9 " + (-1 * cancelCharge) + " has been deducted from your account.");
                                            tmpResponse = "";
                                            setup = false;
                                        } else {
                                            restartApp();
                                        }
                                    }
                                };
                                mHandler.postDelayed(myRunnable, 2000);
                            }
                        }).start();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog

        AlertDialog dialog = builder.create();
        final TextView tvCost = (TextView) dView.findViewById(R.id.cancelcost);
        tvCost.setText("\u20B9" + cost);
        dialog.show();
    }

    public void sendMessageSource(View view) {

        if (currentLocationCoordinate == null)
            Toast.makeText(getApplicationContext(), "Please wait! Searching current location",
                    Toast.LENGTH_LONG).show();
        else {
            //if (sourceMarker != null) sourceMarker.remove();
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            intent.putExtra("Location", "S");
            intent.putExtra("CurrentLatitude", Double.toString(currentLocationCoordinate.latitude));
            intent.putExtra("CurrentLongitude", Double.toString(currentLocationCoordinate.longitude));
            String[] str = {"Home", "Office", "Favorite1", "Favorite2", "Favorite3"};
            for (int i = 0; i < 5; i++) {
                if (prefs.contains(str[i])) {
                    try {
                        JSONObject jsonObject = new JSONObject(prefs.getString(str[i], null));
                        if (jsonObject != null) intent.putExtra(str[i], jsonObject.toString());
                    } catch (JSONException e) {
                    }
                }
            }
            startActivityForResult(intent, 2);
        }
    }

    public void sendMessageSource() {

        if (currentLocationCoordinate == null)
            Toast.makeText(getApplicationContext(), "Please wait! Searching current location",
                    Toast.LENGTH_LONG).show();
        else {
            //if (sourceMarker != null) sourceMarker.remove();
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            intent.putExtra("Location", "S");
            intent.putExtra("CurrentLatitude", Double.toString(currentLocationCoordinate.latitude));
            intent.putExtra("CurrentLongitude", Double.toString(currentLocationCoordinate.longitude));
            String[] str = {"Home", "Office", "Favorite1", "Favorite2", "Favorite3"};
            for (int i = 0; i < 5; i++) {
                if (prefs.contains(str[i])) {
                    try {
                        JSONObject jsonObject = new JSONObject(prefs.getString(str[i], null));
                        if (jsonObject != null) intent.putExtra(str[i], jsonObject.toString());
                    } catch (JSONException e) {
                    }
                }
            }
            startActivityForResult(intent, 2);
        }
    }

    public void sendMessageDestination(View view) {

        if (currentLocationCoordinate == null)
            Toast.makeText(getApplicationContext(), "Please wait!! Searching current location",
                    Toast.LENGTH_LONG).show();
        else {
            //if (destinationMarker != null) destinationMarker.remove();
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            intent.putExtra("Location", "D");
            intent.putExtra("CurrentLatitude", Double.toString(currentLocationCoordinate.latitude));
            intent.putExtra("CurrentLongitude", Double.toString(currentLocationCoordinate.longitude));
            String[] str = {"Home", "Office", "Favorite1", "Favorite2", "Favorite3"};
            for (int i = 0; i < 5; i++) {
                if (prefs.contains(str[i])) {
                    try {
                        JSONObject jsonObject = new JSONObject(prefs.getString(str[i], null));
                        if (jsonObject != null) intent.putExtra(str[i], jsonObject.toString());
                    } catch (JSONException e) {
                    }
                }
            }
            startActivityForResult(intent, 3);
        }

    }

    public void sendMessageDestination() {

        if (currentLocationCoordinate == null)
            Toast.makeText(getApplicationContext(), "Please wait!! Searching current location",
                    Toast.LENGTH_LONG).show();
        else {
            //if (destinationMarker != null) destinationMarker.remove();
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            intent.putExtra("Location", "D");
            intent.putExtra("CurrentLatitude", Double.toString(currentLocationCoordinate.latitude));
            intent.putExtra("CurrentLongitude", Double.toString(currentLocationCoordinate.longitude));
            String[] str = {"Home", "Office", "Favorite1", "Favorite2", "Favorite3"};
            for (int i = 0; i < 5; i++) {
                if (prefs.contains(str[i])) {
                    try {
                        JSONObject jsonObject = new JSONObject(prefs.getString(str[i], null));
                        if (jsonObject != null) intent.putExtra(str[i], jsonObject.toString());
                    } catch (JSONException e) {
                    }
                }
            }
            startActivityForResult(intent, 3);
        }

    }

    public void saveSource(View view) {

        if (sourceMarker == null) {
            Toast.makeText(getApplicationContext(), "No PickUp Location Found", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Double lat = sourceMarker.getPosition().latitude;
        final Double lon = sourceMarker.getPosition().longitude;
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dview = inflater.inflate(R.layout.dialog_save_favorite, null);
        final Spinner addtype = (Spinner) dview.findViewById(R.id.address_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.address_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addtype.setAdapter(adapter);
        TextView tvadd = (TextView) dview.findViewById(R.id.address);
        tvadd.setText(sAddress[0] + ", " + sAddress[1]);
        // Add the buttons
        builder.setView(dview)
                // Add action buttons
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        try {
                            JSONObject jObject = new JSONObject();
                            jObject.put("latitude", lat);
                            jObject.put("longitude", lon);
                            jObject.put("address0", sAddress[0]);
                            jObject.put("address1", sAddress[1]);
                            if (prefs.contains(addtype.getSelectedItem().toString())) {
                                prefs.edit().remove(addtype.getSelectedItem().toString());
                            }
                            prefs.edit().putString(addtype.getSelectedItem().toString(), jObject.toString()).commit();
                        } catch (JSONException e) {
                            Log.e("Exception", e.getMessage());
                            Toast.makeText(getApplicationContext(), "Location not saved", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void saveDestination(View view) {
        if (destinationMarker == null) {
            Toast.makeText(getApplicationContext(), "No Drop Location Found", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dview = inflater.inflate(R.layout.dialog_save_favorite, null);
        final Spinner addtype = (Spinner) dview.findViewById(R.id.address_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.address_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addtype.setAdapter(adapter);
        TextView tvadd = (TextView) dview.findViewById(R.id.address);
        tvadd.setText(dAddress[0] + ", " + dAddress[1]);
        // Add the buttons
        builder.setView(dview)
                // Add action buttons
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        Double lat = destinationMarker.getPosition().latitude;
                        Double lon = destinationMarker.getPosition().longitude;
                        try {
                            JSONObject jObject = new JSONObject();
                            jObject.put("latitude", lat);
                            jObject.put("longitude", lon);
                            jObject.put("address0", dAddress[0]);
                            jObject.put("address1", dAddress[1]);
                            if (prefs.contains(addtype.getSelectedItem().toString())) {
                                prefs.edit().remove(addtype.getSelectedItem().toString());
                            }
                            prefs.edit().putString(addtype.getSelectedItem().toString(), jObject.toString()).commit();


                        } catch (JSONException e) {
                            Log.e("Exception", e.getMessage());
                            Toast.makeText(getApplicationContext(), "Location not saved", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    /*
     * Methods for Map Fragment
     */

    private void setMapFragment(boolean check) {
        //Todo checks
        //if(checkForConnectivity()) createAlertDialog("");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (check) {
            SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mGoogleMap = fragment.getMap();
            mGoogleMap.setOnMarkerDragListener(this);
            mGoogleMap.setOnMapClickListener(this);
            mGoogleMap.setMyLocationEnabled(true);
        }

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Getting the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Getting Current Location
        Location location = getLastKnownLocation(locationManager);

        try {
            if (location != null) {
                currentLocationCoordinate = new LatLng(location.getLatitude(), location.getLongitude());
                onLocationChanged(location);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        locationManager.requestLocationUpdates(provider, 30000, 10, this);
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_CANCELED) {
            Bundle bundle = data.getExtras();

            Double latitude = Double.parseDouble(bundle.getString("latitude"));
            Double longitude = Double.parseDouble(bundle.getString("longitude"));
            String address0 = bundle.getString("address0");
            String address1 = bundle.getString("address1");
            IconGenerator iconFactory = new IconGenerator(this);

            // check if the request code is same as what is passed  here it is 2
            if (requestCode == 2) {
                if (sourceMarker != null) sourceMarker.remove();
                iconFactory.setColor(Color.GREEN);
                sourceMarker = addIcon(iconFactory, 1, new LatLng(latitude, longitude));
                sourceMarker.isDraggable();
                FragmentMain disp = (FragmentMain) displayFragment;
                sAddress[0] = address0;
                sAddress[1] = address1;
                disp.setTextSource(sAddress[0] + ", " + sAddress[1]);
            } else if (requestCode == 3) {
                if (destinationMarker != null) destinationMarker.remove();
                iconFactory.setColor(Color.RED);
                destinationMarker = addIcon(iconFactory, 2, new LatLng(latitude, longitude));
                destinationMarker.isDraggable();

                FragmentMain disp = (FragmentMain) displayFragment;
                dAddress[0] = address0;
                dAddress[1] = address1;
                disp.setTextDestination(dAddress[0] + ", " + dAddress[1]);
            }

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16);
            mGoogleMap.animateCamera(yourLocation);
        }
    }

    private boolean getRideDetails() {
        HttpRequestTask rideRequestInfo = new HttpRequestTask();
        try {
            Log.d("CODE_FLOW", "CAB DETAILS_REQESTED");
            String res = rideRequestInfo.execute("http://www.hoi.co.in/api/riderequestinfo/" + rideRequestId).get(180000, TimeUnit.MILLISECONDS);
            JSONObject jObject, genRideRequestInfo, rideResponceInfo;
            jObject = new JSONObject(res);
            genRideRequestInfo = jObject.getJSONObject("grri");
            rideResponceInfo = jObject.getJSONObject("rrd");
            currentRideDetails = new DetailCurrentRide(genRideRequestInfo);
            carDetails = new DetailCar(rideResponceInfo.getJSONObject("carinfo"));
            driverDetails = new DetailDriver(rideResponceInfo);
            cabLatestCoordinate = new LatLng(rideResponceInfo.getDouble("ridelastlong"), rideResponceInfo.getDouble("ridelastlat"));
            Log.d("CODE_FLOW", "CAB DETAILS_RECEIVED");
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
            return false;
        }
        return true;
    }

    private boolean getCopassengerInfo() {
        HttpRequestTask coPassengerInfoTask = new HttpRequestTask();

        try {
            Log.d("CODE_FLOW", "REQUEST FOR CO PASSENGER INFO");
            String res = coPassengerInfoTask.execute(BASE_SERVER_URL + "copassengerinfo/" + rideRequestId).get(300000, TimeUnit.MILLISECONDS);
            JSONObject jObject;
            jObject = new JSONObject(res);
            Log.d("DATA_CO_PASSENGER_INFO", res);
            coPassengers = new ArrayList<CoPassenger>();

            if (jObject.getInt("riderequestid1") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("coPassenger1name"), jObject.getString("coPassenger1picURL"),
                        jObject.getInt("riderequestid1")));

            if (jObject.getInt("riderequestid2") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("coPassenger2name"), jObject.getString("coPassenger2picURL"),
                        jObject.getInt("riderequestid2")));

            if (jObject.getInt("riderequestid3") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("coPassenger3name"), jObject.getString("coPassenger3picURL"),
                        jObject.getInt("riderequestid3")));

            if (jObject.getInt("riderequestid4") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("coPassenger4name"), jObject.getString("coPassenger4picURL"),
                        jObject.getInt("riderequestid4")));
            Log.d("CODE_FLOW", "CO PASSENGERS DATA RECEIVED");
        } catch (Exception e) {
            Log.d("Exception", e.toString());
            return false;
        }
        Log.d("CODE_FLOW", "RECEIVED CO PASSENGER INFO");
        return true;
    }


    public void viewNextLocation(View view) {
        if (nextCabDestination != null) {
            if (currentLocationCoordinate != null) {
                try {
                    findDirections(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,//28.612936, 77.229483,GMapV2Direction.MODE_DRIVING);
                            nextCabDestination.latitude, nextCabDestination.longitude, GMapV2Direction.MODE_DRIVING);
                    IconGenerator iconFactory = new IconGenerator(this);
                    iconFactory.setColor(Color.GREEN);
                    nextStopMarker = addIcon(iconFactory, 3, nextCabDestination);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Problem with google services", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Current Location not found", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No next cab destination", Toast.LENGTH_LONG).show();
        }

    }

    public void submitDriverRating(View view) {
        int rating = ((FragmentMain) displayFragment).getDriverRating();
        if (rating != 0)
            new HttpRequestTask().execute(BASE_SERVER_URL + "ratedriver/" + rideRequestId + "/" + rating);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                setStateBookRide();
                Log.d("CODE", "Setting up wallet");
            }
        }, 1000);

    }

    /*
     * Navigation Drawer related funtions
     * dataList.add(new DrawerItem("Settings", R.drawable.ic_action_settings));
     */

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

 /*
  * Functions to get the location of the user and set marker for pickup and drop location
  */

    private Location getLastKnownLocation(LocationManager mLocationManager) {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);

            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }

    private Marker addIcon(IconGenerator iconFactory, int type, LatLng position) {
        MarkerOptions markerOptions;
        if (type == 1) {
            markerOptions = new MarkerOptions().
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.pickup)).
                    position(position).
                    anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()).draggable(true);
        } else if (type == 2) {
            markerOptions = new MarkerOptions().
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.drop)).
                    position(position).
                    anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()).draggable(true);
        } else {
            markerOptions = new MarkerOptions().
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).
                    position(position).
                    anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()).draggable(true);
        }
        return mGoogleMap.addMarker(markerOptions);
    }

    @Override
    public void onLocationChanged(Location location) {

        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        if (inRide && setup) {
            float segmentDistance = calculateDistance(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                    latitude, longitude);
            int segmentDuration = (int) calculateTime(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                    latitude, longitude);

            distanceTravelled += segmentDistance;
            duration += segmentDuration;
            costIncurred += billingRate * segmentDistance + 0.5f * segmentDuration;

            // Creating a LatLng object for the current location
            currentLocationCoordinate = new LatLng(latitude, longitude);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
            mGoogleMap.animateCamera(yourLocation);

            if (checkUpdateRide) {
                return;
            } else {

                UpdateRideTask updateRide = new UpdateRideTask();
                if (isConnected()){
                    checkUpdateRide = true;
                    updateRide.execute("http://www.hoi.co.in/api/update/" + rideRequestId);
                }
            }
        }
    }



    /*
     * Awaiting ride user state related methods
     */

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, the TimerTask will run every half minute
        timer.schedule(timerTask, 1000, 100000); //
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (!checkTrackRide) {
                            checkTrackRide = true;
                            trackRide();
                        }

                    }
                });
            }
        };
    }

    public void trackRideAction(View view) {
        trackRide();
    }

    public void trackRide() {
        if (awaitingRide == false) return;
        if (mProgress != null) mProgress.dismiss();
        new Thread(new Runnable() {
            @Override
            public void run() {
                setup = false;
                tmpResponse = "";
                //Code to execute in other thread
                HttpRequestTask trackRide = new HttpRequestTask();
                try {
                    String res = trackRide.execute("http://www.hoi.co.in/api/track/" + rideRequestId).get();
                    JSONObject jObject = new JSONObject(res);
                    billingRate = jObject.getInt("billingrate");
                    if (billingRate == 0) {
                        openRide = true;
                        prefs.edit().putBoolean("openride", openRide).commit();
                    } else if (billingRate > 0) {
                        cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"), jObject.getDouble("ridelastlong"));

                        LatLng source = new LatLng(currentRideDetails.sourceCoordinates.latitude, currentRideDetails.sourceCoordinates.longitude);
                        double time = calculateTime(source.latitude, source.longitude, cabLatestCoordinate.latitude, cabLatestCoordinate.longitude);
                        if (time < 5.0 && cabNearCheck) {
                            cabNearCheck = false;
                            tmpResponse = "cabisnear";
                        }
                        if (jObject.getBoolean("rideunderway") == true) {
                            nextCabDestination = new LatLng(jObject.getDouble("nextdestlat"), jObject.getDouble("nextdestlong"));
                            awaitingRide = false;
                            inRide = true;
                        }
                        if (jObject.getBoolean("ridecancelled") == true) {
                            awaitingRide = false;
                        }
                        setup = true;
                    }
                } catch (Exception e) {
                    Log.e("Exception", e.getMessage());
                }
                //Code to execute in main thread
                mHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (setup) {
                            if (tmpResponse.equals("cabisnear")) {
                                createAlertDialog("Cab is here!", "Your Cab is 5 minutes away..");
                                createNotification("Alert", "Cab is 5 minutes away");
                            }
                            if (cabMarker != null) cabMarker.remove();

                            mGoogleMap.clear();
                            IconGenerator iconFactory = new IconGenerator(MainActivity.this);
                            iconFactory.setColor(Color.YELLOW);
                            cabMarker = addIcon(iconFactory, 3, cabLatestCoordinate);
                            cabMarker.setDraggable(false);

                            if (currentRideDetails.sourceCoordinates != null) {
                                sourceMarker = addIcon(iconFactory, 1, new LatLng(currentRideDetails.sourceCoordinates.latitude, currentRideDetails.sourceCoordinates.longitude));
                                sourceMarker.setDraggable(false);
                            }

                            if (currentRideDetails.destinationCoordinates != null) {
                                destinationMarker = addIcon(iconFactory, 2, new LatLng(currentRideDetails.destinationCoordinates.latitude, currentRideDetails.destinationCoordinates.longitude));
                                destinationMarker.setDraggable(false);
                            }

                            //destinationMarker = addIcon(iconFactory, 2, new LatLng(cabBookingDetails.destination_latitude, cabBookingDetails.destination_longitude));
                            //destinationMarker.setDraggable(false);

                            CameraUpdate cabLocation = CameraUpdateFactory.newLatLngZoom(cabLatestCoordinate, 15);
                            mGoogleMap.animateCamera(cabLocation);
                            try {
                                if (inRide) {
                                    stoptimertask();
                                    setStateInRide();
                                } else if (!awaitingRide) {
                                    stoptimertask();
                                    setStateBookRide();
                                }
                            } catch (Exception e) {
                                restartApp();
                            }
                        }
                        checkTrackRide = false;
                    }
                };
                mHandler.post(myRunnable);

                Log.d("CODE", "Track Ride");
            }
        }).start();
    }

    /*
     * Booking ride related methods
     */
    private float calculateDistance(Double slat, Double slong, Double dlat, Double dlong) {
        float distance = 0.0f;
        distance = (float) (142.6 * Math.sqrt(Math.pow(Math.abs(slat - dlat), 2) + Math.pow(Math.abs(slong - dlong), 2)));
        return distance;
    }

    private float calculateTime(Double slat, Double slong, Double dlat, Double dlong) {
        float time = 0.0f;
        time = (float) (225.73 * Math.sqrt(Math.pow(Math.abs(slat - dlat), 2) + Math.pow(Math.abs(slong - dlong), 2)));
        return time;
    }

    private void applicationLogout() {
        prefs.edit().remove("username").commit();
        prefs.edit().remove("password").commit();
        super.finish();
    }

    /*
     * Wallet Fragment
     */

    private void setUpWallet() {
        displayFragment = new FragmentWallet();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putString("authenticationheader", authenticationHeader);
        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame, displayFragment).commit();
        setTitle("Wallet");

        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Code to execute in other thread
                    setup = false;
                    try {
                        String balance = new HttpRequestTask().execute("http://www.hoi.co.in/api/checkbalance").get();
                        String credits = new HttpRequestTask().execute("http://www.hoi.co.in/api/credits").get();
                        String debits = new HttpRequestTask().execute("http://www.hoi.co.in/api/debits").get();
                        availableinr = (new JSONObject(balance)).getInt("balance");
                        JSONArray jsonArray = new JSONArray(credits);
                        creditList = new ArrayList<TransactionItem>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            TransactionItem tmp = new TransactionItem((JSONObject) jsonArray.get(i));
                            creditList.add(tmp);
                        }

                        jsonArray = new JSONArray(debits);
                        debitList = new ArrayList<TransactionItem>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            TransactionItem tmp = new TransactionItem((JSONObject) jsonArray.get(i));
                            debitList.add(tmp);
                        }
                        setup = true;

                    } catch (Exception e) {
                        Log.e("Exception", e.getMessage());
                    }

                    //Code to execute in main thread
                    mHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (setup) {
                                ((FragmentWallet) displayFragment).setCredits(creditList);
                                ((FragmentWallet) displayFragment).setDebits(debitList);
                                ((FragmentWallet) displayFragment).setBalance(availableinr);

                            } else {
                                createAlertDialog("Connection Problem", "Couldn't load credits and debits");
                            }
                        }
                    };
                    mHandler.postDelayed(myRunnable, 2000);
                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                }
                Log.d("CODE", "Setting up rating pending");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Code to execute in UI thread or main thread
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                mProgress.dismiss();
                            }
                        }, 2000);
                    }
                });
            }
        }).start();

    }
    /*
     * Contact Fragment
     */

    public void makecall(View view) {
        String uri = "tel:" + ((TextView) view.findViewById(R.id.driver_moblie)).getText();
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(uri));
        startActivity(intent);
    }

    public void makeemail(View view) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@hoi.co.in"});
        i.putExtra(Intent.EXTRA_SUBJECT, "reg: Hoi Cabs");

        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setUpContact() {
        Fragment displayFragment = new FragmentContact();
        FragmentManager fm = getSupportFragmentManager();

        fm.beginTransaction().replace(R.id.content_frame, displayFragment).commit();
        fm.executePendingTransactions();
        setTitle("Contact Us");
    }

    /*
     * Dialogs Methods
     */

    private void createChangePassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_changepass, null);
        // Add the buttons

        final EditText oldPass = (EditText) dView.findViewById(R.id.old_pass);
        final EditText newPass = (EditText) dView.findViewById(R.id.new_pass);
        final EditText confPass = (EditText) dView.findViewById(R.id.confirm_pass);

        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String res = "Password not changed";
                if (oldPass.getText().toString().isEmpty()) {
                    oldPass.setError("No Password Entered");
                } else if (oldPass.getText().toString().equals(prefs.getString("password", ""))) {
                    oldPass.setError("Password Mismatch with Old Password");
                } else if (newPass.getText().toString().isEmpty()) {
                    newPass.setError("No Password Entered");
                } else if (!newPass.getText().toString().equals(confPass.getText().toString())) {
                    confPass.setError("Password Mismatch");
                } else {
                    JSONObject data = new JSONObject();
                    try {

                        data.accumulate("password", newPass.getText().toString());
                        res = new HttpRequestTask(data).execute(BASE_SERVER_URL + "updatepassword/" + userDetails.phone).get();

                        Toast.makeText(MainActivity.this, res, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(intent);
                        MainActivity.this.finish();
                    } catch (Exception e) {
                        Log.e("EXCEPTION", e.getMessage());
                    }
                }
            }
        });
    }

    public void createOpenRideDialog(final int state) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_alert, null);
        // Add the buttons
        TextView msg = (TextView) dView.findViewById(R.id.alert_message);
        TextView alertTitle = (TextView) dView.findViewById(R.id.alertTitle);
        if (state == 1) {
            alertTitle.setText("No Ride Found");
            msg.setText("No ride available right now.\n Do you want to wait for next 30 mins?");
        } else if (state == 2) {
            alertTitle.setText("Open Ride");
            msg.setText("Do you want to close your request?");
        }
        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        if (state == 1) {
                            openRide = true;
                            prefs.edit().putBoolean("openride", openRide).commit();
                            setStateAwaitingRide();
                        } else if (state == 2) {
                            try {
                                String res = new HttpRequestTask().execute("http://www.hoi.co.in/api/cancelopenuser").get();
                                openRide = false;
                                prefs.edit().putBoolean("openride", openRide).commit();
                                setStateBookRide();
                                System.out.print(res);
                            } catch (Exception e) {
                                Log.e("Exception", e.getMessage());
                                setStateAwaitingRide();
                                Toast.makeText(getApplicationContext(), "Your Ride is still open!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        if (state == 2) {
                            openRide = true;
                            prefs.edit().putBoolean("openride", openRide).commit();
                            setStateAwaitingRide();
                        } else if (state == 1) {
                            try {
                                String res = new HttpRequestTask().execute("http://www.hoi.co.in/api/cancelopenuser").get();
                                openRide = false;
                                prefs.edit().putBoolean("openride", openRide).commit();
                                setStateBookRide();
                                System.out.print(res);
                            } catch (Exception e) {
                                Log.e("Exception", e.getMessage());
                                setStateAwaitingRide();
                                Toast.makeText(getApplicationContext(), "Your Ride is still open!", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void createAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_alert, null);
        // Add the buttons
        TextView msg = (TextView) dView.findViewById(R.id.alert_message);
        TextView alertTitle = (TextView) dView.findViewById(R.id.alertTitle);
        alertTitle.setText(title);
        msg.setText(message);
        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);

    }

    public void createNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this);
        builder.setSmallIcon(R.drawable.ic_logo);
        builder.setContentTitle(title);
        builder.setContentText(message);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void createRatingDialog(final int passenger) throws ExecutionException, InterruptedException {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_rating, null);
        final RatingBar rating = (RatingBar) dView.findViewById(R.id.rating_bar);
        // Add the buttons

        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        new HttpRequestTask().execute(BASE_SERVER_URL + "rateuser/" + rideRequestId + "/" + coPassengers.get(passenger).id
                                + "/" + rating.getRating());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        de.hdodenhof.circleimageview.CircleImageView pic = (de.hdodenhof.circleimageview.CircleImageView)
                dView.findViewById(R.id.co_passenger_dp);
        pic.setImageBitmap(coPassengers.get(passenger).pic);
        dialog.show();
    }

    public void createFeedbackDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_feedback, null);
        final EditText userComment = (EditText) dView.findViewById(R.id.feedback);

        // Add the buttons

        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        new HttpRequestTask(userComment.getText().toString()).execute("http://www.hoi.co.in/api/feedback");
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void createRiderReportDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_report_rider, null);
        final EditText userComment = (EditText) dView.findViewById(R.id.comment);

        // Add the buttons

        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        new HttpRequestTask(userComment.getText().toString()).execute("http://www.hoi.co.in/api/" + "/addnlrider/" + rideRequestId);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void createVoucherDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.voucher_dialog, null);

        final EditText voucherCode = (EditText) dView.findViewById(R.id.voucher);

        // Add the buttons
        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (voucherCode.getText().toString().isEmpty()) {
                    voucherCode.setError("No Code entered");
                } else {
                    dialog.dismiss();
                    mProgress = ProgressDialog.show(MainActivity.this, "",
                            "Please Wait..", true);
                    mProgress.setCancelable(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            setup = false;
                            tmpResponse = "";
                            try {
                                //Code to execute in other thread
                                String res = new HttpRequestTask().execute(BASE_SERVER_URL + "applyvoucher/" + voucherCode.getText()).get();
                                JSONObject jsonObject = new JSONObject(res);
                                if (jsonObject.getBoolean("accepted")) {
                                    setup = true;
                                    tmpResponse = jsonObject.getString("message");
                                    availableinr = jsonObject.getInt("availableinr");
                                } else {
                                    tmpResponse = jsonObject.getString("message");
                                }
                            } catch (Exception e) {
                                Log.e("Exception", e.getMessage());
                            }

                            //Code to execute in main thread
                            mHandler = new Handler(getApplicationContext().getMainLooper());
                            Runnable myRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (setup) {
                                        Toast.makeText(getApplicationContext(), tmpResponse, Toast.LENGTH_LONG).show();
                                        ((FragmentWallet) displayFragment).setBalance(availableinr);
                                    } else if (tmpResponse != "") {
                                        createAlertDialog("Sorry", tmpResponse);
                                    } else {
                                        createAlertDialog("Connection Problem", "Couldn't connect to server");
                                    }
                                }
                            };
                            mHandler.postDelayed(myRunnable, 1000);
                            Log.d("CODE", "Setting up rating pending");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Code to execute in UI thread or main thread
                                    Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        public void run() {
                                            mProgress.dismiss();
                                        }
                                    }, 1000);
                                }
                            });
                        }
                    }).start();
                }
            }
        });

    }

    public void showCabRates(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_cab_rates, null);
        builder.setView(dView)// Add action buttons
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
    }

    public void findDirections(double fromPositionDoubleLat, double fromPositionDoubleLong, double toPositionDoubleLat, double toPositionDoubleLong, String mode) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT, String.valueOf(fromPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG, String.valueOf(fromPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DESTINATION_LAT, String.valueOf(toPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.DESTINATION_LONG, String.valueOf(toPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, mode);

        GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask();
        asyncTask.execute(map);
    }

    public void handleGetDirectionsResult(ArrayList<LatLng> directionPoints) {
        Polyline newPolyline;
        GoogleMap mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        PolylineOptions rectLine = new PolylineOptions().width(3).color(Color.BLUE);
        for (int i = 0; i < directionPoints.size(); i++) {
            rectLine.add(directionPoints.get(i));
        }
        newPolyline = mMap.addPolyline(rectLine);
    }

    /*
     * Getter functions
     */

    public String getCarNumber() {
        return carDetails.regNumber;
    }

    public String getCarModel() {
        return carDetails.model;
    }

    public String getDriverPhone() {
        return driverDetails.phone;
    }

    public String getDriverName() {
        return driverDetails.name;
    }

    public Bitmap getDriverPic() {
        return driverDetails.driverPic;
    }
    /*
     * Miscellaneous Methods
     */

    public String getAuthHeader() {
        return authenticationHeader;
    }

    public Bitmap getPassengerBitmap(int i) {
        return coPassengers.get(i).pic;
    }
    /*
     * Represents an asynchronous cab booking task used to authenticate
     * the ride.
     */

    private class CabBookingTask extends AsyncTask<String, Void, String> {

        private HttpResponse response;
        private JSONObject cabBookingData;

        @Override
        protected String doInBackground(String... url) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            Encryptor encryptor = new Encryptor();
            String encrptedkey = "";

            try {
                encrptedkey = encryptor.getEncryptedKeyValue(currentDateandTime);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            try {

                HttpPost request = new HttpPost(url[0]);
                request.addHeader("Authorization", "Basic " + authenticationHeader);
                request.addHeader("androidkey", encrptedkey);
                request.addHeader("Content-Type", "application/json");

                System.out.println(authenticationHeader + " " + encrptedkey);
                String json = "";

                // 3. build jsonObject
                cabBookingData = new JSONObject();
                cabBookingData.accumulate("origin_latitude", Math.round(cabBookingDetails.origin_latitude * 10000.0) / 10000.0);
                cabBookingData.accumulate("origin_longitude", Math.round(cabBookingDetails.origin_longitude * 10000.0) / 10000.0);
                cabBookingData.accumulate("origin_address1", cabBookingDetails.origin_address1);
                cabBookingData.accumulate("origin_address2", cabBookingDetails.origin_address2);
                cabBookingData.accumulate("destination_latitude", Math.round(cabBookingDetails.destination_latitude * 10000.0) / 10000.0);
                cabBookingData.accumulate("destination_longitude", Math.round(cabBookingDetails.destination_longitude * 10000.0) / 10000.0);
                cabBookingData.accumulate("destination_address1", cabBookingDetails.destination_address1);
                cabBookingData.accumulate("destination_address2", cabBookingDetails.destination_address2);
                cabBookingData.accumulate("requestdatetime", currentDateandTime);

                // 4. convert JSONObject to JSON to String
                json = cabBookingData.toString();

                // 5. set json to StringEntity

                request.setEntity(new StringEntity(json));
                HttpClient httpclient = new DefaultHttpClient();
                try {
                    response = httpclient.execute(request);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            InputStream inputStream = null;
            String result = null;
            try {
                HttpEntity entity = response.getEntity();
                inputStream = entity.getContent();

                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                result = sb.toString();
            } catch (Exception e) {
                // Oops
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception squish) {
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(final String data) {
        }

        @Override
        protected void onCancelled() {
        }

        public JSONObject getCabBookingData() {
            return cabBookingData;
        }
    }

    private class UpdateRideTask extends AsyncTask<String, Void, String> {

        private HttpResponse response;

        @Override
        protected String doInBackground(String... url) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            Encryptor encryptor = new Encryptor();
            String encrptedkey = "";

            try {
                encrptedkey = encryptor.getEncryptedKeyValue(currentDateandTime);
            } catch (InvalidKeyException e1) {
                e1.printStackTrace();
            } catch (IllegalBlockSizeException e1) {
                e1.printStackTrace();
            } catch (BadPaddingException e1) {
                e1.printStackTrace();
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (DecoderException e1) {
                e1.printStackTrace();
            }

            try {
                HttpPost request = new HttpPost(url[0]);
                request.addHeader("Authorization", "Basic " + authenticationHeader);
                request.addHeader("androidkey", encrptedkey);
                request.addHeader("Content-Type", "application/json");

                String json = "";

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("ridelastlat", Math.round(currentLocationCoordinate.latitude * 10000.0) / 10000.0);
                jsonObject.accumulate("ridelastlong", Math.round(currentLocationCoordinate.longitude * 10000.0) / 10000.0);
                jsonObject.accumulate("requestdatetime", currentDateandTime);
                jsonObject.accumulate("estimateddistance", Math.round(distanceTravelled * 100.0) / 100.0);
                jsonObject.accumulate("estimatedtime", duration);
                jsonObject.accumulate("estimatedfare", Math.round(costIncurred * 100.0) / 100.0);

                if ((++stateCounter) % 5 == 0) {
                    Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = gcd.getFromLocation(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude, 1);
                    if (addresses.size() > 0 && addresses.get(0).getAdminArea() != null)
                        jsonObject.accumulate("state", addresses.get(0).getAdminArea());
                    else
                        jsonObject.accumulate("state", "");
                } else jsonObject.accumulate("state", "");

                // 4. convert JSONObject to JSON to String
                json = jsonObject.toString();

                // 5. set json to StringEntity

                request.setEntity(new StringEntity(json));
                HttpClient httpclient = new DefaultHttpClient();
                response = httpclient.execute(request);

            } catch (Exception e) {
                e.printStackTrace();
            }
            InputStream inputStream = null;
            String result = null;
            try {
                HttpEntity entity = response.getEntity();
                inputStream = entity.getContent();

                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                result = sb.toString();
            } catch (Exception e) {
                // Oops
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception squish) {
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(String data) {
            JSONObject jObject;
            if (data != null) {
                //Todo if the ride has been closed
                try {
                    jObject = new JSONObject(data);
                    int currentRefund = jObject.getInt("refund");
                    if (currentRefund > refund) {
                        createAlertDialog("Refund!", "\u20B9 " + (currentRefund - refund) + " have been added to your account.");
                        refund = currentRefund;
                    }
                    if (jObject.getBoolean("rideratingawaited")) {
                        //Todo show the bill summary
                        setStateRatingPending();
                    } else if (jObject.getBoolean("rideunderway")) {
                        cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"), jObject.getDouble("ridelastlong"));
                        nextCabDestination = new LatLng(jObject.getDouble("nextdestlat"), jObject.getDouble("nextdestlong"));
                        billingRate = (float) jObject.getDouble("billingrate");
                        if (billingRate == 10.0f) {
                            if (coPassengers == null || coPassengers.size() != 0) {
                                getCopassengerInfo();
                                ((FragmentMain) displayFragment).setCopassengerLayout(coPassengers.size());
                            }
                        } else if (billingRate == 6.5f) {
                            if (coPassengers == null || coPassengers.size() != 1) {
                                getCopassengerInfo();
                                ((FragmentMain) displayFragment).setCopassengerLayout(coPassengers.size());
                            }
                        } else if (billingRate == 5f) {
                            if (coPassengers == null || coPassengers.size() != 2) {
                                getCopassengerInfo();
                                ((FragmentMain) displayFragment).setCopassengerLayout(coPassengers.size());
                            }
                        } else if (billingRate == 4f) {
                            if (coPassengers == null || coPassengers.size() != 3) {
                                getCopassengerInfo();
                                ((FragmentMain) displayFragment).setCopassengerLayout(coPassengers.size());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d("Exception", e.toString());
                }
            } else {
                //Todo if no data come from server
            }
            checkUpdateRide = false;
        }

        @Override
        protected void onCancelled() {

        }
    }

    private class HttpRequestTask extends AsyncTask<String, Void, String> {

        private HttpResponse response;
        JSONObject data;
        boolean check;
        boolean report;
        private String comment = "";

        HttpRequestTask(JSONObject data) {
            this.data = data;
            check = true;
            report = false;
        }

        HttpRequestTask() {
            this.data = new JSONObject();
            check = false;
            report = false;
        }

        HttpRequestTask(String msg) {
            comment = msg;
            check = false;
            report = true;
        }

        @Override
        protected String doInBackground(String... url) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            Encryptor encryptor = new Encryptor();
            String encrptedkey = "";

            try {
                encrptedkey = encryptor.getEncryptedKeyValue(currentDateandTime);
            } catch (InvalidKeyException e1) {
                e1.printStackTrace();
            } catch (IllegalBlockSizeException e1) {
                e1.printStackTrace();
            } catch (BadPaddingException e1) {
                e1.printStackTrace();
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (DecoderException e1) {
                e1.printStackTrace();
            }

            try {

                HttpPost request = new HttpPost(url[0]);
                request.addHeader("Authorization", "Basic " + authenticationHeader);
                request.addHeader("androidkey", encrptedkey);

                if (check) {
                    data.accumulate("counter", currentDateandTime);
                    request.addHeader("Content-Type", "application/json");
                    request.setEntity(new StringEntity(data.toString()));
                } else if (report) {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("counter", currentDateandTime));
                    nameValuePairs.add(new BasicNameValuePair("comment", comment));
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                } else {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("counter", currentDateandTime));
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                }
                HttpClient httpclient = new DefaultHttpClient();

                try {
                    response = httpclient.execute(request);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            InputStream inputStream = null;
            String result = null;
            try {
                HttpEntity entity = response.getEntity();
                inputStream = entity.getContent();

                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                result = sb.toString();
            } catch (Exception e) {
                // Oops
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception squish) {
                }
            }
            System.out.println(result);
            return result;
        }

        @Override
        protected void onPostExecute(final String data) {
        }

        @Override
        protected void onCancelled() {

        }
    }

    public class GetDirectionsAsyncTask extends AsyncTask<Map<String, String>, Object, ArrayList> {
        public static final String USER_CURRENT_LAT = "user_current_lat";
        public static final String USER_CURRENT_LONG = "user_current_long";
        public static final String DESTINATION_LAT = "destination_lat";
        public static final String DESTINATION_LONG = "destination_long";
        public static final String DIRECTIONS_MODE = "directions_mode";
        private Exception exception;
        private ProgressDialog progressDialog;

        public void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Path to next Stop");
            progressDialog.show();
        }

        @Override
        public void onPostExecute(ArrayList result) {
            progressDialog.dismiss();
            if (exception == null) {
                handleGetDirectionsResult(result);
            } else {
                processException();
            }
        }

        @Override
        protected ArrayList doInBackground(Map<String, String>... params) {
            Map<String, String> paramMap = params[0];
            try {
                LatLng fromPosition = new LatLng(Double.valueOf(paramMap.get(USER_CURRENT_LAT)), Double.valueOf(paramMap.get(USER_CURRENT_LONG)));
                LatLng toPosition = new LatLng(Double.valueOf(paramMap.get(DESTINATION_LAT)), Double.valueOf(paramMap.get(DESTINATION_LONG)));
                GMapV2Direction md = new GMapV2Direction();
                Document doc = md.getDocument(fromPosition, toPosition, paramMap.get(DIRECTIONS_MODE));
                ArrayList directionPoints = md.getDirection(doc);
                return directionPoints;
            } catch (Exception e) {
                exception = e;
                return null;
            }
        }

        private void processException() {
            Toast.makeText(getApplicationContext(), getString(R.string.error_when_retrieving_data), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_search:
                try {
                    drawerItemSelectedAction(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.action_settings:
                applicationLogout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public String[] getAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String[] address = new String[2];
        address[0] = address[1] = "";
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses != null) {

            if (addresses.get(0).getAddressLine(0) != null)
                address[0] = addresses.get(0).getAddressLine(0);
            if (addresses.get(0).getLocality() != null) address[1] = addresses.get(0).getLocality();
            if (addresses.get(0).getAdminArea() != null)
                address[1] = address[1] + ", " + addresses.get(0).getAdminArea();
            if (addresses.get(0).getPostalCode() != null)
                address[1] = address[1] + ", " + addresses.get(0).getPostalCode();
            return address;
        }

        return new String[]{"Address could not be found", "Address could not be found"};
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle query) {
        CursorLoader cLoader = null;
        if (arg0 == 0)
            cLoader = new CursorLoader(getBaseContext(), PlaceProvider.SEARCH_URI, null, null, new String[]{query.getString("query")}, null);
        else if (arg0 == 1)
            cLoader = new CursorLoader(getBaseContext(), PlaceProvider.DETAILS_URI, null, null, new String[]{query.getString("query")}, null);
        return cLoader;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        // TODO Auto-generated method stub
        FragmentMain disp = (FragmentMain) displayFragment;
        LatLng dragPosition = marker.getPosition();
        double dragLat = dragPosition.latitude;
        double dragLong = dragPosition.longitude;
        if (sourceMarker.getPosition().latitude == dragLat && sourceMarker.getPosition().longitude == dragLong) {
            sAddress = getAddress(sourceMarker.getPosition().latitude, sourceMarker.getPosition().longitude);
            disp.setTextSource(sAddress[0] + ", " + sAddress[1]);
        } else if (destinationMarker.getPosition().latitude == dragLat && destinationMarker.getPosition().longitude == dragLong) {
            dAddress = getAddress(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude);
            disp.setTextDestination(dAddress[0] + ", " + dAddress[1]);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {

        // TODO Auto-generated method stub
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    public void zoomToSource() {
        if (sourceMarker != null) {
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(sourceMarker.getPosition(), 15);
            mGoogleMap.animateCamera(yourLocation);
        }
    }

    public void zoomToDestination() {
        if (destinationMarker != null) {
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(destinationMarker.getPosition(), 15);
            mGoogleMap.animateCamera(yourLocation);
        }
    }

}



