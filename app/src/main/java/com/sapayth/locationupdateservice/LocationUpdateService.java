package com.sapayth.locationupdateservice;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationUpdateService extends Service implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String TAG = "Started service";

    private static final int LOCATION_REQUEST_CODE = 201;
    private static final String CHANNEL_ID = "channelId";
    private FusedLocationProviderClient mFusedLocationClient;

    private boolean mIsThreadRunning = false;

    public LocationUpdateService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "service created");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mIsThreadRunning = true;
            Thread t1 = new Thread(new UpdateLocationThread("t1"));
            t1.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsThreadRunning = false;
        Log.e(TAG, "service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "service started");
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
                                Toast.makeText(LocationUpdateService.this, msg, Toast.LENGTH_SHORT).show();
                                sendNotification(msg);
                            } else {
                                Toast.makeText(LocationUpdateService.this, "Location not found, turn on GPS", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the task you need to do.
                showLocation();
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
            }
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
}