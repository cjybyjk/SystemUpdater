package io.github.cjybyjk.systemupdater.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.github.cjybyjk.systemupdater.R;
import io.github.cjybyjk.systemupdater.downloader.DownloadClient;
import io.github.cjybyjk.systemupdater.helper.UpdatesDbHelper;
import io.github.cjybyjk.systemupdater.helper.UpdatesDownloadHelper;
import io.github.cjybyjk.systemupdater.utils.DataFormatUtils;
import io.github.cjybyjk.systemupdater.utils.FileUtils;
import io.github.cjybyjk.systemupdater.model.Update;
import io.github.cjybyjk.systemupdater.model.UpdateStatus;
import io.github.cjybyjk.systemupdater.utils.NotificationUtils;

public class DownloadManageService extends Service {
    private static final String TAG = "DownloadManageService";
    // 通知id从这里向上累加
    private static final int NotificationIdBase = 3;

    private SharedPreferences mSharedPreferences;

    public class downloadBinder extends Binder {
        public void setCallback(DownloadStatusCallback callback) {
            mCallback = callback;
        }

        public void addDownload(Update update) {
            DownloadManageService.this.addDownload(update);
        }

        public void startDownload(Update update) {
            DownloadManageService.this.startDownload(update);
        }

        public void stopDownload(Update update) {
            DownloadManageService.this.stopDownload(update);
        }

        public void resumeDownload(Update update) {
            DownloadManageService.this.resumeDownload(update);
        }

        public void cancelDownload(Update update) {
            DownloadManageService.this.cancelDownload(update);
        }

        public void verifyDownload(Update update) {
            verifyUpdate(update);
        }
    }

    public interface DownloadStatusCallback {
        void progressCallback(String sha1, int progress, String speed);
        void statusCallback(String sha1, int status);
    }

    private UpdatesDownloadHelper mUpdatesDownloadHelper;
    private UpdatesDbHelper mUpdatesDbHelper;
    private DownloadStatusCallback mCallback;
    private PowerManager.WakeLock mWakeLock;
    private Map<String, Integer> mMapNotificationId;

    public DownloadManageService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new downloadBinder();
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        mUpdatesDownloadHelper = new UpdatesDownloadHelper();
        mUpdatesDbHelper = new UpdatesDbHelper(this);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mMapNotificationId = new HashMap<>();
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(true);
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String mAction = intent.getAction();
            String sha1 = intent.getStringExtra("sha1");
            if (sha1 != null) {
                final Update tUpdate = mUpdatesDbHelper.getUpdate(sha1);
                if (mAction != null && mAction.equals("add_start_resume")) {
                    addDownload(tUpdate);
                    if (tUpdate.getStatus() != UpdateStatus.DOWNLOADED && tUpdate.getStatus() != UpdateStatus.VERIFIED) {
                        if (tUpdate.getStatus() == UpdateStatus.DOWNLOAD_PAUSED || tUpdate.getStatus() == UpdateStatus.DOWNLOADING) {
                            resumeDownload(tUpdate);
                        } else if (tUpdate.getStatus() == UpdateStatus.VERIFYING || tUpdate.getStatus() == UpdateStatus.VERIFIED) {
                            verifyUpdate(tUpdate);
                        } else {
                            startDownload(tUpdate);
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY;
    }

    public void addDownload(final Update update) {
        final String tSHA1 = update.getFileSHA1();
        update.setOnStatusChangedCallback(status -> {
            mUpdatesDbHelper.changeUpdateStatus(update);
            if (mCallback != null) {
                mCallback.statusCallback(tSHA1, status);
            }
        });
        update.setOnProgressChangedCallback((progress, speed) -> {
            mUpdatesDbHelper.changeUpdateDownloadProgress(update, progress);
            if (mCallback != null) {
                mCallback.progressCallback(tSHA1, progress, DataFormatUtils.FormatFileSize(speed) + "/s");
            }
        });
        if (mUpdatesDownloadHelper.containsSHA1(tSHA1))
            return;
        if (!mMapNotificationId.containsKey(tSHA1))
            mMapNotificationId.put(tSHA1, NotificationIdBase + mMapNotificationId.size());
            String tUserAgent = mSharedPreferences.getString("user_agent", getString(R.string.attr_user_agent));
            if (update.getFilePath().equals("")) {
                String tStoragePath = mSharedPreferences.getString("download_path", "");
                if (tStoragePath.equals("")) {
                    tStoragePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                }
                String tFileName = update.getName() + "_" +
                        update.getVersion() + "_" + update.getType() + ".zip";
                if (!tStoragePath.endsWith("/")) tStoragePath += "/";
                update.setFilePath(tStoragePath + tFileName);
                mUpdatesDbHelper.changeUpdateFilePath(update);
            }
            mUpdatesDownloadHelper.addDownload(update, update.getFilePath(), tUserAgent, new UpdatesDownloadHelper.DownloadListener() {
            @Override
            public void onFailureListener(boolean cancelled) {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                destroyNotification(mMapNotificationId.get(tSHA1));
                if (!cancelled) {
                    Log.e(TAG, "Could not download update");
                    mUpdatesDownloadHelper.removeDownloadClient(tSHA1);
                    addDownload(update);
                    update.setStatus(UpdateStatus.DOWNLOAD_FAILED);
                    NotificationUtils.showNotification(getApplicationContext(), "update_download",
                            mMapNotificationId.get(tSHA1), getString(R.string.notification_titie_update_downloading_failed),
                            String.format(getString(R.string.notification_text_update_downloading_failed),
                                    update.getName() + " " + update.getVersion()));
                }

            }
            @Override
            public void onResponseListener(int statusCode, String url, DownloadClient.Headers headers) {
            }
            @Override
            public void onSuccessListener(File destination) {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                update.setStatus(UpdateStatus.DOWNLOADED);
                destroyNotification(mMapNotificationId.get(tSHA1));
                verifyUpdate(update);
            }
            @Override
            public void onProgressChangedListener(final long bytesRead, final long contentLength, final long speed, long eta, boolean done) {
                int progress = (int) (100 * bytesRead / contentLength);
                update.setDownloadProgress(progress, speed);
                startForeground(mMapNotificationId.get(tSHA1),NotificationUtils.buildNotification(
                        getApplicationContext(),"update_download",
                        getString(R.string.notification_titie_update_downloading),
                        String.format(getString(R.string.notification_text_update_downloading),
                                update.getName() + " " + update.getVersion(), progress + "%"),
                        100, progress,false));
            }
        });
    }

    public void startDownload(Update update) {
        DownloadClient tDownloadClient = mUpdatesDownloadHelper.findDownloadClient(update.getFileSHA1());
        update.setStatus(UpdateStatus.DOWNLOADING);
        mWakeLock.acquire();
        tDownloadClient.start();
    }

    public void stopDownload(Update update) {
        DownloadClient tDownloadClient = mUpdatesDownloadHelper.findDownloadClient(update.getFileSHA1());
        update.setStatus(UpdateStatus.DOWNLOAD_PAUSED);
        tDownloadClient.cancel();
        mUpdatesDownloadHelper.removeDownloadClient(update.getFileSHA1());
        addDownload(update);
    }

    public void resumeDownload(Update update) {
        DownloadClient tDownloadClient = mUpdatesDownloadHelper.findDownloadClient(update.getFileSHA1());
        update.setStatus(UpdateStatus.DOWNLOADING);
        tDownloadClient.resume();
    }

    public void cancelDownload(Update update) {
        stopDownload(update);
        new File(update.getFilePath()).delete();
        update.setStatus(UpdateStatus.UNKNOWN);
    }

    // 验证更新
    public void verifyUpdate(Update update) {
        File tFile = new File(update.getFilePath());
        if (tFile.exists()) {
            update.setStatus(UpdateStatus.VERIFYING);
            int tNotifyId = mMapNotificationId.get(update.getFileSHA1());
            startForeground(tNotifyId, NotificationUtils.buildNotification(
                    getApplicationContext(),"update_download", getString(R.string.notification_titie_update_verifying),
                    String.format(getString(R.string.notification_text_update_verifying),
                            update.getName() + " " + update.getVersion()),
                    100, 0,true));
            boolean tResult = FileUtils.calcSHA1(tFile).equals(update.getFileSHA1());
            destroyNotification(tNotifyId);
            if (tResult) {
                update.setStatus(UpdateStatus.VERIFIED);
                NotificationUtils.showNotification(getApplicationContext(), "update_download",
                        tNotifyId, getString(R.string.notification_titie_update_verifying_succeed),
                        String.format(getString(R.string.notification_text_update_verify_succeed),
                                update.getName() + " " + update.getVersion()));
            } else {
                update.setStatus(UpdateStatus.VERIFICATION_FAILED);
                NotificationUtils.showNotification(getApplicationContext(), "update_download",
                        tNotifyId, getString(R.string.notification_titie_update_verifying_failed),
                        String.format(getString(R.string.notification_text_update_verify_failed),
                                update.getName() + " " + update.getVersion()));
            }
        } else {
            update.setStatus(UpdateStatus.UNKNOWN);
        }
    }

    @Override
    public void onDestroy() {
        mUpdatesDbHelper.close();
    }

    private void destroyNotification(int id) {
        stopForeground(true);
        NotificationUtils.destroyNotification(getApplicationContext(), id);
    }

}
