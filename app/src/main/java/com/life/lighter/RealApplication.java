package com.life.lighter;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

/**
 * @author zhoujishi
 * @description
 * @date 2020/6/3
 */
public class RealApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }
}
