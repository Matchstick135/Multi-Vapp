package com.crack.vapp.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AppInfoUtils {
    private static final String FILE_NAME = "string_list.dat";

    public static List<ApplicationInfo> getAllNonSystemApps(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        List<ApplicationInfo> nonSystemApps = new ArrayList<>();
        for (ApplicationInfo appInfo : allApps) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                nonSystemApps.add(appInfo);
            }
        }
        return nonSystemApps;
    }

    public static List<ApplicationInfo> getSavedAppInfos(Context context) {
        List<String> savedPackageNames = getAllStrings(context);
        if (savedPackageNames == null || savedPackageNames.isEmpty()) {
            return new ArrayList<>();
        }

        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        List<ApplicationInfo> savedAppInfos = new ArrayList<>();
        for (ApplicationInfo appInfo : allApps) {
            if (savedPackageNames.contains(appInfo.packageName)) {
                savedAppInfos.add(appInfo);
            }
        }
        return savedAppInfos;
    }

    public static void saveString(Context context, String str) {
        List<String> stringList = loadStringList(context);
        if (stringList == null) {
            stringList = new ArrayList<>();
        }
        if (!stringList.contains(str)) {
            stringList.add(str);
            saveStringList(context, stringList);
        }
    }

    public static List<String> getAllStrings(Context context) {
        List<String> stringList = loadStringList(context);
        return stringList != null ? stringList : new ArrayList<>();
    }

    private static void saveStringList(Context context, List<String> stringList) {
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(stringList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> loadStringList(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            List<String> defaultList = new ArrayList<>();
            return defaultList;
        }
        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (List<String>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
