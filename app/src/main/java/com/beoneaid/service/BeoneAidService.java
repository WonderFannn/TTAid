package com.beoneaid.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.beoneaid.api.IBeoneAidService;
import com.beoneaid.api.IBeoneAidServiceCallback;
import com.beoneaid.smartecho.BeoneAid;
import com.beoneaid.util.LogUtil;

/**
 * Created by wangfan on 2017/10/12.
 */

public class BeoneAidService extends Service implements BeoneAid.OnRecognizeResultListener {

    public static final String SMART_ECHO_ACTION_START = "com.rockchip.echoOnWakeUp.ACTION.START";
    public static final String SMART_ECHO_ACTION_WAKEUP = "com.rockchip.echoOnWakeUp.ACTION.CAE.WAKEUP";

    private BeoneAid mBeoneAid;
    private boolean isEchoRunning = false;
    @Override
    public void onCreate() {
        super.onCreate();
        mBeoneAid = new BeoneAid(getApplicationContext());
        mBeoneAid.start();
        mBeoneAid.setOnRecognizeResultListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //当服务被绑定时切换到api模式
        mBeoneAid.setParseMode(0);
        Log.d("TAG", "onBind: ");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mBeoneAid.setParseMode(0);
        Log.d("TAG", "onRebind: ");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("TAG", "onUnbind: ");
        super.onUnbind(intent);
        return true;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = null;
        if (intent != null){
            action = intent.getAction();
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBeoneAid.stop();
        mCallbacks.kill();
        Intent intent = new Intent(this, BeoneAidService.class);
        intent.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
        startService(intent);
    }

    void callback(String result) {
        final int N = mCallbacks.beginBroadcast();
        Log.d("TAG", "callback: N=="+N);
        for (int i=0; i<N; i++) {
            try {
                Log.d("TAG", "callback: try" + result);
                mCallbacks.getBroadcastItem(i).recognizeResultCallback(result);
            } catch (RemoteException e) {
                Log.e("TAG", "callback: E ==" + e.getMessage());
            }
        }
        mCallbacks.finishBroadcast();
    }

    private final IBeoneAidService.Stub mBinder = new IBeoneAidService.Stub() {

        public void startSpeakingWithoutRecognize(String s){
            mBeoneAid.startTtsOutput(s,false);
        }

        public void startSpeaking(String s){
            mBeoneAid.startTtsOutput(s);
        }

        public void setMode(int i){
            mBeoneAid.setParseMode(i);
        }

        public void registerCallback(IBeoneAidServiceCallback cb) {
            if (cb != null) {
                mCallbacks.register(cb);
            }
        }

        public void unregisterCallback(IBeoneAidServiceCallback cb) {
            if(cb != null) {
                mCallbacks.unregister(cb);
            }
        }
    };

    final RemoteCallbackList<IBeoneAidServiceCallback> mCallbacks = new RemoteCallbackList <IBeoneAidServiceCallback>();

    @Override
    public void onRecognizeResult(String result) {
        callback(result);
    }

}
