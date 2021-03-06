package com.technicolor.homecamservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            Log.i("BootReceiver", "intent received");

//            Intent myIntent = new Intent(context, MainActivity.class);
//            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent serviceIntent = new Intent(context, HomeCamService.class);
            context.startActivity(serviceIntent);
        }
    }
}
