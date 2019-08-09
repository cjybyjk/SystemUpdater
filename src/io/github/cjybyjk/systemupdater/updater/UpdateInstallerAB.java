package io.github.cjybyjk.systemupdater.updater;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

// TODO: finish this after others work
public class UpdateInstallerAB {
    static final String TAG = "UpdateInstallerAB";

    UpdateEngine mUpdateEngine;
    UpdateEngineCallback mUpdateEngineCallback = new UpdateEngineCallback() {
        @Override
        public void onStatusUpdate(int status, float percent) {
            if (status == UpdateEngine.UpdateStatusConstants.DOWNLOADING) {// 回调状态，升级进度
                DecimalFormat df = new DecimalFormat("#");
                String progress = df.format(percent * 100);
                Log.d(TAG, "update progress: " + progress);

            }
        }

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {// 回调状态
                Log.d(TAG, "UPDATE SUCCESS!");
            }
        }
    };

    public UpdateInstallerAB(Context context) throws MalformedURLException, ClassNotFoundException {
        mUpdateEngine = new UpdateEngine();
    }

    // 读取payload_properties.txt
    public static String[] getPayloadProperties(String pathPayloadProp) {
        try {
            File file = new File(pathPayloadProp);
            InputStreamReader is = new InputStreamReader(new FileInputStream(file));
            BufferedReader br = new BufferedReader(is);
            List<String> lines = new ArrayList<String>();
            String line = null;
            while ((line = br.readLine()) != null) {
                Log.d(TAG, "getPayloadProperties line: " + line);
                lines.add(line);
            }
            br.close();
            is.close();
            return lines.toArray(new String[lines.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startUpdateSystem(String pathPayloadBin, String pathPayloadProp) {
        Uri uri = Uri.fromFile(new File(pathPayloadBin));
        mUpdateEngine.bind(mUpdateEngineCallback);// 绑定callback
        mUpdateEngine.applyPayload(uri.toString(), 0l, 0l, getPayloadProperties(pathPayloadProp));// 进行升级
    }

}