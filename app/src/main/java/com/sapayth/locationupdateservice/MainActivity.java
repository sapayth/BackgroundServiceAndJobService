package com.sapayth.locationupdateservice;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 201;

    private Button mStartServiceButton, mStopServiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartServiceButton = findViewById(R.id.startButton);
        mStopServiceButton = findViewById(R.id.stopButton);

        mStartServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGettingLocation();
            }
        });

        mStopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopGettingLocation();
            }
        });
    }

    public void startGettingLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                startService(new Intent(MainActivity.this, LocationUpdateService.class));
            } else {
                JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                ComponentName componentName = new ComponentName(this,
                        LocationUpdateJobService.class);
                JobInfo jobInfo = new JobInfo.Builder(1, componentName)
                        .setPeriodic(8680000)
                        .build();
                jobScheduler.schedule(jobInfo);
                Log.e("Version:", "" + Build.VERSION.SDK_INT);
            }
        } else {
            requestLocationPermission();
        }
    }

    public void stopGettingLocation() {
        Toast.makeText(this, "stop", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            stopService(new Intent(MainActivity.this, LocationUpdateService.class));
        } else {
            JobScheduler jobScheduler = (JobScheduler)this.getSystemService(Context.JOB_SCHEDULER_SERVICE );
            jobScheduler.cancelAll();
        }
    }

    private void requestLocationPermission() {
        // Request the permission. The result will be received in onRequestPermissionResult().
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        // Permission has not been granted and must be requested.
        if (Build.VERSION.SDK_INT >= 23) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Location permission is needed to show your location",
                        Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the task you need to do.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                    startService(new Intent(MainActivity.this, LocationUpdateService.class));
                } else {
                    JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    ComponentName componentName = new ComponentName(this,
                            LocationUpdateJobService.class);
                    JobInfo jobInfo = new JobInfo.Builder(1, componentName)
                            .setPeriodic(8680000)
                            .build();
                    jobScheduler.schedule(jobInfo);
                    Log.e("Version:", "" + Build.VERSION.SDK_INT);
                }
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}