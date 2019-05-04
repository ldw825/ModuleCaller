package com.kent.modulecaller;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Kent
 * @version 1.0
 * @date 2019/05/02
 */
public class ModuleCaller {

    private static volatile ModuleCaller sInstance;
    private static boolean sIsInitialized;
    private static final ModuleScanner SCANNER = ModuleScanner.newInstance();
    // 等待队列
    private CopyOnWriteArrayList<Command> mWaitingList;
    // 执行队列
    private CopyOnWriteArrayList<Command> mExecutingList;
    private Handler mHandler;
    private Command mTempCommand;

    private ModuleCaller() {
        mWaitingList = new CopyOnWriteArrayList<>();
        mExecutingList = new CopyOnWriteArrayList<>();
        mHandler = new Handler();
    }

    public static ModuleCaller getInstance() {
        if (sInstance == null) {
            synchronized (ModuleCaller.class) {
                if (sInstance == null) {
                    sInstance = new ModuleCaller();
                }
            }
        }
        return sInstance;
    }

    /**
     * 在app模块初始化时调用
     *
     * @param context
     */
    public static void init(final Context context) {
        if (sIsInitialized) {
            throw new IllegalStateException("ModuleCaller has been initialized!");
        }
        sIsInitialized = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                SCANNER.scanModules(context);
            }
        }).start();
    }

    /**
     * 是否打开日志
     *
     * @param enable
     */
    public static void enableLogger(boolean enable) {
        LogUtils.sIsLogOn = enable;
    }

    public void call() {
        if (mTempCommand == null) {
            LogUtils.e("no setAction set, can not call other modules");
            return;
        }
        Class<?> clazz = SCANNER.getModuleClass(mTempCommand.getAction());
        if (clazz == null) {
            LogUtils.e("no class found for action:" + mTempCommand.getAction());
            return;
        }
        try {
            // action
            String action = mTempCommand.getAction();
            String methodName = action.substring(action.lastIndexOf(".") + 1);
            // params
            Class<?>[] paramTypes = null;
            Object[] params = mTempCommand.getParams();
            if (params != null && params.length > 0) {
                paramTypes = new Class<?>[params.length];
                for (int i = 0; i < params.length; i++) {
                    paramTypes[i] = params[i].getClass();
                }
            }
            if (false) {
                LogUtils.d("methodName=" + methodName + ", paramTypes=" + paramTypes);
                if (paramTypes != null && paramTypes.length > 0) {
                    for (Class<?> cls : paramTypes) {
                        LogUtils.d("cls=" + cls.getName());
                    }
                }
            }

            Method targetMethod = null;
            Method[] methods = clazz.getMethods();
            if (methods != null && methods.length > 0) {
                for (Method m : methods) {
                    if (m.getName().equals(methodName)) {
                        Class<?>[] types = m.getParameterTypes();
                        if (types == null || types.length == 0) {
                            // 方法无参数
                            if (paramTypes == null) {
                                targetMethod = m;
                                break;
                            }
                        } else {
                            // 参数个数不相等
                            if (types.length != paramTypes.length) {
                                throw new IllegalArgumentException("mismatch parameters count for method " +
                                        methodName);
                            }
                            for (int i = 0; i < types.length; i++) {
                                Class<?> type = types[i];
                                Class<?> param = paramTypes[i];
                                if (param != null && !type.isAssignableFrom(param)) {
                                    throw new IllegalArgumentException("mismatch parameter for method " + methodName
                                            + ", required type=" + type.getName() + ", passed type=" + param.getName());
                                }
                            }
                            targetMethod = m;
                            break;
                        }
                    }
                }
            }

            LogUtils.d("targetMethod=" + targetMethod);
            if (targetMethod != null) {
                // callback
                Callback callback = mTempCommand.getCallback();
                if (callback == null) {
                    // 直接执行
                    execute(mTempCommand, clazz, targetMethod);
                } else {
                    // 添加到等待队列
                    Command command = new Command(mTempCommand);
                    enqueueWatingList(command, clazz, targetMethod);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            LogUtils.e("SecurityException for action:" + mTempCommand.getAction());
            doCallFailed(mTempCommand.getAction(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e("Exception for action:" + mTempCommand.getAction());
            doCallFailed(mTempCommand.getAction(), e.getMessage());
        } finally {
            mTempCommand = null;
        }
    }

    // 如果是异步命令，添加到等待队列
    private synchronized void enqueueWatingList(final Command command, final Class<?> clazz, final Method method) {
        mWaitingList.add(command);
        // 延迟1毫秒执行
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mExecutingList.add(command);
                mWaitingList.remove(command);
                execute(command, clazz, method);
            }
        }, 1);
    }

    private void execute(Command command, Class<?> clazz, Method method) {
        try {
            method.setAccessible(true);
            method.invoke(clazz.newInstance(), command.getParams());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            LogUtils.e("IllegalAccessException for action:" + command.getAction());
            doCallFailed(command.getAction(), e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            LogUtils.e("InvocationTargetException for action:" + command.getAction());
            doCallFailed(command.getAction(), e.getMessage());
        } catch (InstantiationException e) {
            e.printStackTrace();
            LogUtils.e("InstantiationException for action:" + command.getAction());
            doCallFailed(command.getAction(), e.getMessage());
        }
    }

    // 异步调用，成功后回调
    public synchronized void onCallSuccess(Object result) {
        String action = parseAction();
        doCallSuccess(action, result);
    }

    private void doCallSuccess(String action, Object result) {
        if (TextUtils.isEmpty(action)) {
            return;
        }
        Command target = null;
        for (Command command : mExecutingList) {
            if (command.getAction().equals(action)) {
                target = command;
                break;
            }
        }
        if (target != null) {
            mExecutingList.remove(target);
            Callback callback = target.getCallback();
            if (callback != null) {
                callback.onCallSuccess(action, result);
            }
        }
    }

    // 异步调用，失败后回调
    public synchronized void onCallFailed(String message) {
        String action = parseAction();
        doCallFailed(action, message);
    }

    private void doCallFailed(String action, String message) {
        if (TextUtils.isEmpty(action)) {
            return;
        }
        Command target = null;
        for (Command command : mExecutingList) {
            if (command.getAction().equals(action)) {
                target = command;
                break;
            }
        }
        if (target != null) {
            mExecutingList.remove(target);
            Callback callback = target.getCallback();
            if (callback != null) {
                callback.onCallFailed(action, message);
            }
        }
    }

    // 解析调用onCallSuccess或onCallFailed的类的action
    private String parseAction() {
        StackTraceElement element = Thread.currentThread().getStackTrace()[4];
        String fileName = element.getFileName();
        fileName = fileName.substring(0, fileName.indexOf("."));
        String topClassName = element.getClassName();
        topClassName = topClassName.substring(0, topClassName.indexOf(fileName) + fileName.length());
        LogUtils.d("topClassName=" + topClassName);
        try {
            Class<?> topClass = Class.forName(topClassName);
            String action = SCANNER.getAction(topClass);
            if (!TextUtils.isEmpty(action)) {
                return action;
            }
            Class<?>[] classes = topClass.getDeclaredClasses();
            for (int i = 0; i < classes.length; i++) {
                Class<?> clazz = classes[i];
                action = SCANNER.getAction(clazz);
                if (!TextUtils.isEmpty(action)) {
                    return action;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ModuleCaller action(String action) {
        if (mTempCommand == null) {
            mTempCommand = new Command();
        }
        mTempCommand.setAction(action);
        return this;
    }

    public ModuleCaller callback(Callback callback) {
        if (mTempCommand == null) {
            mTempCommand = new Command();
        }
        mTempCommand.setCallback(callback);
        return this;
    }

    public ModuleCaller params(Object... params) {
        if (mTempCommand == null) {
            mTempCommand = new Command();
        }
        mTempCommand.setParams(params);
        return this;
    }

    private static class Command {
        private String action;
        private Object[] params;
        private WeakReference<Callback> callbackRef;

        public Command() {
        }

        public Command(Command origin) {
            this.action = origin.action;
            this.params = origin.params;
            this.callbackRef = origin.callbackRef;
        }

        public Command setAction(String action) {
            this.action = action;
            return this;
        }

        public String getAction() {
            return action;
        }

        public Command setParams(Object... params) {
            this.params = params;
            return this;
        }

        public Object[] getParams() {
            return params;
        }

        public Command setCallback(Callback callback) {
            this.callbackRef = new WeakReference<>(callback);
            return this;
        }

        public Callback getCallback() {
            if (callbackRef != null) {
                return callbackRef.get();
            }
            return null;
        }

    }

    /**
     * 调用其他模块后的回调接口
     */
    public interface Callback {
        /**
         * 调用成功回调
         *
         * @param action 动作：模块名.接口名
         * @param result 调用成功返回的数据，可以为null
         */
        void onCallSuccess(String action, Object result);

        /**
         * 调用失败回调
         *
         * @param action  动作：模块名.接口名
         * @param message 调用失败的消息
         */
        void onCallFailed(String action, String message);
    }

}