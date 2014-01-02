package com.donaldheppner.yamba;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;

public class StatusActivity extends Activity implements View.OnClickListener, TextWatcher, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "StatusActivity";

    EditText editText;
    TextView textCount;
    Button updateButton;
    Twitter twitter;
    SharedPreferences prefs;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status);

        // find views
        editText = (EditText) findViewById(R.id.editText);
        editText.addTextChangedListener(this);

        textCount = (TextView) findViewById(R.id.textCount);
        updateButton = (Button) findViewById(R.id.buttonUpdate);
        updateButton.setOnClickListener(this);

        twitter = new Twitter("student", "password");
        twitter.setAPIRootUrl("http://yamba.marakana.com/api");

        // setup prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemPrefs:
                startActivity(new Intent(this, PrefsActivity.class));
                break;
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        twitter = null;
    }

    private Twitter getTwitter() {
        if (twitter == null) {
            String username, password, apiRoot;
            username = prefs.getString("username", "");
            password = prefs.getString("password", "");
            apiRoot = prefs.getString("apiRoot", "http://yamba.marakana.com/api");

            twitter = new Twitter(username, password);
            twitter.setAPIRootUrl(apiRoot);
        }

        return twitter;
    }

    // Asynchronously posts to twitter
    class PostToTwitter extends AsyncTask<String, Integer, String> {
        // Called to initiate the background activity
        @Override
        protected String doInBackground(String... statuses) {
            try {
                Twitter.Status status = getTwitter().updateStatus(statuses[0]);
                return status.text;
            } catch (TwitterException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                return "Failed to post";
            }
        }

        // Called when there's a status to be updated
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        // Called once the background activity has completed
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(StatusActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {
        String status = editText.getText().toString();
        new PostToTwitter().execute(status);
        Log.d(TAG, "onClicked");
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable statusText) {
        int count = 140 - statusText.length();
        textCount.setText(Integer.toString(count));
        textCount.setTextColor(Color.GREEN);

        if (count < 10)
            textCount.setTextColor(Color.YELLOW);
        if (count < 0)
            textCount.setTextColor(Color.RED);
    }
}
