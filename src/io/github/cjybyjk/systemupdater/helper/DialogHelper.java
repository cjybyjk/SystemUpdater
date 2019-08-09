package io.github.cjybyjk.systemupdater.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.KeyEvent;

public class DialogHelper {
    private Runnable mOnOkRunnable;

    public void buildDialog(Context context, String title, String message, boolean isOkOnly) {
        AlertDialog.Builder tDialog = new AlertDialog.Builder(context);
        tDialog.setTitle(title);
        tDialog.setMessage(Html.fromHtml(message,Html.FROM_HTML_MODE_COMPACT));
        tDialog.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            if (mOnOkRunnable != null) {
                mOnOkRunnable.run();
            }
        });
        if (!isOkOnly) {
            tDialog.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                dialogInterface.dismiss();
            });
        }
        tDialog.setOnKeyListener((dialogInterface, i, keyEvent) -> false);
        tDialog.create().show();
    }

    public void buildDialog(Context context, int titleId, int messageId, boolean isOkOnly) {
        buildDialog(context, context.getString(titleId), context.getString(messageId), isOkOnly);
    }

    public void buildDialog(Context context, int titleId, int messageId) {
        buildDialog(context, titleId, messageId, false);
    }

    public void setOnOkRunnable(Runnable okRunnable) {
        mOnOkRunnable = okRunnable;
    }

}
