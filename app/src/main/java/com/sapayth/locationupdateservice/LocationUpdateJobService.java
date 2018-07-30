package com.sapayth.locationupdateservice;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class LocationUpdateJobService extends JobService {
    private long UPDATE_INTERVAL = 5 * 1000;  /* 5 secs */
    private static final String TAG = "JobService";
    private static final String CHANNEL_ID = "channelId";
    private FusedLocationProviderClient mFusedLocationClient;

    private boolean mIsThreadRunning = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.e(TAG, "Job created");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mIsThreadRunning = true;
            UpdateLocationTask task = new UpdateLocationTask();
            task.execute(UPDATE_INTERVAL);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mIsThreadRunning = false;
        Log.e(TAG, "Job stopped");
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Job started");
        return super.onStartCommand(intent, flags, startId);
    }

    private void showLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                double lat = location.getLatitude();
                                double lon = location.getLongitude();
                                String msg = "Lat: " + lat + ", Lon: " + lon;
                                Toast.makeText(LocationUpdateJobService.this, msg, Toast.LENGTH_SHORT).show();
                                sendNotification(msg);
                            } else {
                                Toast.makeText(LocationUpdateJobService.this, "Location not found, turn on GPS", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Location Failure", e.getMessage());
                        }
                    });
        }

    }

    private void sendNotification(String msg) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(101, mBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private class UpdateLocationThread extends Thread {
        private long UPDATE_INTERVAL = 5 * 1000;  /* 5 secs */

        public UpdateLocationThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (mIsThreadRunning) {
                try{
                    showLocation();
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {}
            }
        }
    }

    private class UpdateLocationTask extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(Long... longs) {
            while (mIsThreadRunning) {
                try {
                    showLocation();
                    Thread.sleep(longs[0]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
