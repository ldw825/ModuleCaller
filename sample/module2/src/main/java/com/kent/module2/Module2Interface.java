package com.kent.module2;

import android.content.Context;
import android.content.Intent;

import com.kent.modulecaller.ModuleClass;
import com.kent.modulecaller.ModuleMethod;

@ModuleClass(module = "module2")
public class Module2Interface {

    @ModuleMethod
    public void jumpActivity(Context context) {
        Intent intent = new Intent(context, Module2Activity.class);
        context.startActivity(intent);
    }

}
