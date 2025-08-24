package com.crack.vapp.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    public static void unzipApk(File apkFile, File targetDir) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(apkFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File file = new File(targetDir, zipEntry.getName());
                // Log.d("extracted file: " + file.getAbsolutePath());
                if (zipEntry.isDirectory() || zipEntry.getName().equals("")) {
                    file.mkdirs();
                } else {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }
    }

    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}