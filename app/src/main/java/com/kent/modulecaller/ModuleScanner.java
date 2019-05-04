package com.kent.modulecaller;

import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;

/**
 * 模块扫描器
 *
 * @author Kent
 * @version 1.0
 * @date 2019/05/02
 */
public class ModuleScanner {

    // ModuleCaller自身的包名
    private static final String SELF_PKG = "com.kent.modulecaller";
    // 类名前缀在以下列表中的类将不会扫描
    private static final List<String> EXCLUDED_CLASS_NAME_PREFIXES = Arrays.asList(SELF_PKG,
            "android.", "com.google.", "com.android.");

    private Map<String, WeakReference<Class<?>>> mModuleClassMap;

    private ModuleScanner() {
        init();
    }

    public static ModuleScanner newInstance() {
        return new ModuleScanner();
    }

    private void init() {
        mModuleClassMap = new ArrayMap<>();
    }

    public void destory() {
        if (mModuleClassMap != null) {
            mModuleClassMap.clear();
        }
    }

    // 是否属于不扫描的类
    private boolean isExcludedClass(String className) {
        for (String prefix : EXCLUDED_CLASS_NAME_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public void scanModules(Context context) {
        ClassLoader classLoader = context.getApplicationContext().getClassLoader();
        try {
            Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathList = pathListField.get(classLoader);
            Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object dexElements = dexElementsField.get(pathList);
            int dexCount = Array.getLength(dexElements);
            LogUtils.d("dexCount=" + dexCount);
            for (int i = 0; i < dexCount; i++) {
                Object element = Array.get(dexElements, i);
                Field dexFileField = element.getClass().getDeclaredField("dexFile");
                dexFileField.setAccessible(true);
                DexFile dexFile = (DexFile) dexFileField.get(element);
                Enumeration<String> entries = dexFile.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement();
                    if (isExcludedClass(entry)) {
                        continue;
                    }
                    LogUtils.d("entry=" + entry);
                    Class<?> clazz = classLoader.loadClass(entry);
                    ModuleClass classAnno = clazz.getAnnotation(ModuleClass.class);
                    if (classAnno != null) {
                        String moduleName = classAnno.module();
                        if (!TextUtils.isEmpty(moduleName)) {
                            Method[] methods = clazz.getMethods();
                            for (Method method : methods) {
                                ModuleMethod methodAnno = method.getAnnotation(ModuleMethod.class);
                                if (methodAnno != null) {
                                    LogUtils.d("module=" + moduleName + ", method=" + method.getName()
                                            + ", class=" + clazz.getName());
                                    String key = moduleName + "." + method.getName();
                                    if (!mModuleClassMap.containsKey(key)) {
                                        WeakReference<Class<?>> r = new WeakReference<Class<?>>(clazz);
                                        mModuleClassMap.put(key, r);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Class<?> getModuleClass(String action) {
        if (!TextUtils.isEmpty(action)) {
            WeakReference<Class<?>> r = mModuleClassMap.get(action);
            if (r != null) {
                return r.get();
            }
        }
        return null;
    }

    public String getAction(Class<?> clazz) {
        Set<Map.Entry<String, WeakReference<Class<?>>>> entrySet = mModuleClassMap.entrySet();
        for (Map.Entry<String, WeakReference<Class<?>>> entry : entrySet) {
            WeakReference<Class<?>> ref = entry.getValue();
            if (ref != null) {
                if (clazz == ref.get()) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

}
