package in.co.hoi.cabshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

/**
 * Created by Ujjwal on 24-06-2015.
 */
public class DetailDriver {

    String name;
    String picURL;
    String phone;
    String backgroundCheck;
    Bitmap driverPic;

    DetailDriver(JSONObject driverData) {
        try {
            if (driverData.has("drivername")) name = driverData.getString("drivername");
            if (driverData.has("driverpic")) picURL = driverData.getString("driverpic");
            if (driverData.has("driverphone")) phone = driverData.getString("driverphone");
            if (driverData.has("driverbgc")) backgroundCheck = driverData.getString("driverbgc");
            driverPic = new DownloadImageTask().execute(picURL).get();
        } catch (Exception e) {
            Log.d("Exception", "Driver Data");
        }
    }

    DetailDriver(String picurl){
        picURL = picurl;
        try{
            driverPic = new DownloadImageTask().execute(picURL).get();
        }catch (Exception e){
            Log.e("Exception", "Driver Pic not downloaded, Connection Error");
        }

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        public DownloadImageTask() {
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {

        }
    }
}
