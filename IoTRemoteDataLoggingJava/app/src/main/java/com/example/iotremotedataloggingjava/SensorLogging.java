package com.example.iotremotedataloggingjava;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SensorLogging implements SensorEventListener {

    public static String TAG = "SensorLogging";
    private MQTTConnector mMQTTConnector;
    private boolean mLoggingEnabled = true;
    private Service mLoggingService;
    private SensorManager mSensorManager;
    private List<Sensor> mSensors = new ArrayList<>();
    public static final List<Integer> DEFAULT_SENSOR_TYPES = Arrays.asList(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_ORIENTATION,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_TEMPERATURE
    );
    private HashMap<Integer, JSONObject> mLatestSensorValues = new HashMap();
    private HashMap<Integer, Boolean> mPublishedMap = new HashMap();
    private static final float PUBLISH_INTERVAL_SECS = 10;
    private Timer mPublishTimer;
    private boolean mIsServiceRunning = false;

    SensorLogging(MQTTConnector mqttConnector, LoggingService loggingService) {
        mMQTTConnector = mqttConnector;
        mLoggingService = loggingService;
        mSensorManager = (SensorManager)mLoggingService.getSystemService(Context.SENSOR_SERVICE);
        mPublishTimer = new Timer();
    }

    public void startSensorLogging(List<Integer> sensorTypes) {
        mSensors.clear();
        if (sensorTypes== null) {
//            mSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            DEFAULT_SENSOR_TYPES.forEach(sensorType -> {
                Sensor defaultSensor = mSensorManager.getDefaultSensor(sensorType);
                if (mSensorManager.getDefaultSensor(sensorType) != null){
                    mSensors.add(defaultSensor);
                } else {
                    Log.w(TAG, "Sensor with " + sensorType.toString() + " not found");
                }
            });
        } else {
            sensorTypes.forEach(sensorType -> {
                Sensor defaultSensor = mSensorManager.getDefaultSensor(sensorType);
                if (mSensorManager.getDefaultSensor(sensorType) != null){
                    mSensors.add(defaultSensor);
                } else {
                    Log.w(TAG, "Sensor with " + sensorType.toString() + " not found");
                }
            });
        }

        mSensors.forEach(sensor -> {
            mSensorManager.registerListener(
                    (SensorEventListener) this,
                    sensor,
//                    SensorManager.SENSOR_DELAY_FASTEST
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        });

        mPublishTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                publishSensorData();
            }
        }, 0, (long) (1000*PUBLISH_INTERVAL_SECS));

        mIsServiceRunning = true;
    }

    public void stopSensorLogging() {
        mPublishTimer.cancel();
        if (mSensors == null) {
            return;
        }
        mSensors.forEach(sensor -> {
            mSensorManager.unregisterListener(
                    (SensorEventListener) this,
                    sensor
            );
        });
        mIsServiceRunning = false;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            String datetimeNow = sdf.format(new Date());
            JSONObject jsonData = new JSONObject();
            jsonData.put("datetime", datetimeNow);
            jsonData.put("value", sensorEvent.values[0]);
            jsonData.put("sensor_name", sensorEvent.sensor.getName());
            jsonData.put("vendor", sensorEvent.sensor.getVendor());
            jsonData.put("version", sensorEvent.sensor.getVersion());
            jsonData.put("type", sensorEvent.sensor.getType());
            jsonData.put("max_range", sensorEvent.sensor.getMaximumRange());
            jsonData.put("resolution", sensorEvent.sensor.getResolution());
            jsonData.put("power", sensorEvent.sensor.getPower());
            jsonData.put("min_delay", sensorEvent.sensor.getMinDelay());
            jsonData.put("device_name", AWSSettings.DEVICE_NAME);
//            Log.d(TAG, jsonData.toString());
            mLatestSensorValues.put(sensorEvent.sensor.getType(), jsonData);
            mPublishedMap.put(sensorEvent.sensor.getType(), false);
        } catch (JSONException err){
            Log.d(TAG, err.toString());
        }
//        Log.d(TAG, sensorEvent.sensor.toString() + ":" + String.valueOf(sensorEvent.values[0]) );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void publishSensorData() {
        mSensors.forEach(sensor -> {
            if (!mPublishedMap.containsKey(sensor.getType())) {
                return;
            }
            if (!mLatestSensorValues.containsKey(sensor.getType())) {
                return;
            }
            if (mPublishedMap.get(sensor.getType())) {
                // This sensor data is already published.
                return;
            }
            mMQTTConnector.publishJson(
                    mLatestSensorValues.get(sensor.getType()), AWSSettings.AWS_IOT_SENSOR_TOPIC_NAME
            );
            Log.d(TAG, "published : " + mLatestSensorValues.get(sensor.getType()).toString());
            mPublishedMap.put(sensor.getType(), true);  // Mark this sensor data is publisehd.
        });
    }

    public boolean isServiceRunning() {
        return mIsServiceRunning;
    }
}
