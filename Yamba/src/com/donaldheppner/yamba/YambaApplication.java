package com.donaldheppner.yamba;

import android.app.Application;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import winterwell.jtwitter.Twitter;

import java.util.List;

/**
 * Created by Donald on 03/01/14.
 */
public class YambaApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = YambaApplication.class.getSimpleName();

    private Twitter twitter;
    private SharedPreferences prefs;
    private boolean serviceRunning;
    private StatusData statusData;

    @Override
    public void onCreate() {
        super.onCreate();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.prefs.registerOnSharedPreferenceChangeListener(this);
        Log.i(TAG, "onCreated");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "onTerminated");
    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        twitter = null;
    }

    public synchronized Twitter getTwitter() {
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

    public boolean isServiceRunning() {
        return serviceRunning;
    }

    public void setServiceRunning(boolean serviceRunning) {
        this.serviceRunning = serviceRunning;
    }

    public StatusData getStatusData() {
        if (statusData == null) {
            statusData = new StatusData(this);
        }
        return statusData;
    }

    // Connects to the online service and puts the latest statuses into DB.
    // Returns the count of new statuses
    public synchronized int fetchStatusUpdates() {
        Log.d(TAG, "Fetching status updates");
        Twitter twitter = this.getTwitter();
        if (twitter == null) {
            Log.d(TAG, "Twitter connection info not initialized");
            return 0;
        }
        try {
            List<Twitter.Status> statusUpdates = twitter.getFriendsTimeline();
            long latestStatusCreatedAtTime = getStatusData().getLatestStatusCreatedAtTime();
            int count = 0;
            ContentValues values = new ContentValues();
            for (Twitter.Status status : statusUpdates) {
                values.put(StatusData.C_ID, status.getId());

                long createdAt = status.getCreatedAt().getTime();
                values.put(StatusData.C_CREATED_AT, createdAt);
                values.put(StatusData.C_TEXT, status.getText());
                values.put(StatusData.C_USER, status.getUser().getName());

                Log.d(TAG, "Got update with id " + status.getId() + ". Saving");

                getStatusData().insertOrIgnore(values);
                if (latestStatusCreatedAtTime < createdAt) {
                    count++;
                }
            }

            Log.d(TAG, count > 0 ? "Got " + count + " status updates" : "No new status updates");

            return count;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to fetch status updates", e);
            return 0;
        }
    }
}
