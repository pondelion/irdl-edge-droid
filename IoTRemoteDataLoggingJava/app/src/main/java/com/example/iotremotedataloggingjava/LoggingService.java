package com.example.iotremotedataloggingjava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LoggingService extends Service {

    public static String TAG = "LoggingService";
    private MQTTConnector mMQTTConnector;
    private LocationLogging mLocationLogging;
    private SensorLogging mSensorLogging;
    private Camera mCamera;
    private CameraPreview mPreview;
    private RtmpStream mRtmpStream;
    private SurfaceView mSurfaceViewRtmp;
    private int mCnt = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mMQTTConnector = new MQTTConnector(this);
        mLocationLogging = new LocationLogging(mMQTTConnector, this);
        mSensorLogging = new SensorLogging(mMQTTConnector, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        String channelId = "default";
        String title = context.getString(R.string.app_name);

        int requestCode = 1;
//        PendingIntent pendingIntent =
//                PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                channelId, title , NotificationManager.IMPORTANCE_DEFAULT);

        if(notificationManager != null) {
            notificationManager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(context, channelId)
                    .setContentTitle(title)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentText("Logging Service")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .build();

            startForeground(1, notification);
        }

        mMQTTConnector.connectSubscribe(new RemoteCommandHandler(this));

//        mLocationLogging.startLocationLogging();
//        startSensorLogging(null);

//        mCamera = getCameraInstance();
//        mPreview = new CameraPreview(getApplicationContext(), mCamera);


//        mSurfaceViewRtmp = new SurfaceView(this.getApplicationContext());
//        wm.addView(mSurfaceViewRtmp, params);
//        mRtmpStreamer = new RtmpStreamer(mSurfaceViewRtmp, this.getApplicationContext());
////        mRtmpStreamer.startStreaming("rtmp://192.168.0.6:8000/live/test");

//        startRtspStreaming("rtmp://192.168.0.6:1935/live/test");

        Log.d(TAG, getServiceStatus().toString());

        (new Timer()).scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                Log.d(TAG, "Hello!");
//                if (mCnt % 4 == 0) {
//                    addCameraView();
//                } else if (mCnt % 4 == 1) {
//                    removeCameraView();
//                } else if (mCnt % 4 == 2) {
//                    addRtmpStreamView();
//                } else if (mCnt % 4 == 3) {
//                    removeRtmpStreamView();
//                }
                mCnt++;
            }
        }, 3000, 10000);

        return START_NOT_STICKY;
        //return START_STICKY;
        //return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        mLocationLogging.stopLocationLogging();
        stopSensorLogging();
        mMQTTConnector.disconnect();
        stopCameraCapture();
        stopRtspStreaming();
        stopSelf();
    }

    public void startSensorLogging(List<Integer> sensorTypes) {
        mSensorLogging.startSensorLogging(sensorTypes);
    }

    public void stopSensorLogging() {
        mSensorLogging.stopSensorLogging();
    }

    public void startLocationLogging() {
        mLocationLogging.startLocationLogging();
    }

    public void stopLocationLogging() {
        mLocationLogging.stopLocationLogging();
    }

    public void startCameraCapture(Camera.PreviewCallback captureCallback) {
        if (mSurfaceViewRtmp != null || mPreview == null) {
            removeRtmpStreamView();
            addCameraView();
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mPreview != null) {
                        mPreview.startPreview(captureCallback);
                    } else {
                        Log.w(TAG, "mPreview is null, skipping capture");
                    }
                }
            }, 3000);
        } else {
            mPreview.startPreview(captureCallback);
        }
    }

    public void stopCameraCapture() {
        if (mPreview != null) {
            mPreview.stopPreview();
        }
    }

    public void startRtspStreaming(String url) {
        if (mPreview != null || mSurfaceViewRtmp == null) {
            removeCameraView();
            addRtmpStreamView();
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mRtmpStream != null) {
                        mRtmpStream.startStreaming(url);
                    } else {
                        Log.w(TAG, "mRtmpStream is null, skipping start streaming");
                    }
                }
            }, 3000);
        } else {
            mRtmpStream.startStreaming(url);
        }
    }

    public void stopRtspStreaming() {
        if (mRtmpStream != null) {
            mRtmpStream.stopStreaming();
        }
    }

    public MQTTConnector getMQTTConnector() { return mMQTTConnector; }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void removeCameraView() {
        if (mCamera != null) {
            WindowManager wm = (WindowManager) getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            mPreview.stopPreview();
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
            wm.removeView(mPreview);
            mPreview = null;
        }
    }

    private void addCameraView() {
        if (mCamera == null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mCamera = getCameraInstance();
                    mPreview = new CameraPreview(getApplicationContext(), mCamera);

                    WindowManager wm = (WindowManager) getApplicationContext()
                            .getSystemService(Context.WINDOW_SERVICE);
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                            PixelFormat.TRANSLUCENT);

                    if (LoggingSettings.DEBUG) {
                        params.height = 600;
                        params.width = 600;
                    } else {
                        params.height = 1;
                        params.width = 1;
                    }

                    wm.addView(mPreview, params);
                }
            });
        }
    }

    private void removeRtmpStreamView() {
        if (mSurfaceViewRtmp != null) {
            WindowManager wm = (WindowManager) getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            mRtmpStream.stopPreview();
            mRtmpStream.stopStreaming();
            wm.removeView(mSurfaceViewRtmp);
            mSurfaceViewRtmp = null;
            mRtmpStream = null;
        }
    }

    private void addRtmpStreamView() {
        if (mSurfaceViewRtmp == null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    WindowManager wm = (WindowManager) getApplicationContext()
                            .getSystemService(Context.WINDOW_SERVICE);
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                            PixelFormat.TRANSLUCENT);
                    if (LoggingSettings.DEBUG) {
                        params.height = 600;
                        params.width = 600;
                    } else {
                        params.height = 1;
                        params.width = 1;
                    }
                    params.x = 600;
                    params.y = 600;
                    mSurfaceViewRtmp = new SurfaceView(LoggingService.this.getApplicationContext());
                    wm.addView(mSurfaceViewRtmp, params);
                    mRtmpStream = new RtmpStream(mSurfaceViewRtmp, LoggingService.this.getApplicationContext());
                }
            });
        }
    }

    public HashMap getServiceStatus() {
        HashMap status = new HashMap();

        status.put("location", mLocationLogging.isServiceRunning() ? "on" : "off");
        status.put("sensor", mSensorLogging.isServiceRunning() ? "on" : "off");
        status.put("streaming", mRtmpStream == null ? "off" : (mRtmpStream.isServiceRunning() ? "on" : "off"));

        return status;
    }
}
