package com.example.iotremotedataloggingjava;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;


public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    public static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
//            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
//                @Override
//                public void onPreviewFrame(byte[] bytes, Camera camera) {
//                    Log.d(TAG, "onPreviewFrame");
//                }
//            });
//            mCamera.startPreview();
            mCamera.setDisplayOrientation(90);
        } catch (IOException e) {
            Log.d("CameraPreview", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d("CameraPreview", "Error starting camera preview: " + e.getMessage());
        }
    }

    public void startPreview(Camera.PreviewCallback previewCallback) {
        mCamera.setPreviewCallback(previewCallback);
        mCamera.startPreview();
    }

    public void stopPreview() {
        mCamera.stopPreview();
    }

    public void takePicture(Camera.PictureCallback captureCallback) {
        mCamera.takePicture(null, null, captureCallback);
    }
}
