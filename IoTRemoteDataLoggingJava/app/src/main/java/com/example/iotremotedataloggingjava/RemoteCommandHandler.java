package com.example.iotremotedataloggingjava;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

public class RemoteCommandHandler implements AWSIotMqttNewMessageCallback {
    public static final String TAG = "RemoteCommandHandler";
    private LoggingService mLoggingService;
    private S3Storage mS3Storage;

    RemoteCommandHandler(LoggingService loggingService) {
        mLoggingService = loggingService;
        mS3Storage = new S3Storage(mLoggingService.getApplicationContext());
    }

    @Override
    public void onMessageArrived(String topic, byte[] data) {
        final String msgStr = new String(data, Charset.forName("UTF-8"));
        Log.d(TAG, topic + ":" + msgStr);
        JSONObject msgJson;
        try {
            msgJson = new JSONObject(msgStr);
            //Log.d(TAG, "msgJson.meeesgae : " + msgJson.getString("message"));
            if (msgJson.has("cmd")) {
//                text += "\n" + msgJson.getString("cmd");
                executeCommand(msgJson.getString("cmd"), msgJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void executeCommand(@NonNull String cmd, JSONObject msgJson) {
        switch (cmd) {
            case "TAKE_PICTURE":
                Log.d(TAG, "[TAKE_PICTURE] starting camera");
                mLoggingService.startCameraCapture(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        Log.d(TAG, "onPreviewFrame : " + mLoggingService.getFilesDir());
                        mLoggingService.stopCameraCapture();
                        final String tmpFilePath = mLoggingService.getFilesDir() + "/camera_image.png";
                        ImageProcess.saveCameraBytesToPng(bytes, camera, 224, 224, tmpFilePath);
                        if (msgJson.has("s3_filepath")) {
                            try {
                                mS3Storage.saveFile(tmpFilePath, msgJson.getString("s3_filepath"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                break;
            case "START_LOGGING":
                Log.d(TAG, "[START_LOGGING] starting logging");
                if (!msgJson.has("target")) {
                    Log.d(TAG, "[START_LOGGING] target is not specified, ignoring command");
                    break;
                }
                try {
                    switch (msgJson.getString("target")) {
                        case "location":
                            mLoggingService.startLocationLogging();
                            break;
                        case "sensor":
                            mLoggingService.startSensorLogging(null);
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "STOP_LOGGING":
                Log.d(TAG, "[STOP_LOGGING] stopping logging");
                if (!msgJson.has("target")) {
                    Log.d(TAG, "[START_LOGGING] target is not specified, ignoring command");
                    break;
                }
                try {
                    switch (msgJson.getString("target")) {
                        case "location":
                            mLoggingService.stopLocationLogging();
                            break;
                        case "sensor":
                            mLoggingService.stopSensorLogging();
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "BEEP":
                ToneGenerator toneGenerator
                        = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                break;
            case "START_STREAMING":
                Log.d(TAG, "[START_STREAMING] starting streaming");
                if (!msgJson.has("url")) {
                    Log.d(TAG, "[START_STREAMING] url is not specified, ignoring command");
                    break;
                }
                try {
                    mLoggingService.startRtspStreaming(msgJson.getString("url"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "STOP_STREAMING":
                Log.d(TAG, "[STOP_STREAMING] stopping streaming");
                try {
                    mLoggingService.stopRtspStreaming();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "GET_STATUS":
                HashMap status = mLoggingService.getServiceStatus();
                Log.d(TAG, "GET_STATUS : " + msgJson.toString());
                Log.d(TAG, status.toString());
                HashMap data = new HashMap();
                try {

                    data.put("cmd_id", msgJson.getString("cmd_id"));
                    data.put("status", status);

                    mLoggingService.getMQTTConnector().publishJson(data, AWSSettings.AWS_IOT_CENTRAL_SERVER_TOPIC_NAME);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
