package com.example.iotremotedataloggingjava.ai.segmentation;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.example.iotremotedataloggingjava.ai.BaseTorchModel;

import org.pytorch.IValue;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.Map;

public class DeepLab extends BaseTorchModel {

    private final String MODEL_FILENAME = "deeplabv3_scripted_optimized_v5.ptl";
    public static final String TAG = "DeepLabV3";

    public DeepLab(Context context) {
        super(context);
    }

    public Bitmap segment(Bitmap bitmap) {

        if (mModel == null) {
            loadLiteModel(MODEL_FILENAME);
        }

        final int CLASSNUM = 21;
        final int DOG = 12;
        final int PERSON = 15;
        final int SHEEP = 17;

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        final float[] inputs = inputTensor.getDataAsFloatArray();

        final long startTime = SystemClock.elapsedRealtime();
        Map<String, IValue> outTensors = mModel.forward(IValue.from(inputTensor)).toDictStringKey();
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d(TAG,  "inference time (ms): " + inferenceTime);

        final Tensor outputTensor = outTensors.get("out").toTensor();
        final float[] scores = outputTensor.getDataAsFloatArray();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] intValues = new int[width * height];
        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                int maxi = 0, maxj = 0, maxk = 0;
                double maxnum = -Double.MAX_VALUE;
                for (int i = 0; i < CLASSNUM; i++) {
                    float score = scores[i * (width * height) + j * width + k];
                    if (score > maxnum) {
                        maxnum = score;
                        maxi = i; maxj = j; maxk = k;
                    }
                }
                if (maxi == PERSON)
                    intValues[maxj * width + maxk] = 0xFFFF0000;
                else if (maxi == DOG)
                    intValues[maxj * width + maxk] = 0xFF00FF00;
                else if (maxi == SHEEP)
                    intValues[maxj * width + maxk] = 0xFF0000FF;
                else
                    intValues[maxj * width + maxk] = 0xFF000000;
            }
        }

        Bitmap bmpSegmentation = Bitmap.createScaledBitmap(bitmap, width, height, true);
        Bitmap outputBitmap = bmpSegmentation.copy(bmpSegmentation.getConfig(), true);
        outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
        final Bitmap transferredBitmap = Bitmap.createScaledBitmap(outputBitmap, bitmap.getWidth(), bitmap.getHeight(), true);

        return transferredBitmap;
    }
}
