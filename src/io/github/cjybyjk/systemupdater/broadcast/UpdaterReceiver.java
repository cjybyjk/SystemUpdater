package io.github.cjybyjk.systemupdater.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.io.File;

import io.github.cjybyjk.systemupdater.R;
import io.github.cjybyjk.systemupdater.helper.UpdatesDbHelper;
import io.github.cjybyjk.systemupdater.model.Update;
import io.github.cjybyjk.systemupdater.model.UpdateStatus;
import io.github.cjybyjk.systemupdater.utils.NotificationUtils;
import io.github.cjybyjk.systemupdater.utils.SystemInfoUtils;

public class UpdaterReceiver extends BroadcastReceiver {

    private final static String TAG = "UpdaterReceiver";
    // 这个Receiver的通知id
    private final static int NotificationId = 1;

    private long mSystemBuildTime;
    private SharedPreferences mSharedPreferences;
    private UpdatesDbHelper mUpdatesDbHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mUpdatesDbHelper = new UpdatesDbHelper(context);
        mSystemBuildTime = Long.parseLong(SystemInfoUtils.getProperty("ro.build.date.utc", "0"));
        Long mRequiredTime = mSharedPreferences.getLong("install_check",0);
        String mRequireSHA1 = mSharedPreferences.getString("install_check_sha1","");
        if (mRequiredTime == 0 || mRequireSHA1.equals(""))return;
        final Update tUpdate = mUpdatesDbHelper.getUpdate(mRequireSHA1);
        String title = "";
        String message = "";
        if (mRequiredTime != mSystemBuildTime) {
            title = context.getString(R.string.notification_title_update_install_failed);
            message = String.format(context.getString(R.string.notification_text_update_install_failed),tUpdate.getName() + " " + tUpdate.getVersion());
            tUpdate.setStatus(UpdateStatus.INSTALLATION_FAILED);
        } else {
            title = context.getString(R.string.notification_title_update_install_success);
            message = String.format(context.getString(R.string.notification_text_update_install_success),tUpdate.getName() + " " + tUpdate.getVersion());
            tUpdate.setStatus(UpdateStatus.INSTALLED);
            if (mSharedPreferences.getBoolean("delete_after_install" ,false)) {
                ((Runnable) () -> {
                    new File(tUpdate.getFilePath()).delete();
                }).run();
            }
        }
        mUpdatesDbHelper.changeUpdateStatus(tUpdate);
        NotificationUtils.showNotification(context, "update_install", NotificationId, title, message);
    }
}
