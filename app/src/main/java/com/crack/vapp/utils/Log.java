package com.crack.vapp.utils;

public class Log {
    private static final String TAG = "Log";

    public static void d( String msg) {
        android.util.Log.d(TAG, "d: " + msg);
    }

    public static void e( String msg) {
        android.util.Log.e(TAG, "e: " + msg);
    }

    public static void i( String msg) {
        android.util.Log.i(TAG, "i: " + msg);
    }
}
