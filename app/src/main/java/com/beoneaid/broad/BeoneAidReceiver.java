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
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BeoneAid", "==================== BeoneAidService Start ======================");
            Intent i = new Intent(context, BeoneAidService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
            context.startService(i);
        }
    }
}
