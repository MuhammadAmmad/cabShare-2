package in.co.hoi.cabshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

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

/**
 * Created by Ujjwal on 27-06-2015.
 */
public class WalletListAdapter  extends BaseAdapter implements View.OnClickListener {

    private Activity activity;
    private List<TransactionItem> data;
    private static LayoutInflater inflater=null;
    public Resources res;
    TransactionItem tempValues=null;
    JSONObject rideInfo;
    ProgressDialog mProgress;
    Handler mHandler;
    int i=0;

    public WalletListAdapter(Activity a, List<TransactionItem> d,Resources resLocal) {

        /********** Take passed values **********/
        activity = a;
        data=d;
        res = resLocal;

        /***********  Layout inflator to call external xml layout () ***********/
        inflater = ( LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        if(data.size()<=0)
            return 1;
        return data.size();
    }

    @Override
    public TransactionItem getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /********* Create a holder Class to contain inflated xml file elements *********/
    public static class ViewHolder{

        public TextView amount;
        public TextView detail;
        public TextView id;
        public de.hdodenhof.circleimageview.CircleImageView transType;
        public TextView time;
        public int type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v  = convertView;
        ViewHolder holder;

        if(convertView==null){

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            v = inflater.inflate(R.layout.wallet_list_item, null);

            /****** View Holder Object to contain tabitem.xml file elements ******/

            holder = new ViewHolder();
            holder.transType = (de.hdodenhof.circleimageview.CircleImageView) v.findViewById(R.id.transType);
            holder.id = (TextView) v.findViewById(R.id.id);
            holder.amount = (TextView)v.findViewById(R.id.trans_cost);
            holder.detail = (TextView) v.findViewById(R.id.trans_detail);
            holder.time = (TextView)v.findViewById(R.id.trans_time);
            holder.type = 0;

            /************  Set holder with LayoutInflater ************/
            v.setTag( holder );
        }
        else
            holder=(ViewHolder)v.getTag();

        if(data.size()>0)
        {
            /***** Get each Model object from Arraylist ********/
            tempValues=null;
            tempValues = ( TransactionItem ) data.get( position );

            /************  Set Model values in Holder elements ***********/

            if(tempValues.detail.equals("voucher")){
                holder.transType.setImageResource(R.drawable.ic_voucher);
                System.out.println(tempValues.voucherId);
                holder.id.setText("#" + tempValues.voucherId);
                holder.detail.setText("VOUCHER");
                holder.type = 1;
            }else if(tempValues.detail.equals("gateway")){
                holder.transType.setImageResource(R.drawable.ic_gateway);
                holder.id.setText("#" + tempValues.creditpgId);
                holder.detail.setText("PAYMENT GATEWAY");
                holder.type = 2;
            }else if(tempValues.detail.equals("refund")){
                holder.transType.setImageResource(R.drawable.ic_refund);
                holder.id.setText("#" + tempValues.cancellationRequestId);
                holder.detail.setText("REFUND");
                holder.type = 3;
            }else if(tempValues.detail.equals("cancellation")){
                holder.transType.setImageResource(R.drawable.ic_cancel);
                holder.id.setText("#" + tempValues.cancellationId);
                holder.detail.setText("RIDE CANCELLED");
                holder.type = 4;
            }else if(tempValues.detail.equals("genriderequest")){
                holder.transType.setImageResource(R.drawable.ic_ride);
                holder.id.setText("#" + tempValues.genRideRequestId);
                holder.detail.setText("RIDE");
                holder.type = 5;
                ImageView summary = ((ImageView) v.findViewById(R.id.transSummary));
                summary.setVisibility(View.VISIBLE);
            }else if(tempValues.detail.equals("riderequest")){
                holder.transType.setImageResource(R.drawable.ic_ride);
                holder.id.setText("#" +tempValues.genRideRequestId);
                holder.detail.setText("RIDE");
            }

            holder.time.setText(tempValues.recordDateTime.substring(0,tempValues.recordDateTime.length() - 5));
            holder.amount.setText(res.getString(R.string.Rs)+" "+tempValues.amount);

            if(holder.type == 5){
                v.setOnClickListener(new OnItemClickListener( position ));
            }else{
                ImageView summary = ((ImageView) v.findViewById(R.id.transSummary));
                summary.setVisibility(View.INVISIBLE);
                if(v.hasOnClickListeners()) v.setOnClickListener(null);
            }
        }
        return v;
    }

    @Override
    public void onClick(View v) {
        Log.v("CustomAdapter", "=====Row button clicked=====");
    }

    private class OnItemClickListener implements View.OnClickListener {
        private int mPosition;
        TransactionItem selectedTransaction;
        String authHeader;


        OnItemClickListener(int position){
            mPosition = position;
            selectedTransaction = data.get(position);
        }

        private void createTransactionDetailDialog(){
            if(rideInfo == null){
                ((MainActivity)activity).createAlertDialog("Error", "Connection to server failed!");
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                // Get the layout inflater
                LayoutInflater inflater = activity.getLayoutInflater();
                View dView = inflater.inflate(R.layout.dialog_transaction_summary, null);
                builder.setView(dView);

                // Set other dialog properties
                // Create the AlertDialog
                final AlertDialog dialog = builder.create();
                try{
                    Double distCharge = rideInfo.getDouble("costincurred") - (rideInfo.getDouble("timeincurred") * 0.5);

                    TextView sourceAdd = (TextView) dView.findViewById(R.id.source);
                    sourceAdd.setText(rideInfo.getString("origaddress1") + rideInfo.getString("origaddress2"));

                    TextView destinationAdd = (TextView) dView.findViewById(R.id.destination);
                    destinationAdd.setText(rideInfo.getString("destaddress1") + rideInfo.getString("destaddress2"));

                    TextView bookingTime = (TextView) dView.findViewById(R.id.trans_time);
                    bookingTime.setText((rideInfo.getString("bookingtime")).replace(
                            (rideInfo.getString("bookingtime").substring(rideInfo.getString("bookingtime").length()-5)),""));

                    TextView distance = (TextView) dView.findViewById(R.id.distance);
                    distance.setText("" + rideInfo.getDouble("distanceincurred"));

                    TextView duration = (TextView) dView.findViewById(R.id.duration);
                    duration.setText("" + rideInfo.getDouble("timeincurred"));

                    TextView distanceCharges = (TextView) dView.findViewById(R.id.distanceCost);
                    distanceCharges.setText(res.getString(R.string.Rs) +((double) Math.round(distCharge* 100) / 100));

                    TextView durationCharges = (TextView) dView.findViewById(R.id.durationCost);
                    durationCharges.setText(res.getString(R.string.Rs) +((double) Math.round(0.5*rideInfo.getDouble("timeincurred")* 100) / 100));

                    TextView tolltax = (TextView) dView.findViewById(R.id.tolltax);
                    tolltax.setText(res.getString(R.string.Rs) + ((double) Math.round(rideInfo.getDouble("tolltax")* 100) / 100));

                    TextView fare = (TextView) dView.findViewById(R.id.fare);
                    fare.setText(res.getString(R.string.Rs) + ((double) Math.round(rideInfo.getDouble("costincurred")*100 + rideInfo.getDouble("tolltax")* 100) / 100));

                }catch(Exception e){
                    Log.d("EXCEPTION", e.getMessage());
                }

                Button dialogClose = (Button) dView.findViewById(R.id.ride_summary_close);
                dialogClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        }

        @Override
        public void onClick(View arg0) {
            final MainActivity mainActivity = (MainActivity) activity;
            authHeader = mainActivity.getAuthHeader();

            mProgress = ProgressDialog.show(mainActivity, "",
                    "Please Wait..", true);

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    try {
                        //Code to execute in other thread
                        HttpRequestTask rideRequestInfo = new HttpRequestTask();
                        Log.d("CODE_FLOW", "CAB DETAILS_REQESTED");
                        String res = rideRequestInfo.execute("http://www.hoi.co.in/api/riderequestinfo/"+selectedTransaction.genRideRequestId).get();
                        System.out.println(res);
                        JSONObject jObject = new JSONObject(res);
                        rideInfo = jObject.getJSONObject("grri");
                        Log.d("CODE_FLOW", "CAB DETAILS_RECEIVED");
                    } catch (Exception e) {
                        Log.e("Exception", e.toString());
                        rideInfo = null;
                    }

                        //Code to execute in main thread
                        mHandler = new Handler(mainActivity.getApplicationContext().getMainLooper());
                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                    createTransactionDetailDialog();
                            }
                        };
                        mHandler.postDelayed(myRunnable, 500);

                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Code to execute in UI thread or main thread
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    mProgress.dismiss();
                                }
                            }, 500);
                        }
                    });
                }
            }).start();

        }

        private class HttpRequestTask extends AsyncTask<String, Void, String> {

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
                    request.addHeader("Authorization", "Basic " + authHeader);
                    request.addHeader("androidkey", encrptedkey);

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("counter", currentDateandTime));
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpClient httpclient = new DefaultHttpClient();

                    try {
                        response = httpclient.execute(request);
                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
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
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                } catch (Exception e) {
                    // Oops
                }
                finally {
                    try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
                }
                return result;
            }

            @Override
            protected void onPostExecute(final String data) {
            }

            @Override
            protected void onCancelled() {

            }
        }
    }
}
