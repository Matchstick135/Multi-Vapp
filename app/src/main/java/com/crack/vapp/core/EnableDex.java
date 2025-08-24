package com.crack.vapp.core;

import android.content.Context;

import static com.crack.vapp.BaseApplication.baseContext;
import static com.crack.vapp.BaseApplication.baseActivity;
import com.crack.vapp.utils.ReflectUtils;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

public class EnableDex {
    static final String TAG = "EnableDex";

    public static void insertClassLoaderChain(File apk) {
        Context context = baseActivity;

        ClassLoader baseClassLoader = context.getClassLoader();

        File optimizedDir = context.getDir("oapk", Context.MODE_PRIVATE);
        DexClassLoader pluginClassLoader = new DexClassLoader(
                apk.getAbsolutePath(),
                optimizedDir.getAbsolutePath(),
                null,
                baseClassLoader
        );

        try {
            Object originClassLoader = ReflectUtils.getFieldValue(baseClassLoader, "parent");

            ReflectUtils.setFieldValue(baseClassLoader, "parent", pluginClassLoader);
            ReflectUtils.setFieldValue(pluginClassLoader, "parent", originClassLoader);
        } catch (Exception e) {
        }
    }

    public static void mergeDexElements(File apk) {
        ArrayList<Object[]> DexElementsList = new ArrayList<>();
        int sumLength = 0;

        Class<?> clazz = ReflectUtils.getClass("dalvik.system.BaseDexClassLoader");
        Field pathListFiled;
        try {
            pathListFiled = clazz.getDeclaredField("pathList");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        pathListFiled.setAccessible(true);

        File optimizedDir = baseContext.getDir("odex", Context.MODE_PRIVATE);
        DexClassLoader pluginClassLoader = new DexClassLoader(
                apk.getAbsolutePath(),
                optimizedDir.getAbsolutePath(),
                null,
                baseContext.getClassLoader()
        );

        Object dexPathList;
        dexPathList = ReflectUtils.getFieldValue(pluginClassLoader, "pathList");
        Object[] pluginDexElements = (Object[]) ReflectUtils.getFieldValue(dexPathList, "dexElements");
        DexElementsList.add(pluginDexElements);
        sumLength += pluginDexElements.length;

        ClassLoader baseClassLoader = baseContext.getClassLoader();
        dexPathList = ReflectUtils.getFieldValue(baseClassLoader, "pathList");
        Object[] baseDexElements = (Object[]) ReflectUtils.getFieldValue(dexPathList, "dexElements");
        DexElementsList.add(baseDexElements);
        sumLength += baseDexElements.length;

        Object[] mergedDexElements = (Object[]) Array.newInstance(
                baseDexElements.getClass().getComponentType(),
                sumLength
        );
        int index = 0;
        for (Object[] dexElement : DexElementsList) {
            System.arraycopy(dexElement, 0, mergedDexElements, index, dexElement.length);
            index += dexElement.length;
        }

        ReflectUtils.setFieldValue(dexPathList, "dexElements", mergedDexElements);
    }
}