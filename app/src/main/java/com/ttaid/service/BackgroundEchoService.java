package com.ttaid.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.ttaid.application.BaseApplication;
import com.ttaid.broad.BroadcastManager;
import com.ttaid.smartecho.BackgroundEcho;
import com.ttaid.util.ToastUtil;

/**
 * Created by wangfan on 2017/10/11.
 */

public class BackgroundEchoService extends Service {

    BackgroundEcho backgroundEcho;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BroadcastManager.ACTION_VOICE_EMULATE_KEY_OPEN);
        mFilter.addAction(BroadcastManager.ACTION_VOICE_EMULATE_KEY_CLOSE);
        registerReceiver(wakeBroadcast, mFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(wakeBroadcast);
        super.onDestroy();
    }

    /**
     * 唤醒广播
     */
    private BroadcastReceiver wakeBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BroadcastManager.ACTION_VOICE_EMULATE_KEY_OPEN.equals(action)) {
                ToastUtil.showShort(BaseApplication.getContext(),"接收到开启后台语音识别广播");
                backgroundEcho = new BackgroundEcho(getApplicationContext());
                backgroundEcho.init();
                backgroundEcho.start();
                backgroundEcho.startTtsOutput("主人，小T在后台为你服务");
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
            }else if(BroadcastManager.ACTION_VOICE_EMULATE_KEY_CLOSE.equals(action)){
                Log.d("wangfan", "onReceive: 接收到关闭后台语音识别广播");
                ToastUtil.showShort(BaseApplication.getContext(),"接收到关闭后台语音识别广播");
                if (backgroundEcho!=null){
                    backgroundEcho.stop();
                }
                backgroundEcho = null;
            }
        }
    };
}
