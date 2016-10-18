package in.co.hoi.rulyp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    ObscuredSharedPreferences prefs;
    private String authenticationHeader;
    private String mPhone = "";

    private LoginButton loginButtonFacebook;
    private CallbackManager callbackManager;
    private AccessToken accessToken;
    private Profile profile;
    JSONObject facebookLoginDetails;
    private int numberOfFriends = -1;
    ProgressDialog processDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        prefs = ObscuredSharedPreferences.getPrefs(this, "HoiCabs", Context.MODE_PRIVATE);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        startService(new Intent(this, RegistrationIntentService.class));

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().logOut();

        loginButtonFacebook = (LoginButton) findViewById(R.id.login_button);
        loginButtonFacebook.setReadPermissions("user_friends");
        // Other app specific specialization

        // Callback registration
        loginButtonFacebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                //Todo show dialog for information received
                accessToken = loginResult.getAccessToken();
                profile = Profile.getCurrentProfile();
                facebookLoginDetails = new JSONObject();
                try {
                    List<GraphResponse> res = new GraphRequest(
                            accessToken, "/me/friends",
                            null, HttpMethod.GET, new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                        }
                    }
                    ).executeAsync().get();
                    JSONObject resp = null;
                    resp = res.get(0).getJSONObject();
                    numberOfFriends = (resp.getJSONObject("summary")).getInt("total_count");
                    GraphRequest request = GraphRequest.newMeRequest(
                            accessToken,
                            new GraphRequest.GraphJSONObjectCallback() {
                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                }
                            });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "id,first_name,gender,last_name,link,name,verified,email");
                    request.setParameters(parameters);
                    res = request.executeAsync().get();
                    facebookLoginDetails = res.get(0).getJSONObject();
                    if (numberOfFriends >= 50) {
                        try {
                            createSignUpDialog();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //todo show error dialog : signup failed
                        Toast.makeText(getApplicationContext(), "You should have atleast 50 friends on facebook", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.d("EXCEPTION", e.getMessage());
                    createAlertDialog("Error", "Action Failed. Please try again");
                }
                LoginManager.getInstance().logOut();
            }

            @Override
            public void onCancel() {
                Log.i("LOGIN", "Facebook Login Cancelled");
                createAlertDialog("Error", "Action Cancelled");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.i("LOGIN", exception.getMessage());
                createAlertDialog("Error", "Facebook signup failed");
            }
        });
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);

        checkForServices(true);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if ((id == R.id.login) || (id == EditorInfo.IME_NULL)) {
                    try {
                        attemptLogin();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    attemptLogin();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        TextView forgotPass = (TextView) findViewById(R.id.forgot_password);
        forgotPass.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo show forgot password dialog
                LoginManager.getInstance().registerCallback(callbackManager,
                        new FacebookCallback<LoginResult>() {
                            @Override
                            public void onSuccess(LoginResult loginResult) {
                                Log.d("Success", "Login");
                            }

                            @Override
                            public void onCancel() {
                                Toast.makeText(getApplicationContext(), "Action Failed", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(FacebookException exception) {
                                Log.e("Exception", exception.getMessage());
                                Toast.makeText(getApplicationContext(), "Action Failed", Toast.LENGTH_LONG).show();
                            }
                        });
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile", "user_friends"));
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);

                // Get the layout inflater
                LayoutInflater inflater = LoginActivity.this.getLayoutInflater();
                View dView = inflater.inflate(R.layout.dialog_forgotpass, null);

                final EditText otp = (EditText) dView.findViewById(R.id.otp);
                otp.setHint("Enter Mobile No");

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
                                LoginManager.getInstance().logOut();
                                LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                                    @Override
                                    public void onSuccess(LoginResult loginResult) {
                                        //Todo show dialog for information received
                                        accessToken = loginResult.getAccessToken();
                                        profile = Profile.getCurrentProfile();
                                        facebookLoginDetails = new JSONObject();
                                        try {
                                            List<GraphResponse> res = new GraphRequest(
                                                    accessToken, "/me/friends",
                                                    null, HttpMethod.GET, new GraphRequest.Callback() {
                                                public void onCompleted(GraphResponse response) {


                                                }
                                            }
                                            ).executeAsync().get();
                                            JSONObject resp = null;
                                            resp = res.get(0).getJSONObject();
                                            numberOfFriends = (resp.getJSONObject("summary")).getInt("total_count");

                                            GraphRequest request = GraphRequest.newMeRequest(
                                                    accessToken,
                                                    new GraphRequest.GraphJSONObjectCallback() {
                                                        @Override
                                                        public void onCompleted(JSONObject object, GraphResponse response) {
                                                        }
                                                    });
                                            Bundle parameters = new Bundle();
                                            parameters.putString("fields", "id,first_name,gender,last_name,link,name,verified,email");
                                            request.setParameters(parameters);
                                            res = request.executeAsync().get();
                                            facebookLoginDetails = res.get(0).getJSONObject();
                                            if (numberOfFriends >= 50) {
                                                try {
                                                    createSignUpDialog();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                //todo show error dialog : signup failed
                                                Toast.makeText(getApplicationContext(), "You should have atleast 50 friends on facebook", Toast.LENGTH_LONG).show();
                                            }
                                        } catch (Exception e) {
                                            Log.d("EXCEPTION", e.getMessage());
                                            Toast.makeText(getApplicationContext(), "Action Failed", Toast.LENGTH_LONG).show();
                                        }
                                        LoginManager.getInstance().logOut();
                                    }

                                    @Override
                                    public void onCancel() {
                                        Log.i("LOGIN", "Facebook Login Cancelled");
                                        Toast.makeText(getApplicationContext(), "Action Failed", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(FacebookException exception) {
                                        Log.i("LOGIN", exception.getMessage());
                                        Toast.makeText(getApplicationContext(), "Connection Problem", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });
                // Set other dialog properties
                // Create the AlertDialog


                final AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String mobileNo = otp.getText().toString();
                        if (mobileNo.isEmpty()) {
                            otp.setError("Enter Mobile no");
                        } else if (mobileNo.length() != 10 || !mobileNo.matches("[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]")) {
                            otp.setError("wrong mobile number");
                        } else {
                            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        //system.out.println(response.toString());

                                        if (response.getBoolean("response")) {
                                            dialog.dismiss();
                                            Toast.makeText(getApplicationContext(), "OTP sent. Please wait for five minutes!", Toast.LENGTH_LONG).show();
                                        } else {
                                            otp.setError("Error! Please resend");
                                            Toast.makeText(getApplicationContext(), "OTP not sent. Please try again", Toast.LENGTH_LONG).show();
                                        }
                                    } catch (Exception e) {
                                        Log.e("EXCEPTION", e.getMessage());
                                        createAlertDialog("Error", "Action Failed. Please try again");
                                    }
                                }
                            };
                            JSONObject data = new JSONObject();
                            try {

                                data = new JSONObject().accumulate("otp", Profile.getCurrentProfile().getId());
                                Log.d("FACEBOOK_ID", Profile.getCurrentProfile().getId());
                            } catch (Exception e) {
                                Log.e("EXCEPTION", e.getMessage());
                                Toast.makeText(getApplicationContext(), "Error Occured", Toast.LENGTH_LONG).show();
                            }
                            volleyRequests(data, "http://www.hoi.co.in/password/changepassword/" + mobileNo,
                                    listener);
                            dialog.dismiss();
                            LoginManager.getInstance().logOut();
                            LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                                @Override
                                public void onSuccess(LoginResult loginResult) {
                                    //Todo show dialog for information received
                                    accessToken = loginResult.getAccessToken();
                                    profile = Profile.getCurrentProfile();
                                    facebookLoginDetails = new JSONObject();
                                    try {
                                        List<GraphResponse> res = new GraphRequest(
                                                accessToken, "/me/friends",
                                                null, HttpMethod.GET, new GraphRequest.Callback() {
                                            public void onCompleted(GraphResponse response) {


                                            }
                                        }
                                        ).executeAsync().get();
                                        JSONObject resp = null;
                                        resp = res.get(0).getJSONObject();
                                        numberOfFriends = (resp.getJSONObject("summary")).getInt("total_count");

                                        GraphRequest request = GraphRequest.newMeRequest(
                                                accessToken,
                                                new GraphRequest.GraphJSONObjectCallback() {
                                                    @Override
                                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                                    }
                                                });
                                        Bundle parameters = new Bundle();
                                        parameters.putString("fields", "id,first_name,gender,last_name,link,name,verified,email");
                                        request.setParameters(parameters);
                                        res = request.executeAsync().get();
                                        facebookLoginDetails = res.get(0).getJSONObject();
                                        if (numberOfFriends >= 50) {
                                            createSignUpDialog();
                                        } else {
                                            //todo show error dialog : signup failed
                                            Toast.makeText(getApplicationContext(), "You should have atleast 50 friends on facebook", Toast.LENGTH_LONG).show();
                                        }

                                    } catch (Exception e) {
                                        Log.d("EXCEPTION", e.getMessage());
                                        createAlertDialog("Error", "Action Failed. Please try again");
                                    }
                                    LoginManager.getInstance().logOut();
                                }

                                @Override
                                public void onCancel() {
                                    Log.i("LOGIN", "Facebook Login Cancelled");
                                    createAlertDialog("Error", "Action Cancelled");
                                }

                                @Override
                                public void onError(FacebookException exception) {
                                    Log.i("LOGIN", exception.getMessage());
                                    createAlertDialog("Error", "Action Failed. Please try again");
                                }
                            });
                        }
                    }
                });

            }
        });
    }

    private void checkForServices(boolean check) {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("LOCATION MANAGER", "GPS Not Enabled");
            Intent callGPSSettingIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(callGPSSettingIntent);
            super.finish();
        } else if (!isConnected) {
            createAlertDialog("No Connection", "Please check your Internet Connectivity!");
        } else if (check) {
            autoLogin();
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() throws ExecutionException, InterruptedException {
        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        checkForServices(false);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            mAuthTask = new UserLoginTask(email, password);

            startProcessDialog();
            mAuthTask.execute((Void) null);
            dismissProcessDialog();
        }
    }

    public void autoLogin() {
        if (mAuthTask != null) {
            return;
        }
        // Store values at the time of the login attempt.
        String email = prefs.getString("username", null);
        String password = prefs.getString("password", null);


        if (email != null && password != null) {
            if (isEmailValid(email) && isPasswordValid(password)) {
                mEmailView.setText(email);
                mPasswordView.setText(password);
                mAuthTask = new UserLoginTask(email, password);
                mAuthTask.execute((Void) null);
            }
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Grow the logic further to check for corporate emails
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {

        //system.out.println(password.length());
        return (password.length() > 4);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void createSignUpDialog() throws JSONException {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.signup_dialog, null);

        EditText firstName = (EditText) dView.findViewById(R.id.first_name);
        EditText lastName = (EditText) dView.findViewById(R.id.last_name);
        final EditText email = (EditText) dView.findViewById(R.id.email);
        final EditText mobile = (EditText) dView.findViewById(R.id.mobile);
        final EditText password = (EditText) dView.findViewById(R.id.password);
        final EditText confirmPassword = (EditText) dView.findViewById(R.id.confirm_password);

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
        String picurl = "https://graph.facebook.com/" + facebookLoginDetails.getString("id") + "/picture";
        de.hdodenhof.circleimageview.CircleImageView pic = (de.hdodenhof.circleimageview.CircleImageView)
                dView.findViewById(R.id.profile_pic);
        new DownloadImageTask(pic).execute(picurl);

        facebookLoginDetails.accumulate("picurl", picurl);
        if (facebookLoginDetails.has("first_name") && !facebookLoginDetails.isNull("first_name")) {
            firstName.setText(facebookLoginDetails.getString("first_name"));
            firstName.setFocusable(false);
        }
        if (facebookLoginDetails.has("last_name") && !facebookLoginDetails.isNull("last_name")) {
            lastName.setText(facebookLoginDetails.getString("last_name"));
            lastName.setFocusable(false);
        }
        if (facebookLoginDetails.has("email") && !facebookLoginDetails.isNull("email")) {
            email.setText(facebookLoginDetails.getString("email"));
        }

        dialog.show();
        //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProcessDialog();
                boolean checkDismiss = true;
                if (TextUtils.isEmpty(email.getText().toString())) {
                    email.setError("This cannot be empty");
                    checkDismiss = false;
                    dismissProcessDialog();
                }
                if (TextUtils.isEmpty(mobile.getText().toString())) {
                    mobile.setError("This cannot be empty");
                    checkDismiss = false;
                    dismissProcessDialog();
                }
                if (TextUtils.isEmpty(password.getText().toString())) {
                    password.setError("This cannot be empty");
                    checkDismiss = false;
                    dismissProcessDialog();
                }
                if ((confirmPassword.getText()).equals(password.getText())) {
                    confirmPassword.setError("Password Mismatch");
                    checkDismiss = false;
                    dismissProcessDialog();
                }


                //Do stuff, possibly set wantToCloseDialog to true then...
                if (checkDismiss) {
                    //Todo submit the form
                    try {
                        if (facebookLoginDetails.has("email")) facebookLoginDetails.remove("email");
                        if (facebookLoginDetails.has("mobile"))
                            facebookLoginDetails.remove("mobile");
                        if (facebookLoginDetails.has("password"))
                            facebookLoginDetails.remove("password");

                        facebookLoginDetails.accumulate("email", email.getText().toString());
                        facebookLoginDetails.accumulate("mobile", mobile.getText().toString());
                        facebookLoginDetails.accumulate("password", password.getText().toString());

                        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                //system.out.println(response.toString());
                                try {
                                    if (response.getBoolean("newusercreated")) {
                                        //Todo show otp dialog
                                        //system.out.println("check 1");
                                        dismissProcessDialog();
                                        dialog.dismiss();
                                        //system.out.println("check 2");
                                        createOTPDialog(facebookLoginDetails.getString("mobile"));
                                        //system.out.println("check 3");
                                    } else {
                                        //do nothing
                                        dialog.dismiss();
                                        dismissProcessDialog();
                                        Toast.makeText(getApplicationContext(), response.getString("reason"), Toast.LENGTH_LONG).show();
                                    }
                                } catch (Exception e) {
                                    dismissProcessDialog();
                                    dialog.dismiss();
                                    Log.e("EXCEPTION", e.getMessage());
                                    createAlertDialog("Error", "Action Failed. Please try again");
                                }
                            }
                        };

                        volleyRequests(facebookLoginDetails, "http://www.hoi.co.in/enduser/signupuser", listener);

                        //Todo check if signup succeeded
                        //new UserLoginTask(email.getText().toString(),password.getText().toString()).execute();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        createAlertDialog("Error", "Action Failed. Please try again");
                    }
                }
            }
        });
        Log.d("SIGNUP_DIALOG", "Sign up dialog created");
    }

    public void createOTPDialog(final String phone) {
        //
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.otp_dialog, null);

        final EditText otp = (EditText) dView.findViewById(R.id.otp);


        // Add the buttons
        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.resend, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (otp.getText().toString().isEmpty()) {
                    otp.setError("No Code entered");
                } else {
                    //Todo send otp using volley and resend if otp mismatch
                    Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getBoolean("response")) {

                                    mAuthTask = new UserLoginTask(facebookLoginDetails.getString("email"), facebookLoginDetails.getString("password"));
                                    startProcessDialog();
                                    mAuthTask.execute((Void) null);
                                    dismissProcessDialog();
                                    dialog.dismiss();
                                } else {

                                    otp.setError("OTP mismatch");
                                }
                            } catch (Exception e) {
                                Log.e("EXCEPTION", e.getMessage());
                                Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    try {
                        volleyRequests(new JSONObject().accumulate("otp", otp.getText().toString()), "http://www.hoi.co.in/enduser/confirmphone/" + phone,
                                listener);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (!response.getBoolean("response")) {
                                Toast.makeText(LoginActivity.this.getApplicationContext(), "OTP not sent!", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("EXCEPTION", e.getMessage());
                            Toast.makeText(LoginActivity.this.getApplicationContext(), "Connection Error", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                volleyRequests(new JSONObject(), "http://www.hoi.co.in/enduser/resendOTP/" + phone,
                        listener);
            }
        });

    }

    public void resendOTP(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);

        // Get the layout inflater
        LayoutInflater inflater = LoginActivity.this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_forgotpass, null);

        final EditText otp = (EditText) dView.findViewById(R.id.otp);
        otp.setHint("Enter Mobile No");

        ((TextView) dView.findViewById(R.id.dialogTitle)).setText("Resend OTP");

        // Add the buttons
        builder.setView(dView)

                // Add action buttons
                .setPositiveButton(R.string.resend, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.submit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog


        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPhone.isEmpty() || mPhone.equals("")){
                    otp.setError("First Resent OTP");
                }
                else if (otp.getText().toString().isEmpty()) {
                    otp.setError("No Code entered");
                } else {
                    //Todo send otp using volley and resend if otp mismatch
                    Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getBoolean("response")) {
                                    dialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Your Account is activated", Toast.LENGTH_LONG).show();
                                } else {
                                    otp.setError("OTP mismatch");
                                }
                            } catch (Exception e) {
                                Log.e("EXCEPTION", e.getMessage());
                                Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    try {
                        volleyRequests(new JSONObject().accumulate("otp", otp.getText().toString()), "http://www.hoi.co.in/enduser/confirmphone/" + mPhone,
                                listener);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String mobileNo = otp.getText().toString();
                if (mobileNo.isEmpty()) {
                    otp.setError("Enter Mobile no");
                } else if (mobileNo.length() != 10 || !mobileNo.matches("[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]")) {
                    otp.setError("wrong mobile number");
                } else {
                    Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (!response.getBoolean("response")) {
                                    Toast.makeText(getApplicationContext(), "OTP not sent!", Toast.LENGTH_LONG).show();
                                }else{
                                    Toast.makeText(getApplicationContext(), "OTP sent", Toast.LENGTH_LONG).show();
                                    otp.setText("");
                                    otp.setHint("Enter OTP");
                                    mPhone = mobileNo;
                                }
                            } catch (Exception e) {
                                Log.e("EXCEPTION", e.getMessage());
                                Toast.makeText(LoginActivity.this.getApplicationContext(), "Connection Error", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    volleyRequests(new JSONObject(), "http://www.hoi.co.in/enduser/resendOTP/" + mobileNo,
                            listener);
                }
            }
        });

    }

    private void startProcessDialog() {
        //Process running dialog
        processDialog = new ProgressDialog(LoginActivity.this);
        processDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        processDialog.setMessage("Please wait...");
        processDialog.setIndeterminate(true);
        processDialog.setCanceledOnTouchOutside(false);
        processDialog.show();
    }

    private void dismissProcessDialog() {
        if (processDialog != null)
            processDialog.dismiss();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, String> {

        private final String mEmail;
        private final String mPassword;
        private HttpResponse response;
        JSONObject jObject;
        Intent intent;
        ProgressDialog asyncDialog = new ProgressDialog(LoginActivity.this);

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            Encryptor encryptor = new Encryptor();
            String encrptedkey = "";

            try {
                encrptedkey = encryptor.getEncryptedKeyValue(currentDateandTime);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                HttpPost request = new HttpPost("http://www.hoi.co.in/api/check");
                String credentials = mEmail + ":" + mPassword;
                String base64EncodedCredentials = authenticationHeader = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                request.addHeader("Authorization", "Basic " + base64EncodedCredentials);
                request.addHeader("androidkey", encrptedkey);


                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("counter", currentDateandTime));

                request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpClient httpclient = new DefaultHttpClient();
                try {
                    //system.out.println("ch2");
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
                //system.out.println("ch4");
                HttpEntity entity = response.getEntity();

                inputStream = entity.getContent();
                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                //system.out.println("ch5");
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                result = sb.toString();
            } catch (Exception e) {
                asyncDialog.dismiss();
                Log.d("Exception: ", e.getMessage());

            } finally {
                //system.out.println(result);
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception squish) {
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(final String data) {
            asyncDialog.dismiss();
            mAuthTask = null;
            try {
                jObject = new JSONObject(data);
                int responseCode = jObject.getInt("code");
                switch (responseCode) {
                    case 1:
                        //Login success ... Loading main activity
                        Log.d("Login", "Setting Parameters for Main Activity");
                        prefs.edit().putString("username", mEmail).commit();
                        prefs.edit().putString("password", mPassword).commit();
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("username", jObject.getString("username"));
                        intent.putExtra("name", jObject.getString("name"));
                        intent.putExtra("phone", jObject.getString("phone"));
                        intent.putExtra("displaypic", jObject.getString("displaypic"));
                        intent.putExtra("availableinr", jObject.getDouble("availableinr"));
                        intent.putExtra("ratingpending", jObject.getBoolean("hastorateprevious"));
                        intent.putExtra("inride", jObject.getBoolean("inaride"));
                        intent.putExtra("awaitingride", jObject.getBoolean("awaitingride"));
                        intent.putExtra("genriderequestid", jObject.getInt("genriderequestid"));
                        intent.putExtra("authenticationHeader", authenticationHeader);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Log.d("Login", "Finished Setting Parameters for Main Activity");
                        getApplicationContext().startActivity(intent);
                        LoginActivity.this.finish();
                        break;
                    case 2:
                        Toast.makeText(getApplicationContext(), "Application not Validated", Toast.LENGTH_LONG).show();
                        mPasswordView.setError(getString(R.string.error_incorrect));
                        mPasswordView.requestFocus();
                        break;
                    case 3:
                        Toast.makeText(getApplicationContext(), "User Not Authorized", Toast.LENGTH_LONG).show();
                        break;
                    case 4:
                        Toast.makeText(getApplicationContext(), "Encrypted Key Not Validated", Toast.LENGTH_LONG).show();
                        mPasswordView.setError(getString(R.string.error_incorrect));
                        mPasswordView.requestFocus();
                        break;
                    case 5:
                        Toast.makeText(getApplicationContext(), "Time not Validated", Toast.LENGTH_LONG).show();
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Some Error Occured", Toast.LENGTH_LONG).show();
            }
            super.onPostExecute(data);
        }

        @Override
        protected void onPreExecute() {
            //set message of the dialog
            asyncDialog.setMessage(getString(R.string.login_attempt));
            asyncDialog.setCancelable(false);
            //show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
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
            bmImage.setImageBitmap(result);
        }
    }

    private void volleyRequests(JSONObject data, String url, Response.Listener<JSONObject> listener) {
        //system.out.println(data.toString());
        //system.out.println(url);
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error != null) {
                    Log.e("EXCEPTION", error.toString());
                    dismissProcessDialog();
                }
                Log.e("EXCEPTION", "Exception occured in volley");
            }
        };

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                url,
                data,
                listener,
                errorListener
        );
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);

        MainApplication.getInstance().getRequestQueue().add(request);
    }
}