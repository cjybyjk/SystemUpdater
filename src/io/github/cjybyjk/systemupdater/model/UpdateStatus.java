package io.github.cjybyjk.systemupdater.model;

public final class UpdateStatus {
    public static final int UNKNOWN = 0;
    public static final int STARTING = 1;
    public static final int DOWNLOADING = 2;
    public static final int DOWNLOADED = 3;
    public static final int DOWNLOAD_PAUSED = 4;
    public static final int DOWNLOAD_FAILED = 5;
    public static final int VERIFYING = 6;
    public static final int VERIFIED = 7;
    public static final int VERIFICATION_FAILED = 8;
    public static final int INSTALLING = 9;
    public static final int INSTALLED = 10;
    public static final int INSTALLATION_FAILED = 11;
    public static final int INSTALLATION_CANCELLED = 12;
    public static final int INSTALLATION_SUSPENDED = 13;
}
