package io.github.cjybyjk.systemupdater.updater;

import android.annotation.SuppressLint;
import android.os.Handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 反射 Android hidden class android.os.UpdateEngine
 */

public class UpdateEngine {

    @SuppressLint("PrivateApi")
    Class<?> mUpdateEngineClass = Class.forName("android.os.UpdateEngine");

    public UpdateEngine() throws ClassNotFoundException {
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("UpdateEngine", (Class<?>) null);
            mUpdateEngineClassMethod.invoke(mUpdateEngineClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public boolean bind(final UpdateEngineCallback callback, final Handler handler) {
        boolean ret = false;
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("bind", UpdateEngineCallback.class, Handler.class);
            ret = (boolean) mUpdateEngineClassMethod.invoke(mUpdateEngineClass, callback, handler);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean bind(final UpdateEngineCallback callback) {
        return bind(callback, null);
    }

    public void applyPayload(String url, long offset, long size, String[] headerKeyValuePairs) {
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("applyPayload", String.class, long.class, String[].class);
            mUpdateEngineClassMethod.invoke(mUpdateEngineClass, url, offset, size, headerKeyValuePairs);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("cancel", (Class<?>) null);
            mUpdateEngineClassMethod.invoke(mUpdateEngineClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void suspend() {
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("suspend", (Class<?>) null);
            mUpdateEngineClassMethod.invoke(mUpdateEngineClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("resume", (Class<?>) null);
            mUpdateEngineClassMethod.invoke(mUpdateEngineClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void resetStatus() {
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("resetStatus", (Class<?>) null);
            mUpdateEngineClassMethod.invoke(mUpdateEngineClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void unbind() {
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("unbind", (Class<?>) null);
            mUpdateEngineClassMethod.invoke(mUpdateEngineClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public boolean verifyPayloadMetadata(String payloadMetadataFilename) {
        boolean ret = false;
        try {
            Method mUpdateEngineClassMethod = mUpdateEngineClass.getMethod("unbind", String.class);
            ret = (boolean) mUpdateEngineClassMethod.invoke(mUpdateEngineClass, payloadMetadataFilename);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Error code from the update engine. Values must agree with the ones in
     * system/update_engine/common/error_code.h.
     */
    public static final class ErrorCodeConstants {

        public static final int SUCCESS = 0;
        public static final int ERROR = 1;
        public static final int FILESYSTEM_COPIER_ERROR = 4;
        public static final int POST_INSTALL_RUNNER_ERROR = 5;
        public static final int PAYLOAD_MISMATCHED_TYPE_ERROR = 6;
        public static final int INSTALL_DEVICE_OPEN_ERROR = 7;
        public static final int KERNEL_DEVICE_OPEN_ERROR = 8;
        public static final int DOWNLOAD_TRANSFER_ERROR = 9;
        public static final int PAYLOAD_HASH_MISMATCH_ERROR = 10;
        public static final int PAYLOAD_SIZE_MISMATCH_ERROR = 11;
        public static final int DOWNLOAD_PAYLOAD_VERIFICATION_ERROR = 12;
        public static final int UPDATED_BUT_NOT_ACTIVE = 52;
    }

    /**
     * Update status code from the update engine. Values must agree with the
     * ones in system/update_engine/client_library/include/update_engine/update_status.h.
     */
    public static final class UpdateStatusConstants {
        public static final int IDLE = 0;
        public static final int CHECKING_FOR_UPDATE = 1;
        public static final int UPDATE_AVAILABLE = 2;
        public static final int DOWNLOADING = 3;
        public static final int VERIFYING = 4;
        public static final int FINALIZING = 5;
        public static final int UPDATED_NEED_REBOOT = 6;
        public static final int REPORTING_ERROR_EVENT = 7;
        public static final int ATTEMPTING_ROLLBACK = 8;
        public static final int DISABLED = 9;

    }

}
