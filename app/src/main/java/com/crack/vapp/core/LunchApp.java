package com.crack.vapp.core;

import android.app.Application;
import android.content.ContextWrapper;

import static com.crack.vapp.BaseApplication.baseApplication;
import static com.crack.vapp.BaseApplication.baseContext;
import com.crack.vapp.utils.ReflectUtils;

import java.lang.reflect.Method;

public class LunchApp {
    private static final String TAG = "LunchApp";

    public static void lunch(String name) {
        if (name != null) {
            Class<?> application = null;
            try {
                application = baseApplication.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (application != null) {
                try {
                    Application application2 = (Application) application.getConstructor().newInstance();

                    Method attachBaseContext = ReflectUtils.getMethod(
                            application.getClass(),
                            "attachBaseContext",
                            ContextWrapper.class
                    );
                    if (attachBaseContext != null) {
                        attachBaseContext.invoke(application2, baseContext);
                    }

                    application2.onCreate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}