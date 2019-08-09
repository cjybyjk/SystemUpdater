package io.github.cjybyjk.systemupdater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.ArraySet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.cjybyjk.systemupdater.service.DownloadManageService;
import io.github.cjybyjk.systemupdater.service.CheckUpdateService;
import io.github.cjybyjk.systemupdater.ui.UpdateCard;
import io.github.cjybyjk.systemupdater.downloader.DownloadClient;
import io.github.cjybyjk.systemupdater.helper.UpdatesDbHelper;
import io.github.cjybyjk.systemupdater.helper.UpdatesDownloadHelper;
import io.github.cjybyjk.systemupdater.utils.DataFormatUtils;
import io.github.cjybyjk.systemupdater.helper.DialogHelper;
import io.github.cjybyjk.systemupdater.utils.FileUtils;
import io.github.cjybyjk.systemupdater.utils.NotificationUtils;
import io.github.cjybyjk.systemupdater.utils.SystemInfoUtils;
import io.github.cjybyjk.systemupdater.model.Update;
import io.github.cjybyjk.systemupdater.model.UpdateStatus;
import io.github.cjybyjk.systemupdater.updater.UpdateInstaller;
import io.github.cjybyjk.systemupdater.updater.UpdateInstallerAB;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private UpdatesDbHelper mUpdatesDbHelper;
    private List<Update> mUpdatesList;
    private long mSystemBuildTime;
    private String mSystemVersion;
    private SharedPreferences mSharedPreferences;

    private TextView tvOSVersion;
    private TextView tvLastCheck;
    private TextView tvNoUpdates;
    private LinearLayout llUpdatesList;
    private ScrollView svUpdates;
    private FloatingActionButton fab;
    private RotateAnimation mRefreshAnimation;
    private boolean mRefreshFlag = false;
    private boolean mFabVisible;
    private int mFabDistance;
    private int mFabThreshold = 20;
    private Map<String, UpdateCard> mUpdateViewsMap;

    private CheckUpdatesConn mCheckUpdatesConn;
    private CheckUpdateService.statusBinder mCheckUpdateStatusBinder;
    private DownloadUpdatesConn mDownloadUpdatesConn;
    private DownloadManageService.downloadBinder mDownloadBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 基本变量
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUpdatesDbHelper = new UpdatesDbHelper(this);
        mSystemVersion = SystemInfoUtils.getProperty(getString(R.string.attr_prop_os_version), "");
        mSystemBuildTime = Long.parseLong(SystemInfoUtils.getProperty("ro.build.date.utc", "0"));

        // 界面组件
        mUpdateViewsMap = new HashMap<>();
        tvNoUpdates = findViewById(R.id.text_no_updates);
        tvOSVersion = findViewById(R.id.text_OS_ver);
        tvLastCheck = findViewById(R.id.text_last_check);
        llUpdatesList = findViewById(R.id.layout_updates_list);
        svUpdates = findViewById(R.id.scroll_updates);

        //检查存储权限
        FileUtils.verifyStoragePermissions(this);

        // 第一次启动，初始化部分设置
        if (mSharedPreferences.getBoolean("first_run", true)) {
            SharedPreferences.Editor tEditor = mSharedPreferences.edit();
            tEditor.putBoolean("first_run", false);
            Set<String> tAutoDownloadSet = new ArraySet<>();
            tAutoDownloadSet.add("wifi");
            tEditor.putStringSet("auto_download", tAutoDownloadSet);
            Set<String> tUpdateTypeSet = new ArraySet<>();
            tUpdateTypeSet.add("full");
            tUpdateTypeSet.add("incremental");
            tEditor.putStringSet("update_type", tUpdateTypeSet);
            tEditor.putString("download_path", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
            tEditor.apply();
        }
        NotificationUtils.createNotificationChannel(this);

        tvOSVersion.setText(String.format(getString(R.string.header_os_version), mSystemVersion));
        setLastCheck();

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if (!mRefreshFlag) {
                refreshAnimationStart();
                mCheckUpdateStatusBinder.checkUpdate();
            }
        });
        // fab旋转动画
        mRefreshAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRefreshAnimation.setInterpolator(new LinearInterpolator());
        mRefreshAnimation.setDuration(1000);
        // fab 滑动隐藏
        svUpdates.setOnScrollChangeListener((view, X, Y, oldX, oldY) -> {
            if (mRefreshFlag)return;
            if (mFabDistance > mFabThreshold && mFabVisible) {
                //隐藏动画
                mFabVisible = false;
                fab.animate().translationY(fab.getHeight() + fab.getBottom()).setInterpolator(new AccelerateInterpolator(3));
                mFabDistance = 0;
            } else if (mFabDistance < -mFabThreshold && !mFabVisible) {
                //显示动画
                mFabVisible = true;
                fab.animate().translationY(0).setInterpolator(new DecelerateInterpolator(3));
                mFabDistance = 0;
            }
            if (mFabVisible && Y < oldY || (!mFabVisible && Y > oldY)) {//向上滑并且可见  或者  向下滑并且不可见
                mFabDistance += (oldY - Y);
            }
        });

        // 初始化服务
        final Intent checkUpdateService = new Intent(this, CheckUpdateService.class);
        final Intent DownloadManageService = new Intent(this, DownloadManageService.class);
        mCheckUpdatesConn = new CheckUpdatesConn();
        mDownloadUpdatesConn = new DownloadUpdatesConn();
        bindService(checkUpdateService, mCheckUpdatesConn, BIND_AUTO_CREATE);
        bindService(DownloadManageService, mDownloadUpdatesConn, BIND_AUTO_CREATE);
        startService(DownloadManageService);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // 显示最后一次检查更新的时间
    private void setLastCheck() {
        tvLastCheck.setText(String.format(
                getString(R.string.header_last_check),
                DataFormatUtils.getDateTimeLocalized(this,
                        mSharedPreferences.getLong("last_check", mSystemBuildTime))));
    }

    // 填充UpdateViewsMap
    private void initUpdatesList() {
        mUpdatesList = mUpdatesDbHelper.getUpdates();
        if (mUpdatesList.isEmpty()) {
            return;
        }
        for (int i = 0; i < mUpdatesList.size(); i++) {
            final Update tUpdate = mUpdatesList.get(i);
            String tSHA1 = tUpdate.getFileSHA1();
            if (!mUpdateViewsMap.containsKey(tSHA1)) {
                final UpdateCard tUpdateCard = new UpdateCard(MainActivity.this, tUpdate);
                setUpdateViewAction(tUpdateCard, tUpdate);
                mDownloadBinder.addDownload(tUpdate);
                mUpdateViewsMap.put(tSHA1, tUpdateCard);
                tUpdateCard.resumeStatus();
            }
        }
    }

    // 展示更新列表
    private void showUpdatesList() {
        if (mUpdatesList.isEmpty()) {
            tvNoUpdates.setVisibility(View.VISIBLE);
            return;
        } else {
            tvNoUpdates.setVisibility(View.GONE);
        }
        llUpdatesList.removeAllViewsInLayout();
        for (int i = 0; i < mUpdatesList.size(); i++) {
            llUpdatesList.addView(mUpdateViewsMap.get(mUpdatesList.get(i).getFileSHA1()));
        }
    }

    // 安装更新(其实是安装之前的检查)
    private void installUpdate(final Context context, final Update update, final int action) {
        int tStatus = update.getStatus();
        if (tStatus != UpdateStatus.VERIFIED && tStatus != UpdateStatus.INSTALLING) {
            DialogHelper tDialogHelper = new DialogHelper();
            tDialogHelper.setOnOkRunnable(() -> realInstallUpdate(context, update, action));
            tDialogHelper.buildDialog(context, R.string.confirm_verify_dialog_title, R.string.confirm_verify_dialog_message);
            return;
        }
        realInstallUpdate(context, update, action);
    }

    // 真正的安装更新过程
    private void realInstallUpdate(final Context context, final Update update, int action) {
        if (SystemInfoUtils.isABDevice()) {
            try {
                // TODO: 需要AB分区设备进行测试
                UpdateInstallerAB updateInstallerAB = new UpdateInstallerAB(context);
                updateInstallerAB.startUpdateSystem("binpath", "proppath");
            } catch (ClassNotFoundException | MalformedURLException e) {
                e.printStackTrace();
            }
        } else {
            // 非AB分区设备
            final UpdateInstaller updateInstaller = new UpdateInstaller(new File(update.getFilePath()),
                    mSharedPreferences.getBoolean("clean_cache_after_install",false));
            DialogHelper tDialogHelper = new DialogHelper();
            tDialogHelper.setOnOkRunnable(() -> {
                try {
                    mSharedPreferences.edit().putLong("install_check", update.getTimestamp())
                            .putString("install_check_sha1",update.getFileSHA1()).apply();
                    updateInstaller.installUpdate(context, mSharedPreferences.getString("append_ors",""));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            tDialogHelper.buildDialog(context, R.string.confirm_reboot_dialog_title, R.string.confirm_reboot_dialog_message);
        }
    }

    // 设置UpdateView的用户事件
    public void setUpdateViewAction(UpdateCard tUpdateCard, final Update tUpdate) {
        tUpdateCard.setUpdateAction(new UpdateCard.UpdateActionsCallback() {
            @Override
            public void actionShowDescription() {
                DialogHelper tDialogHelper = new DialogHelper();
                tDialogHelper.buildDialog(MainActivity.this, getString(R.string.title_update_desc), DataFormatUtils.base64Decode(tUpdate.getDescription()), true);
            }

            @Override
            public void actionCopyDownloadURL() {
                ClipboardManager tClipboardManager = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData tClipData = ClipData.newPlainText("text/plain", tUpdate.getDownloadUrl());
                tClipboardManager.setPrimaryClip(tClipData);
                showSnackbar(R.string.snack_url_copied, Snackbar.LENGTH_SHORT);
            }

            @Override
            public void actionDownloadManage(final int action) {
                switch (action) {
                    case 0:
                        doWarningMobileNetwork(() -> mDownloadBinder.startDownload(tUpdate));
                        break;
                    case 1:
                        mDownloadBinder.stopDownload(tUpdate);
                        break;
                    case 2:
                        doWarningMobileNetwork(() -> mDownloadBinder.resumeDownload(tUpdate));
                        break;
                    case 3:
                        mDownloadBinder.cancelDownload(tUpdate);
                        break;
                }
            }

            @Override
            public void actionInstallManage(final int action) {
                installUpdate(MainActivity.this, tUpdate, action);
            }

            @Override
            public void actionDeleteUpdate() {
                DialogHelper tDialogHelper = new DialogHelper();
                tDialogHelper.setOnOkRunnable(() -> {
                    if (new File(tUpdate.getFilePath()).delete()) {
                        tUpdate.setStatus(UpdateStatus.UNKNOWN);
                        showSnackbar(R.string.snack_file_deleted, Snackbar.LENGTH_SHORT);
                    } else {
                        showSnackbar(R.string.snack_file_deletion_failed, Snackbar.LENGTH_SHORT);
                    }
                });
                tDialogHelper.buildDialog(MainActivity.this, R.string.confirm_delete_dialog_title, R.string.confirm_delete_dialog_message);
            }

            @Override
            public void actionVerifyUpdate() {
                mDownloadBinder.verifyDownload(tUpdate);
            }
        });
    }


    public void doWarningMobileNetwork(Runnable act) {
        DialogHelper tDialogHelper = new DialogHelper();
        if (SystemInfoUtils.getNetworkType(MainActivity.this).equals("data") &&
                mSharedPreferences.getBoolean("mobile_network_warning",true)) {
            tDialogHelper.setOnOkRunnable(act);
            tDialogHelper.buildDialog(MainActivity.this, R.string.confirm_mobile_network_warning_title,
                    R.string.confirm_mobile_network_warning_message);
        } else {
            act.run();
        }
    }

    public void showSnackbar(int strId, int duration) {
        Snackbar.make(findViewById(R.id.layout_main), strId, duration).show();
    }

    // fab旋转动画
    private void refreshAnimationStart() {
        mRefreshAnimation.setRepeatCount(Animation.INFINITE);
        fab.startAnimation(mRefreshAnimation);
        mRefreshFlag = true;
    }

    private void refreshAnimationStop() {
        mRefreshAnimation.setRepeatCount(0);
        mRefreshFlag = false;
    }

    private class CheckUpdatesConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mCheckUpdateStatusBinder = (CheckUpdateService.statusBinder) iBinder;
            mCheckUpdateStatusBinder.setDownloadListener(new UpdatesDownloadHelper.DownloadListener() {
                @Override
                public void onFailureListener(boolean cancelled) {
                    runOnUiThread(() -> {
                        showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
                        refreshAnimationStop();
                    });
                }

                @Override
                public void onResponseListener(int statusCode, String url, DownloadClient.Headers headers) {
                }

                @Override
                public void onSuccessListener(File destination) {
                    final Date tDate = new Date();
                    initUpdatesList();
                    runOnUiThread(() -> {
                        setLastCheck();
                        showUpdatesList();
                        refreshAnimationStop();
                    });
                }

                @Override
                public void onProgressChangedListener(long bytesRead, long contentLength, long speed, long eta, boolean done) {
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private class DownloadUpdatesConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDownloadBinder = (DownloadManageService.downloadBinder) iBinder;
            mDownloadBinder.setCallback(new DownloadManageService.DownloadStatusCallback() {
                @Override
                public void progressCallback(String sha1, final int progress, final String speed) {
                    final UpdateCard tUpdateCard = mUpdateViewsMap.get(sha1);
                    if (tUpdateCard != null) {
                        runOnUiThread(() -> tUpdateCard.updateProgress(progress, speed));
                    }
                }

                @Override
                public void statusCallback(String sha1, final int status) {
                    final UpdateCard tUpdateCard = mUpdateViewsMap.get(sha1);
                    if (tUpdateCard != null) {
                        runOnUiThread(() -> {
                            tUpdateCard.updateStatus(status);
                        });
                    }
                }
            });
            // 显示更新列表
            initUpdatesList();
            showUpdatesList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    @Override
    protected void onDestroy() {
        unbindService(mCheckUpdatesConn);
        unbindService(mDownloadUpdatesConn);
        mUpdatesDbHelper.close();
        super.onDestroy();
    }
}

