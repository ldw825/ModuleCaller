package com.kent.modulecaller.sample;

import android.app.Application;
import android.content.Context;

import com.kent.modulecaller.ModuleCaller;

/**
 * @author Kent
 * @version 1.0
 * @date 2019/05/04
 */
public class SampleApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ModuleCaller.init(base);
        ModuleCaller.enableLogger(true);
    }

}
