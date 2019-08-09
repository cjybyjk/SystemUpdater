package io.github.cjybyjk.systemupdater.updater;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class UpdateInstaller {
    private static final String TAG = "UpdateInstaller";
    private File mUpdateZip;
    private boolean mCleanCache;

    public UpdateInstaller(File updateZip, boolean cleanCache) {
        mUpdateZip = updateZip;
        mCleanCache = cleanCache;
    }

    public boolean installUpdate(Context context, String appendORS) throws IOException {
        if (!mUpdateZip.exists()) {
            Log.e(TAG, "File " + mUpdateZip.getAbsolutePath() + " not exists");
            return false;
        }

        File recovery_dir = new File("/cache/recovery/");
        File command_file = new File("/cache/recovery/command");

        try {
            if (!recovery_dir.exists()) {
                recovery_dir.mkdirs();
            }
            if (command_file.exists()) {
                command_file.delete();
            }
            Log.d(TAG, "Ready to install " + mUpdateZip.getAbsolutePath());
            FileWriter command = new FileWriter(command_file.getPath());
            try {
                command.write("--update-package=" + mUpdateZip.getAbsolutePath());
                command.write("\n");
                if (mCleanCache) {
                    command.write("--wipe_cache");
                    command.write("\n");
                }
                command.write(appendORS + "\n");
            } finally {
                command.close();
            }
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            pm.reboot("recovery");
        } catch (Exception e) {
            e.printStackTrace();
            Process suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream suDataOutputStream = new DataOutputStream(suProcess.getOutputStream());
            suDataOutputStream.writeBytes("mkdir -p \"" + recovery_dir.getAbsolutePath() + "\"\n");
            suDataOutputStream.writeBytes("echo \"--update-package=" + mUpdateZip.getAbsolutePath() +
                    "\" > " + command_file.getAbsolutePath() + " \n");
            if (mCleanCache) {
                suDataOutputStream.writeBytes("echo \"--wipe_cache\" >> " + command_file.getAbsolutePath() + " \n");
            }
            suDataOutputStream.writeBytes("echo \"" + appendORS + "\" >> " + command_file.getAbsolutePath() + " \n");
            suDataOutputStream.writeBytes("reboot recovery \n");
            suDataOutputStream.flush();
        }

        return true;
    }

}
