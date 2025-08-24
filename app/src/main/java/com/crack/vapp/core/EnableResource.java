package com.crack.vapp.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.TypedValue;

import com.crack.vapp.utils.ReflectUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class EnableResource {
    public static AssetManager assetManager;

    public synchronized static void mergeResources(Context baseContext, String pluginResourcePath) {
        try {
            assetManager = AssetManager.class.newInstance();

            Method addAssetPathMethod = ReflectUtils.getMethod(AssetManager.class, "addAssetPath", String.class);
            ReflectUtils.invokeMethod(assetManager, addAssetPathMethod, pluginResourcePath);
            String baseResourcePath = baseContext.getPackageResourcePath();
            ReflectUtils.invokeMethod(assetManager, addAssetPathMethod, baseResourcePath);

            Resources multiResources = new Resources(assetManager, baseContext.getResources().getDisplayMetrics(),
                    baseContext.getResources().getConfiguration());
            ReflectUtils.setFieldValue(baseContext, "mResources", multiResources);
            Object loadApk = ReflectUtils.getFieldValue(baseContext, "mPackageInfo");
            ReflectUtils.setFieldValue(loadApk, "mResources", multiResources);

            Object resourcesImpl = ReflectUtils.getFieldValue(multiResources, "mResourcesImpl");
            Class<?> ActivityThreadClass = ReflectUtils.getClass("android.app.ActivityThread");
            Object currentActivityThread = ReflectUtils.getFieldValue(ActivityThreadClass, "sCurrentActivityThread");
            Object resourcesManager = ReflectUtils.getFieldValue(currentActivityThread, "mResourcesManager");
            Map resourceImpls = (Map) ReflectUtils.getFieldValue(resourcesManager, "mResourceImpls");
            Object resourceskey = resourceImpls.keySet().iterator().next();
            resourceImpls.put(resourceskey, new WeakReference<>(resourcesImpl));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void replaceAssetManager() {
        Class<?> clazz = ReflectUtils.getClass("android.content.res.ResourcesImpl");
        Method getValue = ReflectUtils.getMethod(clazz, "getValue", int.class, TypedValue.class, boolean.class);
        try {
            CalvinBridge.hookMethod(getValue, new CA_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object ResourcesImpl = param.thisObject;
                    if (assetManager != null) {
                        ReflectUtils.setFieldValue(ResourcesImpl, "mAssets", assetManager);
                    }

                    super.beforeHookedMethod(param);
                }
            });

        } catch (Exception e) {
        }
    }
}