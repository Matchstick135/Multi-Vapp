package com.crack.vapp.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import org.w3c.dom.Document;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import axml.xml.AxmlUtils;

public class PackageParser {
    static String TAG = "PackageParser";

    public static PackageInfo getPackageInfo(String apkPath, Context context) {
        Class<?> PackageParser = ReflectUtils.getClass("android.content.pm.PackageParser");
        Method parsePackage = ReflectUtils.getMethod(PackageParser, "parsePackage", File.class, int.class, boolean.class);
        if (PackageParser == null) {
//            Log.e(TAG, "Hidden API restrictions not been removed yet");
            return null;
        }
        try {
            Object o = PackageParser.newInstance();
            Object Package = parsePackage.invoke(o, new File(apkPath), 0, false);
            if (Package != null) {
                Log.d("hook", "Package: " + Package);

                Class<?> ApexInfo = ReflectUtils.getClass("android.apex.ApexInfo");

                Method generatePackageInfo1 = ReflectUtils.getMethod(
                        PackageParser,
                        "generatePackageInfo",
                        Package.getClass(),
                        ApexInfo,
                        int.class
                );
                int flags = 1048575;
//                Log.d(TAG, "getPackageInfo: flags : "+ flags);
                PackageInfo packageInfo = (PackageInfo) generatePackageInfo1.invoke(o, Package, null, flags);

                if (packageInfo != null) {
//                    Log.d(TAG, "packageInfo: " + packageInfo);
                    return packageInfo;
                }
            }
        } catch (Exception e) {
//            Log.e(TAG, "getPackageInfo: return null " );
            return null;
        }
//        Log.e(TAG, "getPackageInfo: return null " );
        return null;
    }

    public static String getLuncherActivityName(File dir) {
        File axml = new File(dir, "AndroidManifest.xml");
        File xml = new File(dir, "Manifest.xml");
        try {
            AxmlUtils.decode(axml.getAbsolutePath(), xml.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<String> activityNames;
        try {
            AndroidManifestParser parser = new AndroidManifestParser();
            Document doc = parser.parseXmlFile(xml);
            activityNames = parser.getLauncherActivityNames(doc);
//            Log.d(TAG,"Launcher Activity Names: " + activityNames);
            return activityNames.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}