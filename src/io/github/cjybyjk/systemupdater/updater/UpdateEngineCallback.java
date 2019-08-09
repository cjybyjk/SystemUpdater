package io.github.cjybyjk.systemupdater.updater;

public abstract class UpdateEngineCallback {

    /**
     * Invoked when anything changes. The value of {@code status} will
     * be one of the values from {@link UpdateEngine.UpdateStatusConstants},
     * and {@code percent} will be valid [TODO: in which cases?].
     */
    public abstract void onStatusUpdate(int status, float percent);

    /**
     * Invoked when the payload has been applied, whether successfully or
     * unsuccessfully. The value of {@code errorCode} will be one of the
     * values from {@link UpdateEngine.ErrorCodeConstants}.
     */
    public abstract void onPayloadApplicationComplete(int errorCode);

}