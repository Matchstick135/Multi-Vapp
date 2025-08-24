package com.crack.vapp.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import static com.crack.vapp.utils.FileUtils.deleteDirectory;
import static com.crack.vapp.utils.FileUtils.unzipApk;

import java.io.File;
import java.util.Objects;

public class InstallApp {
    public static void install(Context context, ApplicationInfo appInfo) {
        try {
            String apkPath = appInfo.sourceDir;
            File apkFile = new File(apkPath);

            String packageName = appInfo.packageName;
            File workDir = new File(context.getFilesDir(), packageName);
            if (workDir.exists()) {
                deleteDirectory(workDir);
            }

            if (!Objects.requireNonNull(workDir.getParentFile()).exists()) {
                workDir.getParentFile().mkdirs();
            }
            workDir.mkdirs();

            File dataDir = new File(workDir, "data");
            File appDir = new File(workDir, "app");
            File sdcardDir = new File(workDir, "sdcard");
            dataDir.mkdirs();
            appDir.mkdirs();
            sdcardDir.mkdirs();

            unzipApk(apkFile, appDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}