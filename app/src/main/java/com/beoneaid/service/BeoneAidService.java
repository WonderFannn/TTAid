package com.beoneaid.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.beoneaid.api.IBeoneAidService;
import com.beoneaid.api.IBeoneAidServiceCallback;
import com.beoneaid.application.BaseApplication;
import com.beoneaid.broad.BroadcastManager;
import com.beoneaid.smartecho.BeoneAid;
import com.beoneaid.util.LogUtil;
import com.beoneaid.util.ToastUtil;

import java.io.IOException;

/**
 * Created by wangfan on 2017/10/12.
 */

public class BeoneAidService extends Service implements BeoneAid.OnRecognizeResultListener {

    private static final String TAG = "BeoneAidService";

    public static final String SMART_ECHO_ACTION_START = "com.rockchip.echoOnWakeUp.ACTION.START";
    public static final String SMART_ECHO_ACTION_WAKEUP = "com.rockchip.echoOnWakeUp.ACTION.CAE.WAKEUP";
    public static final String SMART_ECHO_ACTION_NETWORK_DISCONNECTED = "com.rockchip.echoOnWakeUp.ACTION.NETWORK.DISCONNECTED";
    public static final String SMART_ECHO_ACTION_NETWORK_CONNECTED = "com.rockchip.echoOnWakeUp.ACTION.NETWORK.CONNECTED";
    public static final String SMART_ECHO_ACTION_GET_BATTERY = "android.intent.action.show_batteryinfo";
    public static final String SMART_ECHO_ACTION_POWER_KEY_LONG_PRESS = "com.android.interceptPowerKeyLongPress";
    public static final String SMART_ECHO_ACTION_POWER_KEY_UP = "com.android.interceptPowerKeyUp";
    public static final String SMART_ECHO_UPDATE_SUCCESS = "com.android.update_success";
    public static final String SMART_ECHO_INSTALL_APK = "com.android.install_apk";

    private AudioManager mAm;
    private boolean checkNet = true;
    private BeoneAid mBeoneAid;
    private boolean isEchoRunning = false;
    private boolean needSayPowerUp = false;

    
    @Override
    public void onCreate() {
        super.onCreate();
        mAm = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mBeoneAid = new BeoneAid(getApplicationContext());
        mBeoneAid.start();
        isEchoRunning = true;
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
            Log.d(TAG, "onStartCommand: "+action);
        }
        if (action != null) {
            LogUtil.d("BeoneAidService - onStartCommand - " + action);
            if (SMART_ECHO_ACTION_START.equals(action)) {
                if (isEchoRunning) {
                    mBeoneAid.startTtsOutput("哔湾助手在后台", false);
                }
            } else if(SMART_ECHO_ACTION_WAKEUP.equals(action)) {
                int mode = intent.getIntExtra("mode",0);
                mBeoneAid.onWakeUp(0, 0, mode);
            } else if(SMART_ECHO_UPDATE_SUCCESS.equals(action)) {
                if (isEchoRunning) {
                    mBeoneAid.startTtsOutput("更新成功", false);
                }
            } else if(SMART_ECHO_INSTALL_APK.equals(action)) {
                if (isEchoRunning) {
                    mBeoneAid.needInstallApk = true;
                    mBeoneAid.filePath = intent.getStringExtra("filePath");
                    mBeoneAid.startTtsOutput("主人，要更新到最新版本吗，如需更新请回复确定");
                }
            } else if (SMART_ECHO_ACTION_NETWORK_DISCONNECTED.equals(action)){
                if (isEchoRunning){
                    if (checkNet) {
                        checkNet = false;
                        new Handler().postDelayed(new Runnable(){
                            public void run() {
                                checkNet = true;
                            }
                        }, 2000);
                        ToastUtil.showTopToast(BaseApplication.getContext(), "主人，网络断开了，我休息了");
                        AssetManager assetManager = getApplicationContext().getAssets();
                        AssetFileDescriptor afd = null;
                        try {
                            afd = assetManager.openFd("net_disconnected.wav");
                            mAm.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                            MediaPlayer player = new MediaPlayer();
                            player.setDataSource(afd.getFileDescriptor(),
                                    afd.getStartOffset(), afd.getLength());
                            player.prepare();
                            player.start();
                            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    mAm.abandonAudioFocus(null);
                                }
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (SMART_ECHO_ACTION_NETWORK_CONNECTED.equals(action)){
                if (isEchoRunning) {
                    mBeoneAid.startTtsOutput("主人，网络连接了，我回来了", false);
                }
            } else if (SMART_ECHO_ACTION_GET_BATTERY.equals(action)){
                if (isEchoRunning) {
                    mBeoneAid.startTtsOutput("当前电量还有百分之"+ (int)(getBattery()*100), false);
                }
            } else if (SMART_ECHO_ACTION_POWER_KEY_LONG_PRESS.equals(action)){
                if (isEchoRunning){
                    mBeoneAid.sayPowerLongPress();
                }
                needSayPowerUp = true;
            } else if (SMART_ECHO_ACTION_POWER_KEY_UP.equals(action)){
                if (isEchoRunning && needSayPowerUp){
                    mBeoneAid.sayPowerUp();
                }
                needSayPowerUp = false;
            } else if (BroadcastManager.OTA_SW_UPDATE.equals(action)){
                //固件更新
                String type = intent.getStringExtra("type");
                if (type.equals("local")){
                    type = "本地";
                }
                if (isEchoRunning){
                    mBeoneAid.needUpdateOTA = true;
                    mBeoneAid.startTtsOutput("检测到系统有"+type+"更新，如需升级请回复确定");
                }
            }else if (BroadcastManager.SPEAK_TEXT.equals(action)){
                if (isEchoRunning){
                    mBeoneAid.needUpdateOTA = true;
                    mBeoneAid.startTtsOutput(intent.getStringExtra("speakText"),false);
                }
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

    /***
     * 获取系统电量
     */
    private float getBattery(){
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(intent == null){
            return 0f;
        }
        int level = intent.getIntExtra("level", 0);
        int scale = intent.getIntExtra("scale", 100);
        return ((float)level) / scale;
    }
}
