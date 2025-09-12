package com.crack.vapp.core;

import android.content.ContextWrapper;
import android.content.Intent;

import static com.crack.vapp.BaseApplication.baseActivity;
import com.crack.vapp.utils.ReflectUtils;
import com.crack.vapp.proxy.ProxyService;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class EnableService {
    private static final String TAG = "EnableService";
    static HashMap<String, Intent> intentMap = new HashMap<>();

    public static void hook() {
        Method startService = ReflectUtils.getMethod(ContextWrapper.class, "startService", Intent.class);
        CalvinBridge.hookMethod(startService, new CA_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent pluginIntent = (Intent) param.args[0];
                String uniqueKey = UUID.randomUUID().toString();
                intentMap.put(uniqueKey, pluginIntent);
                Intent fakeIntent = new Intent(baseActivity, ProxyService.class);
                fakeIntent.putExtra("key", uniqueKey);
                param.args[0] = fakeIntent;
                super.beforeHookedMethod(param);
            }
        });

        Class<?> ActivityThread = ReflectUtils.getClass("android.app.ActivityThread");
        CalvinBridge.hookAllMethods(ActivityThread, "handleCreateService", new CA_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object data = param.args[0];
                Intent fakeIntent = (Intent) ReflectUtils.getFieldValue(data, "intent");
                if (fakeIntent != null) {
                    String uniqueKey = fakeIntent.getStringExtra("key");
                    Intent pluginIntent = intentMap.get(uniqueKey);

                    Object info = ReflectUtils.getFieldValue(param.args[0], "info");
                    if (info != null && pluginIntent != null) {
                        ReflectUtils.setFieldValue(info, "name", pluginIntent.getComponent().getClassName());
                        ReflectUtils.setFieldValue(info, "packageName", pluginIntent.getComponent().getPackageName());
                        intentMap.remove(uniqueKey);
                    }
                }

                super.beforeHookedMethod(param);
            }
        });
    }
}