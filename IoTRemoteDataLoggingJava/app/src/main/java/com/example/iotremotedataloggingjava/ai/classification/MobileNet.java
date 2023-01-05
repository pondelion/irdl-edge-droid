package com.example.iotremotedataloggingjava.ai.classification;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.iotremotedataloggingjava.ai.BaseTorchModel;

import org.pytorch.IValue;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class MobileNet extends BaseTorchModel {

    private final String MODEL_FILENAME = "mobilenet_v3_model_v5.ptl";
    public static final String TAG = "MobileNet";

    public MobileNet(Context context) {
        super(context);
    }

    public String classify(Bitmap bitmap) {

        if (mModel == null) {
            loadLiteModel(MODEL_FILENAME);
        }

        Log.d(TAG, "start predict");

        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        final Tensor outputTensor = mModel.forward(IValue.from(inputTensor)).toTensor();

        final float[] scores = outputTensor.getDataAsFloatArray();

        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }

        String className = ImageNetClasses.IMAGENET_CLASSES[maxScoreIdx];

        Log.d(TAG, "Predicted label : " + className);

        return className;
    }
}
