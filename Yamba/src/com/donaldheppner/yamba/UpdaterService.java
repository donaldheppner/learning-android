package com.donaldheppner.yamba;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Donald on 03/01/14.
 */
public class UpdaterService extends Service {

    /**
     * Thread that performs the actual update from the online service
     */
    private class Updater extends Thread {

        public Updater() {
            super("UpdaterService-Updater");
        }

        @Override
        public void run() {
            UpdaterService updaterService = UpdaterService.this;

            while (updaterService.runFlag) {
                Log.d(TAG, "Running background thread");
                try {
                    YambaApplication yamba = (YambaApplication) updaterService.getApplication();
                    int newUpdates = yamba.fetchStatusUpdates();
                    if (newUpdates > 0) {
                        Log.d(TAG, "We have " + newUpdates + " new status");
                    }
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    updaterService.runFlag = false;
                }
            }
        }
    } // Updater


    static final String TAG = "UpdaterServce";

    static final int DELAY = 60000;
    private boolean runFlag = false;
    private Updater updater;
    private YambaApplication yambaApplication;

    private DbHelper dbHelper;
    private SQLiteDatabase db;


    @Override
    public IBinder onBind(Intent intent) {
        return null; // used only for bound services, this is an unbound service
    }

    @Override
    public void onCreate() {
        super.onCreate();

        yambaApplication = (YambaApplication) getApplication();
        updater = new Updater();
        dbHelper = new DbHelper(this);

        Log.d(TAG, "onCreated");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        this.runFlag = true;
        updater.start();
        yambaApplication.setServiceRunning(true);

        Log.d(TAG, "onStarted");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.runFlag = false;
        updater.interrupt();
        updater = null;
        yambaApplication.setServiceRunning(false);

        Log.d(TAG, "onDestroyed");
    }
}
