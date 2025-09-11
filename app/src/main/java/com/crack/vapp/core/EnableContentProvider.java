package com.crack.vapp.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import static com.crack.vapp.BaseApplication.baseContext;
import static com.crack.vapp.BaseApplication.pluginPackageInfo;
import com.crack.vapp.utils.ReflectUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class EnableContentProvider {
    private static final String TAG = "EnableContentProvider";

    private static Map<String, ProviderInfo> mPluginProviders = new HashMap<>();
    private static Map<String, Object> mProviderInstances = new HashMap<>();
    private static Map<Uri, Uri> mUriMapping = new HashMap<>();

    public static void hook() {
        try {
            Class<?> activityThreadClass = ReflectUtils.getClass("android.app.ActivityThread");
            if (activityThreadClass != null) {
                CalvinBridge.hookAllMethods(activityThreadClass, "installProvider", new CA_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ProviderInfo providerInfo = (ProviderInfo) param.args[1];

                        if (providerInfo != null && pluginPackageInfo != null &&
                                pluginPackageInfo.packageName.equals(providerInfo.packageName)) {
                            mPluginProviders.put(providerInfo.authority, providerInfo);

                            String originalAuthority = providerInfo.authority;
                            String hostAuthority = "provider_" + System.currentTimeMillis() + "_" + originalAuthority;
                            providerInfo.authority = hostAuthority;

                            Uri originalUri = Uri.parse("content://" + originalAuthority);
                            Uri hostUri = Uri.parse("content://" + hostAuthority);
                            mUriMapping.put(hostUri, originalUri);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object provider = param.getResult();
                        ProviderInfo providerInfo = (ProviderInfo) param.args[1];

                        if (provider != null && providerInfo != null && mPluginProviders.containsValue(providerInfo)) {
                            mProviderInstances.put(providerInfo.authority, provider);
                        }
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Hook installProvider failed", e);
        }

        try {
            Class<?> contentResolverClass = Class.forName("android.content.ContentResolver");
            CalvinBridge.hookAllMethods(contentResolverClass, "acquireProvider", new CA_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object provider = param.getResult();
                    String name = (String) param.args[1];

                    if (provider != null && name != null && mPluginProviders.containsKey(name)) {
                        param.setResult(new PluginContentProviderWrapper(provider, name));
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Hook acquireProvider failed", e);
        }

        Method attachInfoMethod = ReflectUtils.getMethod(ContentProvider.class, "attachInfo", Context.class, ProviderInfo.class);
        if (attachInfoMethod != null) {
            CalvinBridge.hookMethod(attachInfoMethod, new CA_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ProviderInfo info = (ProviderInfo) param.args[1];
                    if (info != null && pluginPackageInfo != null &&
                            pluginPackageInfo.packageName.equals(info.packageName)) {
                        param.args[0] = baseContext;
                    }
                }
            });
        }
    }

    private static class PluginContentProviderWrapper {
        private final Object mBaseProvider;
        private final String mAuthority;

        public PluginContentProviderWrapper(Object baseProvider, String authority) {
            this.mBaseProvider = baseProvider;
            this.mAuthority = authority;
        }

        private Uri redirectUri(Uri uri) {
            for (Map.Entry<Uri, Uri> entry : mUriMapping.entrySet()) {
                if (uri.toString().startsWith(entry.getKey().toString())) {
                    String path = uri.toString().substring(entry.getKey().toString().length());
                    return Uri.parse(entry.getValue().toString() + path);
                }
            }
            return uri;
        }

        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            try {
                Method queryMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "query",
                        Uri.class, String[].class, String.class, String[].class, String.class);
                if (queryMethod != null) {
                    return (Cursor) ReflectUtils.invokeMethod(mBaseProvider, queryMethod,
                            redirectUri(uri), projection, selection, selectionArgs, sortOrder);
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider query failed", e);
            }
            return null;
        }

        public String getType(Uri uri) {
            try {
                Method getTypeMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "getType", Uri.class);
                if (getTypeMethod != null) {
                    return (String) ReflectUtils.invokeMethod(mBaseProvider, getTypeMethod, redirectUri(uri));
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider getType failed", e);
            }
            return null;
        }

        public Uri insert(Uri uri, ContentValues values) {
            try {
                Method insertMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "insert",
                        Uri.class, ContentValues.class);
                if (insertMethod != null) {
                    return (Uri) ReflectUtils.invokeMethod(mBaseProvider, insertMethod, redirectUri(uri), values);
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider insert failed", e);
            }
            return null;
        }

        public int delete(Uri uri, String selection, String[] selectionArgs) {
            try {
                Method deleteMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "delete",
                        Uri.class, String.class, String[].class);
                if (deleteMethod != null) {
                    return (Integer) ReflectUtils.invokeMethod(mBaseProvider, deleteMethod,
                            redirectUri(uri), selection, selectionArgs);
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider delete failed", e);
            }
            return 0;
        }

        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            try {
                Method updateMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "update",
                        Uri.class, ContentValues.class, String.class, String[].class);
                if (updateMethod != null) {
                    return (Integer) ReflectUtils.invokeMethod(mBaseProvider, updateMethod,
                            redirectUri(uri), values, selection, selectionArgs);
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider update failed", e);
            }
            return 0;
        }

        public Bundle call(String method, String arg, Bundle extras) {
            try {
                Method callMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "call",
                        String.class, String.class, Bundle.class);
                if (callMethod != null) {
                    return (Bundle) ReflectUtils.invokeMethod(mBaseProvider, callMethod, method, arg, extras);
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider call failed", e);
            }
            return null;
        }

        public int bulkInsert(Uri uri, ContentValues[] values) {
            try {
                Method bulkInsertMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "bulkInsert",
                        Uri.class, ContentValues[].class);
                if (bulkInsertMethod != null) {
                    return (Integer) ReflectUtils.invokeMethod(mBaseProvider, bulkInsertMethod, redirectUri(uri), values);
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider bulkInsert failed", e);
            }
            return 0;
        }

        public IBinder asBinder() {
            try {
                Method asBinderMethod = ReflectUtils.getMethod(mBaseProvider.getClass(), "asBinder");
                if (asBinderMethod != null) {
                    return (IBinder) ReflectUtils.invokeMethod(mBaseProvider, asBinderMethod);
                }
            } catch (Exception e) {
                throw new RuntimeException("Plugin provider asBinder failed", e);
            }
            return null;
        }
    }
}