package com.crack.vapp.core;

import android.content.Intent;
import android.content.pm.ApplicationInfo;

import static com.crack.vapp.BaseApplication.baseActivity;
import static com.crack.vapp.BaseApplication.baseContext;
import static com.crack.vapp.BaseApplication.pluginPackageInfo;
import com.crack.vapp.utils.PackageParser;

import java.io.File;

public class Begin {
    private static final String TAG = "Begin";

    public static void begin(ApplicationInfo appInfo){
        String pluginApkPath = appInfo.sourceDir;
        String pluginPackageName = appInfo.packageName;
        File pluginWorkDir = new File(baseActivity.getFilesDir(), pluginPackageName);

        BypassSignature.replaceCreators(appInfo.packageName);
        BypassSignature.redirectSyscall();

        RedirectIO.redirect(pluginWorkDir);

        EnableActivity.hook();
        EnableService.hook();
        EnableContentProvider.hook();
        EnableBroadcastReceiver.hook();
        EnableBroadcastReceiver.parseStaticReceivers(pluginApkPath);

        EnableResource.mergeResources(baseContext, pluginApkPath);
        EnableResource.replaceAssetManager();
        File pluginApkFile = new File(pluginApkPath);
        EnableDex.mergeDexElements(pluginApkFile);

        pluginPackageInfo = PackageParser.getPackageInfo(pluginApkPath, baseContext);
        LunchApp.lunch(pluginPackageInfo.applicationInfo.name);
        String launcherActivityName = PackageParser.getLuncherActivityName(pluginWorkDir);
        try {
            Intent intent = new Intent(baseActivity, baseActivity.getClassLoader().loadClass(launcherActivityName));
            baseActivity.startActivity(intent);
            baseActivity.finish();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
