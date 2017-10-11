package com.ttaid.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.ttaid.application.BaseApplication;
import com.ttaid.broad.BroadcastManager;
import com.ttaid.util.JsonParser;
import com.ttaid.util.ToastUtil;

public class BackgroungSpeechRecongnizerService extends Service {

    // 语音听写对象
    public static SpeechRecognizer hearer;
    //监听线程
    ListenThread mListenThread;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    private int wakeTimes = 5;

    @Override
    public void onCreate() {
        Log.d("wangfan", "BackgroungSpeechRecongnizerService created");
        super.onCreate();
        ToastUtil.showShort(BaseApplication.getContext(),"后台语音识别服务启动了");
        //注册广播
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BroadcastManager.ACTION_VOICE_EMULATE_KEY_OPEN);
        mFilter.addAction(BroadcastManager.ACTION_VOICE_EMULATE_KEY_CLOSE);
        registerReceiver(wakeBroadcast, mFilter);

        // 初始化
        init();
    }

    @Override
    public void onDestroy() {
        ToastUtil.showShort(BaseApplication.getContext(),"后台语音识别服务已关闭");
        unregisterReceiver(wakeBroadcast);
        if (hearer != null){
            hearer.cancel();
            hearer.destroy();
        }
        if(mListenThread != null){
            mListenThread.interrupt();
            mListenThread = null;
        }
        super.onDestroy();
    }

    private void init() {
        hearer = SpeechRecognizer.createRecognizer(this, mInitListener);
        setParameter();
        mTts = SpeechSynthesizer.createSynthesizer(this, mInitListener);
        setTTSParam();
    }


    private void speakText(String text) {
        mTts.startSpeaking(text, mTtsListener);
    }
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            if (hearer.isListening()) {
                hearer.stopListening();
                if (mListenThread != null) {
                    mListenThread.interrupt();
                    mListenThread = null;
                }
            }
        }
        @Override
        public void onSpeakPaused() {
        }
        @Override
        public void onSpeakResumed() {
        }
        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
        }
        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }
        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                if (mListenThread != null) {
                    mListenThread.start();
                } else {
                    mListenThread = new ListenThread();
                    mListenThread.start();
                }
            } else if (error != null) {
//                showTip(error.getPlainDescription(true));
                ToastUtil.showShort(BaseApplication.getContext(),error.getPlainDescription(true));
            }
        }
        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };
    /**
     * 设置听写对象
     */
    private void setParameter() {
        // domain:域名
        hearer.setParameter(SpeechConstant.DOMAIN, "iat");
        // zh_cn汉语
        hearer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // mandarin:普通话
        hearer.setParameter(SpeechConstant.ACCENT, "mandarin");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        hearer.setParameter(SpeechConstant.VAD_BOS, "5000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        hearer.setParameter(SpeechConstant.VAD_EOS, "1000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        hearer.setParameter(SpeechConstant.ASR_PTT,  "0");
    }
    private void setTTSParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME,"xiaoyan");
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED,"70");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME,"80");

        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }
    public class ListenThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                while(true){
                    if (hearer != null && !hearer.isListening()) {
                        Thread.sleep(50);
                        hearer.startListening(mRecoListener);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int i) {
            if (i != ErrorCode.SUCCESS) {
                Log.i("TAG", "初始化失败，错误码：" + i);
            }
        }
    };

    /**
     * 听写监听器
     */
    private RecognizerListener mRecoListener = new RecognizerListener(){
        //听写结果回调接口(返回Json格式结果，用户可参见附录12.1)；
        //一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
        //isLast等于true时会话结束。
        public void onResult(RecognizerResult results, boolean isLast) {
            parsedResult(results, isLast);
        }

        @Override
        public void onBeginOfSpeech() {
            //开始录音
            Log.i("TAG", "开始录音!");
        }

        @Override
        public void onEndOfSpeech() {
            Log.i("TAG", "结束录音!");
        }

        @Override
        public void onError(SpeechError error) {
            if(error.getErrorCode()!=10118){
                ToastUtil.showShort(getApplicationContext(), "onError:"+error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }

        @Override
        public void onVolumeChanged(int i, byte[] b) {

        }
    };

    /**
     * 解析json并显示听写内容（执行唤醒内容）
     * @param results
     * 			json数据
     * @param isLast
     * 			是否是最后一句话
     */
    private void parsedResult(RecognizerResult results, Boolean isLast) {

        String text = null;
        Log.i("TAG", "parsedJsonAndSetText：" + results.getResultString());
        try {
            text = JsonParser.parseIatResult(results.getResultString());
            if(TextUtils.isEmpty(text)){
                return;
            }
            Log.i("TAG", "WS->result:"+text);
            if(!TextUtils.isEmpty(text)){
                if (text.contains("成都成都")){
                    speakText("主人，小T为你服务");
                    wakeTimes = 5;
                }
                if (wakeTimes == 0){
                    speakText("主人，小T睡觉了");
                    wakeTimes--;
                    return;
                }else if(wakeTimes < 0){
                    return;
                }
                ToastUtil.showShort(BaseApplication.getContext(),text);
                if (text.contains("返回")){
                    BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_BACK,null);
                    wakeTimes = 5;
                    return;
                }else if (text.contains("确定")){
                    BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_CENTER,null);
                    wakeTimes = 5;
                    return;
                }else if (text.contains("上")){
                    BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
                    wakeTimes = 5;
                    return;
                }else if (text.contains("下")){
                    BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_DOWN,null);
                    wakeTimes = 5;
                    return;
                }else if (text.contains("左")){
                    BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_LEFT,null);
                    wakeTimes = 5;
                    return;
                }else if (text.contains("右")){
                    BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_RIGHT,null);
                    wakeTimes = 5;
                    return;
                }else if(text.contains("休眠")) {
                    speakText("主人，小T睡觉了");
                    wakeTimes = -1;
                }
                else{
                    wakeTimes --;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                speakText("主人，小T在后台为你服务");
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
                if (hearer != null) {
                    hearer.startListening(mRecoListener);
                }else {
                    hearer = SpeechRecognizer.createRecognizer(context, mInitListener);
                    hearer.startListening(mRecoListener);
                }
                mListenThread = new ListenThread();
                mListenThread.start();
            }else if(BroadcastManager.ACTION_VOICE_EMULATE_KEY_CLOSE.equals(action)){

                Log.d("wangfan", "onReceive: 接收到关闭后台语音识别广播");
                ToastUtil.showShort(BaseApplication.getContext(),"接收到关闭后台语音识别广播");
                if(mListenThread!=null){
                    mListenThread.interrupt();
                    mListenThread = null;
                }
                if (hearer != null) {
                    hearer.stopListening();
                }
            }
        }
    };
}
