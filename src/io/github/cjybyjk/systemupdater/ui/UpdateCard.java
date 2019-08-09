package io.github.cjybyjk.systemupdater.ui;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import io.github.cjybyjk.systemupdater.R;
import io.github.cjybyjk.systemupdater.utils.DataFormatUtils;
import io.github.cjybyjk.systemupdater.model.Update;
import io.github.cjybyjk.systemupdater.model.UpdateStatus;

public class UpdateCard extends LinearLayout {

    private final static String TAG = "UpdateView";
    private TextView tvUpdateName;
    private TextView tvUpdateTime;
    private TextView tvUpdateSize;
    private TextView tvUpdateType;
    private TextView tvStatus;
    private TextView tvStatusValue;
    private ProgressBar mProg;
    private ImageButton btnDownload;
    private ImageButton btnUpdate;
    private CardView cvBody;
    private LinearLayout llStatus;
    private PopupMenu mPopupMenu;
    private MenuItem mActionDeleteFile;
    private MenuItem mActionCancelDownload;
    private MenuItem mActionCancelInstall;
    private int mUpdateStatus;
    private UpdateActionsCallback mUpdateActionsCallback;
    public UpdateCard(final Context context, final Update update) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.update_card, this);

        // 初始化ui组件
        Resources resources = this.getResources();
        cvBody = findViewById(R.id.cv_body);
        llStatus = findViewById(R.id.layout_status);
        tvUpdateName = findViewById(R.id.text_update_name);
        tvUpdateTime = findViewById(R.id.text_update_time);
        tvUpdateSize = findViewById(R.id.text_update_size);
        tvUpdateType = findViewById(R.id.text_update_type);
        tvStatus = findViewById(R.id.text_status);
        tvStatusValue = findViewById(R.id.text_status_value);
        btnDownload = findViewById(R.id.btn_download);
        btnUpdate = findViewById(R.id.btn_update);
        mProg = findViewById(R.id.prog);
        mUpdateStatus = update.getStatus();

        // 长按弹出菜单
        mPopupMenu = new PopupMenu(context, this);
        mPopupMenu.getMenuInflater().inflate(R.menu.menu_update_item, mPopupMenu.getMenu());
        mActionCancelDownload = mPopupMenu.getMenu().findItem(R.id.action_cancel_download);
        mActionCancelInstall = mPopupMenu.getMenu().findItem(R.id.action_cancel_install);
        mActionDeleteFile = mPopupMenu.getMenu().findItem(R.id.action_delete_file);
        mPopupMenu.setOnMenuItemClickListener(item -> menuItemAction(item));
        cvBody.setOnClickListener(view -> {
            if (mUpdateActionsCallback != null) {
                mUpdateActionsCallback.actionShowDescription();
            }
        });
        cvBody.setOnLongClickListener(view -> {
            mPopupMenu.show();
            return true;
        });

        // 下载按钮
        btnDownload.setOnClickListener(view -> {
            if (mUpdateActionsCallback != null) {
                if (mUpdateStatus == UpdateStatus.DOWNLOADING) {
                    mUpdateActionsCallback.actionDownloadManage(1);
                } else if (mUpdateStatus == UpdateStatus.DOWNLOAD_PAUSED || mUpdateStatus == UpdateStatus.DOWNLOAD_FAILED) {
                    mUpdateActionsCallback.actionDownloadManage(2);
                } else {
                    mUpdateActionsCallback.actionDownloadManage(0);
                }
            }
        });
        // 更新按钮
        btnUpdate.setOnClickListener(view -> {
            if (mUpdateActionsCallback != null) {
                mUpdateActionsCallback.actionInstallManage(0);
            }
        });

        tvUpdateName.setText(update.getName() + " " + update.getVersion());

        tvUpdateTime.setText(String.format(resources.getString(R.string.card_update_time),
                DataFormatUtils.getDateTimeLocalized(getContext(), update.getTimestamp())));
        tvUpdateSize.setText(DataFormatUtils.FormatFileSize(update.getFileSize()));
        if (update.getType() != null && update.getType().equals("incremental")) {
            tvUpdateType.setText(
                    String.format(resources.getString(R.string.card_update_type),
                            resources.getText(R.string.text_update_type_incremental)));
        } else {
            tvUpdateType.setText(
                    String.format(resources.getString(R.string.card_update_type),
                            resources.getText(R.string.text_update_type_full)));
        }

        updateStatus(mUpdateStatus);
        mProg.setProgress(update.getDownloadProgress());
    }

    public void resumeStatus() {
        if (mUpdateActionsCallback != null) {
            switch (mUpdateStatus) {
                case UpdateStatus.STARTING:
                    mUpdateActionsCallback.actionDownloadManage(0);
                    break;
                case UpdateStatus.DOWNLOADING:
                    mUpdateActionsCallback.actionDownloadManage(2);
                    break;
                case UpdateStatus.VERIFYING:
                    mUpdateActionsCallback.actionVerifyUpdate();
                    break;
                case UpdateStatus.INSTALLING:
                    mUpdateActionsCallback.actionInstallManage(1);
                    break;
            }
        }
    }

    private boolean menuItemAction(MenuItem item) {
        if (mUpdateActionsCallback == null) {
            return false;
        }
        switch (item.getItemId()) {
            case R.id.action_copy_url:
                mUpdateActionsCallback.actionCopyDownloadURL();
                break;
            case R.id.action_delete_file:
                mUpdateActionsCallback.actionDeleteUpdate();
                break;
            case R.id.action_cancel_install:
                mUpdateActionsCallback.actionInstallManage(2);
                break;
            case R.id.action_cancel_download:
                mUpdateActionsCallback.actionDownloadManage(3);
                break;
        }
        return true;
    }

    public void setUpdateAction(UpdateActionsCallback updateAction) {
        mUpdateActionsCallback = updateAction;
    }

    // 根据Status更新界面
    public void updateStatus(int tStatus) {
        mUpdateStatus = tStatus;

        llStatus.setVisibility(VISIBLE);
        btnDownload.setVisibility(VISIBLE);
        btnUpdate.setVisibility(VISIBLE);
        tvStatusValue.setVisibility(GONE);
        btnUpdate.setImageResource(R.drawable.ic_system_update);
        btnDownload.setImageResource(R.drawable.ic_file_download);
        mActionCancelDownload.setVisible(false);
        mActionCancelInstall.setVisible(false);
        mActionDeleteFile.setVisible(false);
        mProg.setIndeterminate(false);

        switch (tStatus) {
            case UpdateStatus.UNKNOWN:
                btnUpdate.setVisibility(GONE);
                llStatus.setVisibility(GONE);
                break;
            case UpdateStatus.STARTING:
                btnUpdate.setVisibility(GONE);
                btnDownload.setImageResource(R.drawable.ic_pause);
                mProg.setIndeterminate(true);
                tvStatus.setText(R.string.status_starting);
                mActionCancelDownload.setVisible(true);
                break;
            case UpdateStatus.DOWNLOADING:
                tvStatusValue.setVisibility(VISIBLE);
                btnUpdate.setVisibility(GONE);
                btnDownload.setImageResource(R.drawable.ic_pause);
                tvStatus.setText(R.string.status_downloading);
                mActionCancelDownload.setVisible(true);
                break;
            case UpdateStatus.DOWNLOADED:
                btnDownload.setVisibility(GONE);
                tvStatus.setText(R.string.status_downloaded);
                mActionDeleteFile.setVisible(true);
                break;
            case UpdateStatus.DOWNLOAD_PAUSED:
                btnUpdate.setVisibility(GONE);
                tvStatus.setText(R.string.status_paused);
                mActionCancelDownload.setVisible(true);
                break;
            case UpdateStatus.DOWNLOAD_FAILED:
                btnUpdate.setVisibility(GONE);
                tvStatus.setText(R.string.status_download_failed);
                mActionCancelDownload.setVisible(true);
                break;
            case UpdateStatus.VERIFYING:
                tvStatus.setText(R.string.status_verifying);
                mProg.setIndeterminate(true);
                break;
            case UpdateStatus.VERIFIED:
                btnDownload.setVisibility(GONE);
                tvStatus.setText(R.string.status_verified);
                mActionDeleteFile.setVisible(true);
                break;
            case UpdateStatus.VERIFICATION_FAILED:
                btnDownload.setVisibility(GONE);
                tvStatus.setText(R.string.status_verification_failed);
                mActionDeleteFile.setVisible(true);
                break;
            case UpdateStatus.INSTALLING:
                btnDownload.setVisibility(GONE);
                btnUpdate.setImageResource(R.drawable.ic_pause);
                tvStatus.setText(R.string.status_installing);
                mActionCancelInstall.setVisible(true);
                break;
            case UpdateStatus.INSTALLED:
                btnDownload.setVisibility(GONE);
                btnUpdate.setImageResource(R.drawable.ic_done);
                tvStatus.setText(R.string.status_installed);
                mActionDeleteFile.setVisible(true);
                break;
            case UpdateStatus.INSTALLATION_FAILED:
                btnDownload.setVisibility(GONE);
                tvStatus.setText(R.string.status_installation_failed);
                mActionDeleteFile.setVisible(true);
                break;
            case UpdateStatus.INSTALLATION_CANCELLED:
                btnDownload.setVisibility(GONE);
                tvStatus.setText(R.string.status_installation_cancelled);
                mActionDeleteFile.setVisible(true);
                break;
            case UpdateStatus.INSTALLATION_SUSPENDED:
                btnDownload.setVisibility(GONE);
                tvStatus.setText(R.string.status_installation_suspended);
                mActionCancelInstall.setVisible(true);
                break;
        }
    }

    public void updateProgress(int progress, String statusValue) {
        mProg.setProgress(progress);
        tvStatusValue.setText(statusValue);
    }

    public interface UpdateActionsCallback {
        void actionShowDescription();

        void actionCopyDownloadURL();

        void actionDownloadManage(int action);

        void actionInstallManage(int action);

        void actionDeleteUpdate();

        void actionVerifyUpdate();
    }

}
