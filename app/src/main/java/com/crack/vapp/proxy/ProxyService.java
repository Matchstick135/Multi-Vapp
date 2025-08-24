package com.crack.vapp.proxy;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ProxyService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}