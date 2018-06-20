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
    private static final String TAG = "BeoneAidReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null) {
            return;
        }
        Log.d(TAG, "onReceive: "+intent.getAction());
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
            context.startService(i);
        }else if (BroadcastManager.UPDATE_SUCCESS.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BeoneAidService.SMART_ECHO_UPDATE_SUCCESS);
            context.startService(i);
        }else if (BroadcastManager.INSTALL_APK.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BeoneAidService.SMART_ECHO_INSTALL_APK);
            i.putExtra("filePath",intent.getStringExtra("filePath"));
            context.startService(i);
        }else if (BroadcastManager.WAKEUP_BYUSER.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_WAKEUP);
            context.startService(i);
        }else if (BroadcastManager.GET_BATTERY_INFO.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_GET_BATTERY);
            context.startService(i);
        }else if (BroadcastManager.POWER_KEY_LONG_PRESS.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_POWER_KEY_LONG_PRESS);
            context.startService(i);
        }else if (BroadcastManager.POWER_KEY_UP.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_POWER_KEY_UP);
            context.startService(i);
        }else if (BroadcastManager.OTA_SW_UPDATE.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BroadcastManager.OTA_SW_UPDATE);
            i.putExtra("type",intent.getStringExtra("type"));
            context.startService(i);
        }else if (BroadcastManager.SPEAK_TEXT.equals(intent.getAction())){
            Intent i = new Intent(context, BeoneAidService.class);
            i.setAction(BroadcastManager.SPEAK_TEXT);
            i.putExtra("speakText",intent.getStringExtra("speakText"));
            context.startService(i);
        }
    }
}
