package com.crack.vapp.core;

import java.io.File;

public class RedirectIO {
    private static final String TAG = "RedirectIO";

    private static native void redirectIO(String[][] redirectRules);

    public static void redirect(File pluginWorkDir) {
        String workDirPath = pluginWorkDir.getAbsolutePath();

        String[][] redirectRules = {
                {workDirPath + "/data/", "/data/data/([^/]+)/", "/data/user/0/([^/]+)/"},
                {workDirPath + "/sdcard/", "/sdcard/", "/mnt/sdcard/", "/storage/emulated/0/"},
                {workDirPath + "/app/lib/", "/data/app/([^/]+)/lib/"}
        };

        redirectIO(redirectRules);
    }
}