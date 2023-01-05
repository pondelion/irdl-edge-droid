package com.example.iotremotedataloggingjava;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity";
    private LocationManager mLocationManager;
    private Button mBtnStartService;
    private Button mBtnStopService;
    private RtmpStream mRtmpStreamer;
    private static final int REQUEST_CODE_LOCATION = 1000;
    private static final int REQUEST_CODE_OVERLAY = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnStartService = (Button) findViewById(R.id.button_start_service);
        mBtnStopService = (Button) findViewById(R.id.button_stop_service);
        mBtnStopService.setEnabled(false);

        mBtnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPermissions()) {
                    return;
                }
                startService();
                mBtnStopService.setEnabled(true);
                mBtnStartService.setEnabled(false);
            }
        });
        mBtnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
                mBtnStopService.setEnabled(false);
                mBtnStartService.setEnabled(true);
            }
        });

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Button btn3 = (Button) findViewById(R.id.button_3);
//        mBtn4 = (Button) findViewById(R.id.button_4);
    }

    private void startService() {
        Intent intent = new Intent(getApplication(), LoggingService.class);
        startForegroundService(intent);
    }

    private void stopService() {
        Intent intent = new Intent(getApplication(), LoggingService.class);
        stopService(intent);
    }

    private boolean checkPermissions() {
        // Location
        if (mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "location manager Enabled");
        } else {
            Intent settingsIntent =
                    new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
            Log.d(TAG, "not gpsEnable, startActivity");
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, REQUEST_CODE_LOCATION);
            Log.d(TAG, "checkSelfPermission false");
            return false;
        }
        // Camera
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,}, REQUEST_CODE_CAMERA);
            Log.d(TAG, "checkSelfPermission false");
            return false;
        }
        // Overlay
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // The user granted the permission.
                    return;
//                    startService();
//                    mBtnStopService.setEnabled(true);
//                    mBtnStartService.setEnabled(false);
                } else {
                    // The user canceled the choice or denied the permission.
                }
                return;
            }
            case REQUEST_CODE_CAMERA: {
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (!Settings.canDrawOverlays(this)) {
                Log.d(TAG,"SYSTEM_ALERT_WINDOW permission not granted");
            }
        }
    }
}