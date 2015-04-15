package me.pjq.musicplayer.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public class Utils {
    /**
     * Unbind all the drawables.
     *
     * @param view
     */
    public static void unBindDrawables(View view) {
        if (view != null) {
            if (view.getBackground() != null) {
                view.getBackground().setCallback(null);
            }

            if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    unBindDrawables(((ViewGroup) view).getChildAt(i));
                }

                ((ViewGroup) view).removeAllViews();
            }
        }
    }

    /**
     * Check whether the network is available.
     *
     * @param context
     * @return true if the network is available.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].isConnected()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isWifiActive(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] info;
        if (connectivity != null) {
            info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if ((info[i].getTypeName().equalsIgnoreCase("WIFI") || info[i]
                            .getTypeName().equalsIgnoreCase("WI FI"))
                            && info[i].isConnected()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check the network is available and not wifi,maybe 2G or 3G data
     * connection.
     *
     * @param context
     * @return true if is not wifi.
     */
    public static boolean isNetworkAvailableAndNotWifi(Context context) {
        return isNetworkAvailable(context) && !isWifiActive(context);
    }

    public static String getTimeReadable(int durationSecond) {
        int ss = durationSecond;

        // post hour
        int hour = durationSecond / 3600;
        String hourString = "";
        if (hour > 0) {
            ss = durationSecond % 3600;

            if (hour < 10) {
                hourString = "0" + hour;
            } else {
                hourString = "" + hour;
            }
        }

        int minute = ss / 60;
        int second = ss % 60;
        String minuteString = "";
        String sencodString = "";

        if (minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = "" + minute;
        }
        if (second < 10) {
            sencodString = "0" + second;
        } else {
            sencodString = "" + second;
        }

        String readableTime = minuteString + ":" + sencodString;
        if (hour > 0) {
            readableTime = hourString + ":" + readableTime;
        }

        return readableTime;
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }
}
