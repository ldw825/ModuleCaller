package com.kent.modulecaller;

import android.util.Log;

/**
 * @author Kent
 * @version 1.0
 * @date 2019/05/02
 */
class LogUtils {

    // 日志TAG前缀
    private static final String GLOBAL_TAG_PREFIX = "MODULE_CALLER_";
    // 是否启用verbose级以上的log打印
    private static final boolean ENABLE_VERBOSE = false;
    // 是否启用log打印，动态判断标记
    static boolean sIsLogOn;

    public static void v(String msg) {
        if (ENABLE_VERBOSE || sIsLogOn) {
            String[] logInfo = getLogInfo();
            Log.v(logInfo[0], logInfo[1] + msg);
        }
    }

    public static void d(String msg) {
        if (ENABLE_VERBOSE || sIsLogOn) {
            String[] logInfo = getLogInfo();
            Log.d(logInfo[0], logInfo[1] + msg);
        }
    }

    public static void i(String msg) {
        if (ENABLE_VERBOSE || sIsLogOn) {
            String[] logInfo = getLogInfo();
            Log.i(logInfo[0], logInfo[1] + msg);
        }
    }

    public static void w(String msg) {
        if (ENABLE_VERBOSE || sIsLogOn) {
            String[] logInfo = getLogInfo();
            Log.w(logInfo[0], logInfo[1] + msg);
        }
    }

    public static void e(String msg) {
        if (ENABLE_VERBOSE || sIsLogOn) {
            String[] logInfo = getLogInfo();
            Log.e(logInfo[0], logInfo[1] + msg);
        }
    }

    private static String[] getLogInfo() {
        StackTraceElement element = Thread.currentThread().getStackTrace()[4];
        String fileName = element.getFileName();
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        if (fileName.contains(".")) {
            fileName = fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        String methodName = element.getMethodName();
        return new String[]{GLOBAL_TAG_PREFIX + fileName, methodName + "->"};
    }

}
