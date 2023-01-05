package com.example.iotremotedataloggingjava;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;

public class RtmpStream implements ConnectCheckerRtmp, SurfaceHolder.Callback {

    private RtmpCamera1 rtmpCamera1;
//    private String url = "rtmp://192.168.0.6:8000/live/test";
    private SurfaceView mSurfaceView;
    private Context mContext;

    RtmpStream(SurfaceView surfaceView, Context context) {
        mSurfaceView = surfaceView;
        mContext = context;

        rtmpCamera1 = new RtmpCamera1(surfaceView, this);
        rtmpCamera1.setReTries(10);

        surfaceView.getHolder().addCallback(this);
    }

    public void startStreaming(String url) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (!rtmpCamera1.isStreaming()) {
                    if (rtmpCamera1.isRecording()
                            || rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {
                        rtmpCamera1.startPreview();
                        rtmpCamera1.startStream(url);
                    }
                }
            }
        });
    }

    public void stopStreaming() {
        if (rtmpCamera1.isStreaming()) {
            rtmpCamera1.stopStream();
        }
        rtmpCamera1.stopPreview();
    }

    private void toast(String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        toast("Auth error");
        rtmpCamera1.stopStream();
    }

    @Override
    public void onAuthSuccessRtmp() {
        toast("Auth success");
    }

    @Override
    public void onConnectionFailedRtmp(final String reason) {
        if (rtmpCamera1.reTry(5000, reason, null)) {
            toast("Retry");
        } else {
            toast("Connection failed. " + reason);
            rtmpCamera1.stopStream();
        }
    }

    @Override
    public void onConnectionStartedRtmp(@NonNull String s) {
    }

    @Override
    public void onConnectionSuccessRtmp() {
        toast("Connection success");
    }

    @Override
    public void onDisconnectRtmp() {
        toast("Disconnected");
    }

    @Override
    public void onNewBitrateRtmp(long l) {
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        rtmpCamera1.startPreview();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        rtmpCamera1.stopRecord();
        if (rtmpCamera1.isStreaming()) {
            rtmpCamera1.stopStream();
        }
        rtmpCamera1.stopPreview();
    }

    public void startPreview() {
        rtmpCamera1.startPreview();
    }

    public void stopPreview() {
        rtmpCamera1.stopPreview();
    }

    public boolean isServiceRunning() {
        return rtmpCamera1.isStreaming();
    }
}