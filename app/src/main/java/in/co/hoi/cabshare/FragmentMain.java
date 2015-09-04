package in.co.hoi.cabshare;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

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
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Ujjwal on 27-06-2015.
 */
public class FragmentMain extends Fragment {

    boolean inRide;
    boolean awaitingRide;
    boolean ratingPending;
    int numOfcoPassengers;
    int rideId;
    String authHeader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;


        inRide = getArguments().getBoolean("inride");
        awaitingRide = getArguments().getBoolean("awaitingride");
        ratingPending = getArguments().getBoolean("ratingpending");

        if(inRide){
            Log.d("User State", "inRide");
            view = inflater.inflate(R.layout.fragment_inride, container, false);
        }
        else if(awaitingRide){
            Log.d("User State", "awaiting Ride");
            view = inflater.inflate(R.layout.fragment_awatingride, container, false);
        }
        else if(ratingPending){
            Log.d("User State", "rating pending");
            view = inflater.inflate(R.layout.fragment_rating_pending, container, false);
        }
        else {
            Log.d("User State", "Book Ride");
            view = inflater.inflate(R.layout.fragment_bookingride, container, false);
            TextView tvSource = (TextView) view.findViewById(R.id.source_address);
            tvSource.setText(getArguments().getString("pickupaddress"));
            TextView tvDestination = (TextView) view.findViewById(R.id.destination_address);
            tvSource.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((MainActivity)getActivity()).zoomToSource();
                }
            });
            tvSource.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    ((MainActivity) getActivity()).sendMessageSource();
                    return true;
                }
            });
            tvDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((MainActivity) getActivity()).zoomToDestination();
                }
            });
            tvDestination.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    ((MainActivity) getActivity()).sendMessageDestination();
                    return true;
                }
            });
        }
        return view;
    }

    public void setTextSource(String text){
        TextView textView = (TextView) getView().findViewById(R.id.source_address);
        textView.setText(text);
    }

    public void setTextDestination(String text){
        TextView textView = (TextView) getView().findViewById(R.id.destination_address);
        textView.setText(text);
    }

    public int getDriverRating(){
        return (int) ((RatingBar)getView().findViewById(R.id.rating_bar)).getRating();
    }

    public void setCopassengerLayout(int numOfcoPassengers){

        de.hdodenhof.circleimageview.CircleImageView c1,c2,c3,cUnknown;
        c1 = (de.hdodenhof.circleimageview.CircleImageView) getView().findViewById(R.id.coPassenger1);
        c1.setImageResource(R.drawable.male_user);
        c1.setOnClickListener(null);
        c2 = (de.hdodenhof.circleimageview.CircleImageView) getView().findViewById(R.id.coPassenger2);
        c2.setImageResource(R.drawable.male_user);
        c2.setOnClickListener(null);
        c3 = (de.hdodenhof.circleimageview.CircleImageView) getView().findViewById(R.id.coPassenger3);
        c3.setImageResource(R.drawable.male_user);
        c3.setOnClickListener(null);
        cUnknown = (de.hdodenhof.circleimageview.CircleImageView) getView().findViewById(R.id.unknown);

        if(numOfcoPassengers >= 1){
            c1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ((MainActivity) getActivity()).createRatingDialog(0);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            c1.setImageBitmap(((MainActivity)getActivity()).getPassengerBitmap(0));
        }
        Log.d("Passenger1", "done");

        if(numOfcoPassengers >= 2){
            c2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ((MainActivity) getActivity()).createRatingDialog(1);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            c2.setImageBitmap(((MainActivity) getActivity()).getPassengerBitmap(1));
        }

        Log.d("Passenger2", "done");

        if(numOfcoPassengers >= 3){
            c3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ((MainActivity) getActivity()).createRatingDialog(2);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            c3.setImageBitmap(((MainActivity) getActivity()).getPassengerBitmap(2));
        }
        Log.d("Passenger3", "done");
        cUnknown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Todo report unknnown passenger in ride
                ((MainActivity) getActivity()).createRiderReportDialog();
            }
        });
        Log.d("CODE_FLOW", "Horizontal View Created");
    }

    public void setDriverDetails(){
        TextView carMake = (TextView) getView().findViewById(R.id.car_make);
        TextView carNo = (TextView) getView().findViewById(R.id.car_no);
        carMake.setText(((MainActivity) getActivity()).getCarModel());
        carNo.setText(((MainActivity) getActivity()).getCarNumber());

        TextView driverName = (TextView) getView().findViewById(R.id.driver_name);
        TextView driverNo = (TextView) getView().findViewById(R.id.driver_moblie);
        driverName.setText(((MainActivity) getActivity()).getDriverName());
        driverNo.setText(((MainActivity) getActivity()).getDriverPhone());

        de.hdodenhof.circleimageview.CircleImageView driverpic =
                (de.hdodenhof.circleimageview.CircleImageView) getView().findViewById(R.id.driver_pic);
        driverpic.setImageBitmap(((MainActivity) getActivity()).getDriverPic());
    }

    public void setRideSummary(JSONObject rideSummaryData){

        //driverid driverpic
        try {

            TextView distance = (TextView) getView().findViewById(R.id.distance);
            distance.setText(((double) Math.round(rideSummaryData.getDouble("distancecoveredkms") * 100) / 100)+"");

            TextView duration = (TextView) getView().findViewById(R.id.duration);
            duration.setText(((double) Math.round(rideSummaryData.getDouble("timetakenmins") * 100) / 100)+"");

            Double totalFare =rideSummaryData.getDouble("totalfare");
            TextView fare = (TextView) getView().findViewById(R.id.fare);
            fare.setText("\u20B9" + ((double) Math.round(totalFare * 100) / 100));

            DetailDriver driver = new DetailDriver(rideSummaryData.getString("driverpic"));

            de.hdodenhof.circleimageview.CircleImageView driverpic =
                    (de.hdodenhof.circleimageview.CircleImageView) getView().findViewById(R.id.driverPic);
            driverpic.setImageBitmap(driver.driverPic);


        } catch (JSONException e) {
            Log.e("Exception", e.getMessage());
        }
    }


}
