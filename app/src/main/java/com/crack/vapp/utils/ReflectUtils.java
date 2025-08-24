package com.crack.vapp.utils;

import android.util.Log;

import java.lang.reflect.*;

public class ReflectUtils {
    private static final String TAG = "ReflectUtils";

    // 获取特定类的 Class 对象
    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
//            Log.e(TAG, "Class not found: " + className, e);
            return null;
        }
    }

    // 获取类特定方法的 Method 对象
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
//            Log.e(TAG, "Method not found: " + methodName, e);
            return null;
        }
    }

    // 获取类特定字段的 Field 对象
    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            }
//            Log.e(TAG, "Field not found: " + fieldName, e);
            return null;
        }
    }

    // 设置对象的字段值
    public static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = getField(obj.getClass(), fieldName);
            field.set(obj, value);
        } catch (IllegalAccessException e) {
//            Log.e(TAG, "Set field failed: " + fieldName, e);
        }
    }

    // 获取对象的字段值
    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = getField(obj.getClass(), fieldName);
            return field.get(obj);
        } catch (IllegalAccessException e) {
//            Log.e(TAG, "Get field failed: " + fieldName, e);
            return null;
        }
    }

    // 获取对象所有的字段值
    private static String getClassFieldValues(Object obj) {
        Class<?> clazz = obj.getClass();
        StringBuilder sb = new StringBuilder();
        sb.append("Field values of ").append(clazz.getSimpleName()).append(":\n");
        while (clazz != null && clazz != Object.class) {
            sb.append("\n[").append(clazz.getSimpleName()).append("]\n");
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    sb.append("  ")
                            .append(field.getType().getSimpleName())
                            .append(" ")
                            .append(field.getName())
                            .append(" = ")
                            .append(field.get(obj))
                            .append("\n");
                } catch (IllegalAccessException e) {
                    sb.append("  ").append(field.getName()).append(" = [access denied]\n");
                }
            }
            clazz = clazz.getSuperclass();
        }
        return sb.toString();
    }

    // 打印对象所有的字段值
    public static void printFieldValues(Object obj) {
        try {
            Log.d(TAG, "printFieldValues: " + getClassFieldValues(obj));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 反射调用方法
    public static Object invokeMethod(Object obj, Method method, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
//            Log.e(TAG, "Invoke method failed: " + method.getName(), e);
            return null;
        }
    }
}