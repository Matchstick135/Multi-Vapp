package com.crack.vapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;

public class BaseApplication extends Application {
    public static Application baseApplication;
    public static Context baseContext;
    public static Activity baseActivity;
    public static String basePackageName = "com.crack.vapp";
    public static String proxyActivityName = "com.crack.vapp.proxy.ProxyActivity";

    public static PackageInfo pluginPackageInfo;

    static {
        System.loadLibrary("bypass");
    }

    @Override
    public Context getBaseContext() {
        return super.getBaseContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        baseApplication = this;
        baseContext = base;
    }
}
