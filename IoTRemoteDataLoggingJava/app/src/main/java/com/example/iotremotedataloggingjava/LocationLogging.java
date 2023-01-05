package com.example.iotremotedataloggingjava;

import android.app.Service;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;

public class LocationLogging implements LocationListener {

    public static String TAG = "LocationLogging";
    private MQTTConnector mMQTTConnector;
    private boolean mLoggingEnabled = true;
    private Service mLoggingService;
    private LocationManager mLocationManager;
    private int mMinUpdateTimeMs = 20*1000;
    private int mMinUpdateDistanceM = 2;
    private boolean mIsServiceRunning = false;

    LocationLogging(MQTTConnector mqttConnector, LoggingService loggingService) {
        mMQTTConnector = mqttConnector;
        mLoggingService = loggingService;
        mLocationManager = (LocationManager) mLoggingService.getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, location.getLatitude() + ", " + location.getLongitude());
        if (mLoggingEnabled) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String datetimeNow = sdf.format(new Date());
            HashMap data = new HashMap();
            data.put("datetime", datetimeNow);
            data.put("lat", location.getLatitude());
            data.put("lng", location.getLongitude());
            data.put("device_name", AWSSettings.DEVICE_NAME);
            mMQTTConnector.publishJson(data, AWSSettings.AWS_IOT_LOCATION_TOPIC_NAME);
            toast(data.toString());
        }
    }

    public void enableLogging() {
        this.mLoggingEnabled = true;
    }

    public void disableLogging() {
        this.mLoggingEnabled = false;
    }

    public void startLocationLogging() {
        Log.d(TAG,"locationStart()");
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, mMinUpdateTimeMs, mMinUpdateDistanceM, this
        );
        mIsServiceRunning = true;
    }

    public void stopLocationLogging() {
        mLocationManager.removeUpdates(this);
        mIsServiceRunning = false;
    }

    private void toast(String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mLoggingService.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setMinUpdateTime(int timeMs) {
        this.mMinUpdateTimeMs = timeMs;
    }

    public void setMinUpdateDistanceM(int distanceM) {
        this.mMinUpdateDistanceM = distanceM;
    }

    public boolean isServiceRunning() {
        return mIsServiceRunning;
    }

}
