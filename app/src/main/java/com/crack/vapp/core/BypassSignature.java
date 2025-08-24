package com.crack.vapp.core;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import static com.crack.vapp.BaseApplication.baseApplication;
import com.crack.vapp.utils.ReflectUtils;

import java.io.File;
import java.util.Map;

public class BypassSignature {
    private static final String TAG = "BypassSignature";

    private static boolean ifExists = false;
    private static String apkFileName = null;
    public static void replaceCreators(String packageName) {
        String fakeSignatureData = getFakeSignatureData(packageName);
        if (fakeSignatureData == null) {
            return;
        }

        Signature fakeSignature = new Signature(Base64.decode(fakeSignatureData, Base64.DEFAULT));
        Parcelable.Creator<PackageInfo> originalCreator = PackageInfo.CREATOR;
        Parcelable.Creator<PackageInfo> creator = new Parcelable.Creator<>() {
            @Override
            public PackageInfo createFromParcel(Parcel source) {
                PackageInfo packageInfo = originalCreator.createFromParcel(source);
                if (packageInfo.packageName.equals(packageName)) {
                    if (packageInfo.signingInfo != null) {
                        Signature[] signaturesArray = packageInfo.signingInfo.getApkContentsSigners();
                        if (signaturesArray != null && signaturesArray.length > 0) {
                            signaturesArray[0] = fakeSignature;
                        }
                    }
                }
                return packageInfo;
            }

            @Override
            public PackageInfo[] newArray(int size) {
                return originalCreator.newArray(size);
            }
        };
        try {
            ReflectUtils.getField(PackageInfo.class, "CREATOR").set(null, creator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Object cache = ReflectUtils.getField(PackageManager.class, "sPackageInfoCache").get(null);
            cache.getClass().getMethod("clear").invoke(cache);
        } catch (Throwable ignored) {
        }

        try {
            Map<?, ?> mCreators = (Map<?, ?>) ReflectUtils.getField(Parcel.class, "mCreators").get(null);
            mCreators.clear();
        } catch (Throwable ignored) {
        }

        try {
            Map<?, ?> sPairedCreators = (Map<?, ?>) ReflectUtils.getField(Parcel.class, "sPairedCreators").get(null);
            sPairedCreators.clear();
        } catch (Throwable ignored) {
        }
    }

    private static String getFakeSignatureData(String packageName) {
        File bypassDir = new File(Environment.getExternalStorageDirectory(), "BypassSignature");
        if (!bypassDir.exists() || !bypassDir.isDirectory()) {
            return null;
        }

        File[] files = bypassDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".apk"));
        if (files == null) {
            return null;
        }

        for (File apkFile : files) {
            String apkPackageName = getPackageName(apkFile);
            if (packageName.equals(apkPackageName)) {
                ifExists = true;
                apkFileName = apkFile.getName();
                Signature[] signatures = getSignatureData(apkFile);
                if (signatures != null && signatures.length > 0) {
                    return signatures[0].toCharsString();
                }
            }
        }

        ifExists = false;
        apkFileName = null;
        return null;
    }

    private static String getPackageName(File apkFile) {
        try {
            PackageManager packageManager = baseApplication.getPackageManager();
            PackageInfo info = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            return info != null ? info.packageName : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Signature[] getSignatureData(File apkFile) {
        try {
            PackageManager packageManager = baseApplication.getPackageManager();
            PackageInfo info = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(),
                    PackageManager.GET_SIGNING_CERTIFICATES);

            if (info != null && info.signingInfo != null) {
                return info.signingInfo.getApkContentsSigners();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void redirectSyscall() {
        if (!ifExists) {
            return;
        }
        initSeccomp(apkFileName);
    }

    public static native void initSeccomp(String FileName);
}