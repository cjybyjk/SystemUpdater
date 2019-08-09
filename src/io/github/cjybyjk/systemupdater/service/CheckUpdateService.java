package io.github.cjybyjk.systemupdater.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import io.github.cjybyjk.systemupdater.R;
import io.github.cjybyjk.systemupdater.broadcast.UpdatesCheckReceiver;
import io.github.cjybyjk.systemupdater.downloader.DownloadClient;
import io.github.cjybyjk.systemupdater.helper.UpdatesDbHelper;
import io.github.cjybyjk.systemupdater.helper.UpdatesDownloadHelper;
import io.github.cjybyjk.systemupdater.utils.FileUtils;
import io.github.cjybyjk.systemupdater.utils.SystemInfoUtils;
import io.github.cjybyjk.systemupdater.model.Update;

public class CheckUpdateService extends Service {

    private static final String TAG = "CheckUpdateService";

    public class statusBinder extends Binder {
        public void setDownloadListener(UpdatesDownloadHelper.DownloadListener listener) {
            mDownloadListener = listener;
        }

        public void checkUpdate() {
            CheckUpdateService.this.checkUpdate();
        }
    }

    private SharedPreferences mSharedPreferences;
    private UpdatesDbHelper mUpdatesDbHelper;
    private UpdatesDownloadHelper.DownloadListener mDownloadListener;
    private LocalBroadcastManager mLocalBroadcastManager;
    private UpdatesCheckReceiver mReceiver;
    private Date mDate;

    public CheckUpdateService() {
    }

    @Override
    public void onCreate() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUpdatesDbHelper = new UpdatesDbHelper(this);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReceiver = new UpdatesCheckReceiver();
        mDate = new Date();
    }

    public int onStartCommand(Intent intent, int flags, final int startId) {
        if (intent == null || intent.getAction() == null || !intent.getAction().equals("check")) {
            long nowTime = mDate.getTime() / 1000;
            long lastCheckTime = mSharedPreferences.getLong("last_check", nowTime / 1000);
            long secondOfaDay = 86400;
            long checkTriggerTime = 0;
            switch (mSharedPreferences.getString("auto_check_interval", "daily")) {
                case "daily":
                    checkTriggerTime = lastCheckTime + secondOfaDay;
                    break;
                case "weekly":
                    checkTriggerTime = lastCheckTime + secondOfaDay * 7;
                    break;
                case "monthly":
                    checkTriggerTime = lastCheckTime + secondOfaDay * 30;
                    break;
                case "never":
                    return super.onStartCommand(intent, flags, startId);
            }
            if (checkTriggerTime < nowTime) {
                checkTriggerTime = nowTime + 10;
            }
            AlarmManager tManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
            Intent tUpdatesCheck = new Intent(this ,CheckUpdateService.class);
            tUpdatesCheck.setAction("check");
            PendingIntent tPendingIntent = PendingIntent.getService(this, 0, tUpdatesCheck, 0);
            tManager.set(AlarmManager.RTC_WAKEUP, checkTriggerTime * 1000, tPendingIntent);
        } else {
            mDownloadListener = new UpdatesDownloadHelper.DownloadListener() {
                @Override
                public void onFailureListener(boolean cancelled) {
                }

                @Override
                public void onResponseListener(int statusCode, String url, DownloadClient.Headers headers) {
                }

                @Override
                public void onSuccessListener(File destination) {
                    IntentFilter tIntentFilter = new IntentFilter();
                    tIntentFilter.addAction("CHECK_DONE");
                    mLocalBroadcastManager.registerReceiver(mReceiver, tIntentFilter);
                    Intent tIntent = new Intent("CHECK_DONE");
                    mLocalBroadcastManager.sendBroadcast(tIntent);
                }

                @Override
                public void onProgressChangedListener(long bytesRead, long contentLength, long speed, long eta, boolean done) {
                }
            };
            checkUpdate();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new statusBinder();
    }

    public void checkUpdate() {
        final String mDeviceName = SystemInfoUtils.getProperty("ro.product.device", "");
        final String mProductName = SystemInfoUtils.getProperty("ro.build.product", "");
        String updateUrl = mSharedPreferences.getString("update_channel", "");
        if (updateUrl.equals("")) {
            // 读取build.prop里定义的url
            updateUrl = SystemInfoUtils.getProperty(
                    getString(R.string.attr_prop_update_channel_url), "");
        }
        if (updateUrl.equals("unknown")) {
            // 读取默认url
            updateUrl = getString(R.string.attr_update_channel_url);
        }

        final File updateJsonFile = new File(getCacheDir().getAbsolutePath() + "/updates.json");
        updateJsonFile.delete(); // 删除旧的json
        DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
            @Override
            public void onFailure(final boolean cancelled) {
                Log.e(TAG, "Could not download updates list");
                if (mDownloadListener != null) {
                    mDownloadListener.onFailureListener(cancelled);
                }
            }

            @Override
            public void onResponse(int statusCode, String url, DownloadClient.Headers headers) {
                if (mDownloadListener != null) {
                    mDownloadListener.onResponseListener(statusCode, url, headers);
                }
            }

            @Override
            public void onSuccess(File destination) {
                Log.d(TAG, "List downloaded");
                mSharedPreferences.edit().putLong("last_check", mDate.getTime() / 1000).apply();
                String jsonString = FileUtils.readToString(updateJsonFile);
                try {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        String tDevice = jsonObject.getString("device");
                        String tType = jsonObject.getString("type");
                        if ((!tDevice.equals(mDeviceName) && !tDevice.equals(mProductName)) ||
                                !mSharedPreferences.getStringSet("update_type", null).contains(tType)
                                ) {
                            continue;
                        }
                        Update tUpdate = new Update();
                        tUpdate.setName(jsonObject.getString("name"));
                        tUpdate.setVersion(jsonObject.getString("version"));
                        tUpdate.setType(jsonObject.getString("type"));
                        if (jsonObject.getString("type").equals("full")) {
                            tUpdate.setRequirement(0);
                        } else {
                            tUpdate.setRequirement(jsonObject.getLong("requirement"));
                        }
                        tUpdate.setDownloadUrl(jsonObject.getString("url"));
                        tUpdate.setDescription(jsonObject.getString("description"));
                        tUpdate.setTimestamp(jsonObject.getLong("timestamp"));
                        tUpdate.setFileSize(jsonObject.getLong("file_size"));
                        tUpdate.setFileSHA1(jsonObject.getString("sha1"));
                        mUpdatesDbHelper.addUpdate(tUpdate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mUpdatesDbHelper.cleanUpdates(Long.parseLong(SystemInfoUtils.getProperty("ro.build.date.utc", "0")));
                if (mDownloadListener != null) {
                    mDownloadListener.onSuccessListener(destination);
                }
            }
        };

        final DownloadClient downloadClient;
        try {
            DownloadClient.Builder downloadBuilder = new DownloadClient.Builder();
            downloadBuilder.setUrl(updateUrl);
            downloadBuilder.setDestination(updateJsonFile);
            downloadBuilder.setDownloadCallback(callback);
            if (mDownloadListener != null) {
                downloadBuilder.setProgressListener((bytesRead, contentLength, speed, eta, done) -> mDownloadListener.onProgressChangedListener(bytesRead, contentLength, speed, eta, done));
            }
            downloadClient = downloadBuilder.build();
            downloadClient.start();
        } catch (IOException exception) {
            return;
        }
    }

    @Override
    public void onDestroy() {
        mUpdatesDbHelper.close();
    }

}
