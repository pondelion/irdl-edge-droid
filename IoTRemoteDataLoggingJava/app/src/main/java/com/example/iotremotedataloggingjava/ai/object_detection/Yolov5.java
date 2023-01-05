package com.example.iotremotedataloggingjava.ai.object_detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.iotremotedataloggingjava.ai.BaseTorchModel;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Yolov5 extends BaseTorchModel {

    private Context mContext = null;
    private final String MODEL_FILENAME = "yolov5s.torchscript.ptl";
    public static final String TAG = "Yolov5";

    public Yolov5(Context context) {
        super(context);
    }

    public List<ObjectDetectionResult> detect(Bitmap bitmap) {

        if (mModel == null) {
            loadLiteModel(MODEL_FILENAME);
        }

        int inputWidth = Yolov5PrePostProcessor.mInputWidth;
        int inputHeight = Yolov5PrePostProcessor.mInputHeight;
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                bitmap, inputWidth, inputHeight, true
        );

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                resizedBitmap, Yolov5PrePostProcessor.NO_MEAN_RGB, Yolov5PrePostProcessor.NO_STD_RGB
        );
        IValue[] outputTuple = mModel.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        float outputWidth = inputWidth;
        float outputHeight = inputHeight;
        float imgScaleX = (float)bitmap.getWidth() / Yolov5PrePostProcessor.mInputWidth;
        float imgScaleY = (float)bitmap.getHeight() / Yolov5PrePostProcessor.mInputHeight;
        float ivScaleX = (float)outputWidth / bitmap.getWidth();
        float ivScaleY = (float)outputHeight / bitmap.getHeight();

        final ArrayList<ObjectDetectionResult> results = Yolov5PrePostProcessor.outputsToNMSPredictions(
                outputs, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0, 0
        );

        return results;
    }
}
