package com.kent.module1;


import android.os.Handler;

import com.kent.modulecaller.ModuleCaller;
import com.kent.modulecaller.ModuleClass;
import com.kent.modulecaller.ModuleMethod;

import java.util.Random;

@ModuleClass(module = "module1")
public class Module1Interface {

    private Random random = new Random();

    @ModuleMethod
    public void getValueAsync() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ModuleCaller.getInstance().onCallSuccess(random.nextInt(1000));
            }
        }, 1000);
    }

}
