package com.crack.vapp.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;

import static com.crack.vapp.BaseApplication.pluginPackageInfo;
import com.crack.vapp.utils.ReflectUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class EnableBroadcastReceiver {
    private static final String TAG = "EnableBroadcastReceiver";

    private static Map<String, List<BroadcastReceiver>> mDynamicReceivers = new HashMap<>();
    private static List<ActivityInfo> mStaticReceivers = new ArrayList<>();
    private static Map<String, List<IntentFilter>> mReceiverFilters = new HashMap<>();

    public static void hook() {
        Method registerReceiverMethod = ReflectUtils.getMethod(Context.class, "registerReceiver",
                BroadcastReceiver.class, IntentFilter.class);
        CalvinBridge.hookMethod(registerReceiverMethod, new CA_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                BroadcastReceiver receiver = (BroadcastReceiver) param.args[0];
                IntentFilter filter = (IntentFilter) param.args[1];

                if (receiver != null && filter != null) {
                    String packageName = pluginPackageInfo.packageName;
                    if (!mDynamicReceivers.containsKey(packageName)) {
                        mDynamicReceivers.put(packageName, new ArrayList<>());
                    }
                    mDynamicReceivers.get(packageName).add(receiver);

                    if (!mReceiverFilters.containsKey(packageName)) {
                        mReceiverFilters.put(packageName, new ArrayList<>());
                    }
                    mReceiverFilters.get(packageName).add(filter);
                }
            }
        });

        try {
            Class<?> contextWrapperClass = Class.forName("android.content.ContextWrapper");
            CalvinBridge.hookAllMethods(contextWrapperClass, "registerReceiver", new CA_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    BroadcastReceiver receiver = (BroadcastReceiver) param.args[0];
                    IntentFilter filter = (IntentFilter) param.args[1];

                    if (receiver != null && filter != null) {
                        String packageName = pluginPackageInfo.packageName;
                        if (!mDynamicReceivers.containsKey(packageName)) {
                            mDynamicReceivers.put(packageName, new ArrayList<>());
                        }
                        mDynamicReceivers.get(packageName).add(receiver);

                        if (!mReceiverFilters.containsKey(packageName)) {
                            mReceiverFilters.put(packageName, new ArrayList<>());
                        }
                        mReceiverFilters.get(packageName).add(filter);
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Hook ContextWrapper.registerReceiver failed", e);
        }

        try {
            Class<?> activityThreadClass = ReflectUtils.getClass("android.app.ActivityThread");
            CalvinBridge.hookAllMethods(activityThreadClass, "handleReceiver", new CA_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object data = param.args[0];
                    Intent intent = (Intent) ReflectUtils.getFieldValue(data, "intent");

                    if (intent != null && pluginPackageInfo != null) {
                        String targetPackage = intent.getPackage();
                        if (targetPackage != null && targetPackage.equals(pluginPackageInfo.packageName)) {
                            intent.setPackage(null);
                            intent.setComponent(null);
                        }
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Hook handleReceiver failed", e);
        }

        try {
            Class<?> intentResolverClass = ReflectUtils.getClass("com.android.internal.content.IntentResolver");
            CalvinBridge.hookAllMethods(intentResolverClass, "queryIntent", new CA_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];

                    if (intent != null && pluginPackageInfo != null) {
                        List<ResolveInfo> results = (List<ResolveInfo>) param.getResult();
                        if (results == null) {
                            results = new ArrayList<>();
                        }

                        for (ActivityInfo receiverInfo : mStaticReceivers) {
                            for (IntentFilter filter : mReceiverFilters.getOrDefault(pluginPackageInfo.packageName, new ArrayList<>())) {
                                if (filter.match(intent.getAction(), intent.getType(), intent.getScheme(),
                                        intent.getData(), intent.getCategories(), TAG) > 0) {
                                    ResolveInfo resolveInfo = new ResolveInfo();
                                    resolveInfo.activityInfo = receiverInfo;
                                    results.add(resolveInfo);
                                }
                            }
                        }

                        param.setResult(results);
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Hook IntentResolver failed", e);
        }
    }

    public static void parseStaticReceivers(String pluginApkPath) {
        try {
            Object packageParser = ReflectUtils.createInstance("android.content.pm.PackageParser");
            if (packageParser == null) {
                throw new RuntimeException("PackageParser instance creation failed");
            }

            Method parsePackageMethod = ReflectUtils.getMethod(packageParser.getClass(), "parsePackage",
                    File.class, int.class);
            if (parsePackageMethod == null) {
                throw new RuntimeException("parsePackage method not found");
            }

            Object pkg = ReflectUtils.invokeMethod(packageParser, parsePackageMethod, new File(pluginApkPath), 0);
            if (pkg != null) {
                List receivers = (List) ReflectUtils.getFieldValue(pkg, "receivers");
                if (receivers != null) {
                    for (Object receiver : receivers) {
                        ActivityInfo info = (ActivityInfo) ReflectUtils.getFieldValue(receiver, "info");
                        if (info != null) {
                            mStaticReceivers.add(info);

                            List<IntentFilter> filters = (List<IntentFilter>) ReflectUtils.getFieldValue(receiver, "intents");
                            if (filters != null) {
                                String packageName = pluginPackageInfo.packageName;
                                if (!mReceiverFilters.containsKey(packageName)) {
                                    mReceiverFilters.put(packageName, new ArrayList<>());
                                }
                                mReceiverFilters.get(packageName).addAll(filters);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parse static receivers failed", e);
        }
    }
}