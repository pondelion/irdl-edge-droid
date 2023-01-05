package com.example.iotremotedataloggingjava.ai;

import android.content.Context;
import android.util.Log;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BaseTorchModel {

    protected Context mContext = null;
    protected Module mModel = null;

    public BaseTorchModel(Context context) {
        mContext = context;
    }

    public void loadLiteModel(String modelFileName) {
        Log.d("loadModel", "Start model loading");
        try {
            if (mModel == null) {
                mModel = LiteModuleLoader.load(
                        assetFilePath(mContext, modelFileName)
                );
            }
        } catch (
                IOException e) {
            Log.e("loadModel", "Error reading assets", e);
            return;
        }
    }

    protected String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}
