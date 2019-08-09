package io.github.cjybyjk.systemupdater.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.lang.reflect.Method;

public class SystemInfoUtils {

    private static final String TAG = "SystemInfoUtils";

    /**
     * isABDevice
     *
     * @return true: this device has A/B slot; false: this device hasn't A/B slot
     */
    public static boolean isABDevice() {
        return (getProperty("ro.build.ab_update", "false").equals("true"));
    }

    /**
     * Get system property on build.prop and other files
     *
     * @param key          key
     * @param defaultValue return this value when key not exists
     * @return value
     */
    public static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> sysPropClass = Class.forName("android.os.SystemProperties");
            Method get = sysPropClass.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(sysPropClass, key, "unknown"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return value;
        }
    }

    public static String getNetworkType(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    return "data";
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)  {
                    return "wifi";
                }
            }
        }
        return "null";
    }

}
