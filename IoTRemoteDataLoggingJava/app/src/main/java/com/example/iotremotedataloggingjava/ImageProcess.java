package com.example.iotremotedataloggingjava;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageProcess {

    public static Bitmap getBitmapImageFromYUV(byte[] data, int width, int height) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return bmp;
    }

    public static Bitmap saveCameraBytesToPng(byte[] bytes, Camera camera, int resizeW, int resizeH, String fileptah) {
        int w = camera.getParameters().getPreviewSize().width;
        int h = camera.getParameters().getPreviewSize().height;
        Bitmap bmp = ImageProcess.getBitmapImageFromYUV(bytes, w, h);
        Bitmap bmpScaled = Bitmap.createScaledBitmap(bmp, resizeW, resizeH, true);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap bmpRotated = Bitmap.createBitmap(bmpScaled, 0, 0, bmpScaled.getWidth(), bmpScaled.getHeight(), matrix, true);
        try (FileOutputStream out = new FileOutputStream(fileptah)) {
            bmpRotated.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmpRotated;
    }
}
