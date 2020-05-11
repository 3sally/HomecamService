package com.technicolor.homecamservice;

import android.os.PowerManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PowerManagerHelper {
    private static String TAG = "PowerManagerHelper : ";

    public static void goToSleep(PowerManager powerManager, long time) {
        try {
            Class<?> klass = Class.forName("android.os.PowerManager");
            Method goToSleep = klass.getMethod("goToSleep", long.class);
            Log.d(TAG, "invoke goToSleep");
            goToSleep.invoke(powerManager, time);
        } catch (InvocationTargetException e) {

            // Answer:
            e.getCause().printStackTrace();
        } catch (Exception e) {

            // generic exception handling
            e.printStackTrace();
        }
    }

    public static void wakeUp(PowerManager powerManager, long time) {
        try {
            Class<?> klass = Class.forName("android.os.PowerManager");
            Method wakeUp = klass.getMethod("wakeUp", long.class);
            Log.d(TAG, "invoke wakeUp");
            wakeUp.invoke(powerManager, time);
        } catch (InvocationTargetException e) {

            // Answer:
            e.getCause().printStackTrace();
        } catch (Exception e) {

            // generic exception handling
            e.printStackTrace();
        }
    }
}
