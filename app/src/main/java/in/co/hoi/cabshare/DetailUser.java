package in.co.hoi.cabshare;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Ujjwal on 24-06-2015.
 */
public class DetailUser {

    String name;
    String username;
    String phone;
    String displayPic;

    DetailUser(JSONObject userData) {
        try {
            name = userData.getString("name");
            username = userData.getString("username");
            phone = userData.getString("phone");
            displayPic = userData.getString("displaypic");
        } catch (Exception e) {
            Log.d("Exception", "User Data");
        }
    }

    DetailUser(String name, String phone, String username, String displayPic) {
        this.name = name;
        this.username = username;
        this.phone = phone;
        this.displayPic = displayPic;

    }

}
