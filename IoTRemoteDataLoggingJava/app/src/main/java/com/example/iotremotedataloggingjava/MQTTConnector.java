package com.example.iotremotedataloggingjava;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.HashMap;

public class MQTTConnector {

    public static String TAG = "MQTTConnector";
    private AWSIotMqttManager mMQTTManager;
    private Context mContext;
    private boolean mIsConnected = false;

    MQTTConnector(@NonNull Context context) {
        mMQTTManager = new AWSIotMqttManager(AWSSettings.AWS_IOT_THING_NAME, AWSSettings.AWS_IOT_ENDPOINT);
        mContext = context;
    }

    public void connectSubscribe(AWSIotMqttNewMessageCallback messageReceivedCallback) {
        String keyStorePath = mContext.getFilesDir().getAbsolutePath();
        boolean isPresent = AWSIotKeystoreHelper.isKeystorePresent(keyStorePath, AWSSettings.KEY_STORE_NAME);
        if (!isPresent) {
            try {
                saveCertificateAndPrivateKey(keyStorePath);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        KeyStore keyStore = AWSIotKeystoreHelper.getIotKeystore(
                AWSSettings.CERT_ID,
                keyStorePath,
                AWSSettings.KEY_STORE_NAME,
                AWSSettings.KEY_STORE_PASSWORD
        );
        this.mMQTTManager.connect(keyStore, new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                Log.d(TAG, "AWSIotMqttClientStatusCallback#onStatusChanged : " + status.toString());
                if (status == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected) {
                    subscribe(AWSSettings.AWS_IOT_COMMAND_TOPIC_NAME, messageReceivedCallback);
                    Log.d(TAG, "subscribed to " + AWSSettings.AWS_IOT_COMMAND_TOPIC_NAME);
                    mIsConnected = true;
                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                    mIsConnected = false;
                }
            }
        });
    }

    public void disconnect() {
        this.mMQTTManager.disconnect();
    }

    private void saveCertificateAndPrivateKey(@NonNull String keyStorePath) throws Exception {
        final String certFile = AWSSettings.CERT_FILE;
        final String certStr = readAssetFile(certFile);
        Log.d(TAG, "certStr : " + certStr);
        final String privKeyFile = AWSSettings.PRIVATE_KEY_FILE;
        final String privKeyStr = readAssetFile(privKeyFile);
        Log.d(TAG, "privKeyStr : " + privKeyStr);
        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(
                AWSSettings.CERT_ID,
                certStr,
                privKeyStr,
                keyStorePath,
                AWSSettings.KEY_STORE_NAME,
                AWSSettings.KEY_STORE_PASSWORD
        );
    }

    private String readAssetFile(String fileName) throws IOException {
        StringBuilder contentsStr = new StringBuilder();
        InputStream fIn = null;
        InputStreamReader isr = null;
        BufferedReader input = null;
        try {
            fIn = mContext.getResources().getAssets().open(fileName, Context.MODE_WORLD_READABLE);
            isr = new InputStreamReader(fIn);
            input = new BufferedReader(isr);
            String line = "";
            while ((line = input.readLine()) != null) {
                contentsStr.append(line + "\n");
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            try {
                if (isr != null)
                    isr.close();
                if (fIn != null)
                    fIn.close();
                if (input != null)
                    input.close();
            } catch (Exception e2) {
                e2.getMessage();
            }
        }
        return contentsStr.toString();
    }

    private void subscribe(String topic, AWSIotMqttNewMessageCallback messageReceivedCallback) {
        if (this.mMQTTManager == null) {
            return;
        }
        this.mMQTTManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0, messageReceivedCallback);
    }

    public void publishString(@NonNull String msg, @NonNull String topic) {
        if (this.mMQTTManager == null | !mIsConnected) {
            return;
        }
        this.mMQTTManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
    }

    public void publishJson(@NonNull HashMap data, @NonNull String topic) {
        if (this.mMQTTManager == null | !mIsConnected) {
            return;
        }
        JSONObject json = new JSONObject(data);
        this.mMQTTManager.publishData(json.toString().getBytes(), topic, AWSIotMqttQos.QOS0);
    }

    public void publishJson(@NonNull JSONObject data, @NonNull String topic) {
        if (this.mMQTTManager == null | !mIsConnected) {
            return;
        }
        this.mMQTTManager.publishData(data.toString().getBytes(), topic, AWSIotMqttQos.QOS0);
    }
}
