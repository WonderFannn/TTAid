package com.beoneaid.broad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.beoneaid.service.BeoneAidService;

/**
 * Created by wangfan on 2017/10/12.
 */

public class BeoneAidReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null) {
            return;
        }
        Log.d("BeoneAid", "onReceive: "+intent.getAction());
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BeoneAid", "==================== BeoneAidService Start ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
            context.startService(i);
        }else if (BroadcastManager.UPDATE_SUCCESS.equals(intent.getAction())){
            Log.d("BeoneAid", "==================== BeoneAidService UPDATE_SUCCESS ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_UPDATE_SUCCESS);
            context.startService(i);
        }else if (BroadcastManager.INSTALL_APK.equals(intent.getAction())){
            Log.d("BeoneAid", "==================== BeoneAidService INSTALL_APK ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_INSTALL_APK);
            i.putExtra("filePath",intent.getStringExtra("filePath"));
            context.startService(i);
        }else if (BroadcastManager.WAKEUP_BYUSER.equals(intent.getAction())){
            Log.d("BeoneAid", "==================== BeoneAidService WakeUp ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_WAKEUP);
            context.startService(i);
        }else if (BroadcastManager.GET_BATTERY_INFO.equals(intent.getAction())){
            Log.d("BeoneAid", "==================== BeoneAidService get battery ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_GET_BATTERY);
            context.startService(i);
        }else if (BroadcastManager.POWER_KEY_LONG_PRESS.equals(intent.getAction())){
            Log.d("BeoneAid", "==================== BeoneAidService power key long press ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_POWER_KEY_LONG_PRESS);
            context.startService(i);
        }else if (BroadcastManager.POWER_KEY_UP.equals(intent.getAction())){
            Log.d("BeoneAid", "==================== BeoneAidService power key up ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_POWER_KEY_UP);
            context.startService(i);
        }
    }
}
