package com.beoneaid.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.beoneaid.smartecho.BeoneAid;
import com.beoneaid.util.LogUtil;

/**
 * Created by wangfan on 2017/10/12.
 */

public class BeoneAidService extends Service {

    public static final String SMART_ECHO_ACTION_START = "com.rockchip.echoOnWakeUp.ACTION.START";
    public static final String SMART_ECHO_ACTION_WAKEUP = "com.rockchip.echoOnWakeUp.ACTION.CAE.WAKEUP";

    private BeoneAid mBeoneAid;
    private boolean isEchoRunning = false;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBeoneAid = new BeoneAid(getApplicationContext());
        mBeoneAid.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBeoneAid.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if (action != null) {
            LogUtil.d("BeoneAidService - onStartCommand - " + action);
            if (SMART_ECHO_ACTION_START.equals(action)) {
                if (!isEchoRunning) {
                    mBeoneAid.startTtsOutput("哔湾助手在后台", false);
                }
            } else if(SMART_ECHO_ACTION_WAKEUP.equals(action)) {
                mBeoneAid.onWakeUp(0, 0);
            }
        }
        return START_STICKY;
    }

}
