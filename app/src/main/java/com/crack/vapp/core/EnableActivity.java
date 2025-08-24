package com.crack.vapp.core;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import static com.crack.vapp.BaseApplication.baseContext;
import static com.crack.vapp.BaseApplication.pluginPackageInfo;
import static com.crack.vapp.BaseApplication.baseActivity;
import static com.crack.vapp.BaseApplication.basePackageName;
import static com.crack.vapp.BaseApplication.proxyActivityName;
import com.crack.vapp.utils.ReflectUtils;
import com.crack.vapp.proxy.ProxyActivity;

import java.lang.reflect.Method;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class EnableActivity {
    private static final String TAG = "EnableActivity";

    public static void hook() {
        Method execStartActivity = null;
        try {
            execStartActivity = ReflectUtils.getMethod(
                    Instrumentation.class,
                    "execStartActivity",
                    Context.class,
                    IBinder.class,
                    IBinder.class,
                    Activity.class,
                    Intent.class,
                    int.class,
                    Bundle.class
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        CalvinBridge.hookMethod(execStartActivity, new CA_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent pluginIntent = (Intent) param.args[4];
                Intent fakeIntent = new Intent(baseActivity, ProxyActivity.class);
                fakeIntent.setComponent(new ComponentName(basePackageName, proxyActivityName));
                fakeIntent.putExtra("plugin", pluginIntent);
                param.args[4] = fakeIntent;

                super.beforeHookedMethod(param);
            }
        });

        try {
            Class<?> clazz = ReflectUtils.getClass("android.app.ActivityThread");
            CalvinBridge.hookAllMethods(clazz, "performLaunchActivity", new CA_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object activityClientRecord = param.args[0];
                    Object LoadedApk = ReflectUtils.getFieldValue(activityClientRecord, "packageInfo");

                    ReflectUtils.setFieldValue(LoadedApk, "mApplication", null);
                    ReflectUtils.setFieldValue(LoadedApk, "mApplicationInfo", pluginPackageInfo.applicationInfo);

                    Intent fakeIntent = (Intent) ReflectUtils.getFieldValue(activityClientRecord, "intent");
                    if (fakeIntent != null) {
                        Intent pluginIntent = (Intent) fakeIntent.getParcelableExtra("plugin");
                        if (pluginIntent != null) {
                            ReflectUtils.setFieldValue(activityClientRecord, "intent", pluginIntent);
                        }
                    }

                    Object activityInfo = ReflectUtils.getFieldValue(activityClientRecord, "activityInfo");
                    ReflectUtils.setFieldValue(activityInfo, "theme", pluginPackageInfo.applicationInfo.theme);

                    super.beforeHookedMethod(param);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Method getPackageName = ReflectUtils.getMethod(baseContext.getClass(), "getPackageName");
        CalvinBridge.hookMethod(getPackageName, new CA_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(pluginPackageInfo.packageName);

                super.afterHookedMethod(param);
            }
        });
    }
}