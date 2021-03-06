package com.beoneaid.smartecho;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.beoneaid.R;
import com.beoneaid.application.BaseApplication;
import com.beoneaid.broad.BroadcastManager;
import com.beoneaid.smartecho.audio.PcmRecorder;
import com.beoneaid.util.BeoneSmartHomeSpeechProcessor;
import com.beoneaid.util.Config;
import com.beoneaid.util.DesktopPetManager;
import com.beoneaid.util.GetMacUtil;
import com.beoneaid.util.JsonParser;
import com.beoneaid.util.LedController;
import com.beoneaid.util.LogUtil;
import com.beoneaid.util.ToastUtil;
import com.beoneaid.util.versionupdate.CheckVersionTask;
import com.beoneaid.util.versionupdate.IParse;
import com.beoneaid.util.versionupdate.VersionInfo;
import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIMessage;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wangfan on 2017/10/12.
 */

public class BeoneAid implements CaeWakeupListener{

    private static final String TAG = "BeoneAid";
    private Context mContext;

    private CaeWakeUpFileObserver mCaeWakeUpFileObserver;

    boolean mStartRecognize = false;
    boolean mIsOnTts = false;
    boolean mIsNeedStartIat = false;

    public BeoneAid(Context context) {
        mContext = context;
        init();
    }


    public void init() {
        LogUtil.d("SmartEcho - init");
        initTts();
        initIat();
        initReqQue();
        initMac();
        creatPet();
        initMediaPlayer();
        initAudioManager();
        mCaeWakeUpFileObserver = new CaeWakeUpFileObserver(this);
        mSelfCheckThread.start();
    }

    public void start() {
        LogUtil.d("SmartEcho - start");

        mRecorder = new PcmRecorder();
        mRecorder.startRecording(mPcmListener);
        if (mCaeWakeUpFileObserver != null) {
            mCaeWakeUpFileObserver.startWatching();
        }
        loginBeone();
    }

    public void stop() {
        mRecorder.stopRecording();
        mRecorder = null;
        LogUtil.d("SmartEcho - stop");
        if (mCaeWakeUpFileObserver != null) {
            mCaeWakeUpFileObserver.stopWatching();
        }
        stopIat();
    }

    @Override
    public void onWakeUp(int angle, int channel, int keywordID, boolean broadcastWakeup) {
        LogUtil.d("SmartEcho - onWakeUp");
        if (keywordID>=0){
            parseMode = keywordID;
            if (lockMode4 && keywordID == 0){
                parseMode = 4;
            }
            voicer = voicers[parseMode];
        }
        setChannel(channel);
        Log.d("TAG", "Echo  onWakeUp - angle:"+angle+"chane:"+channel);

        if(wakeupMode.equals(allModes[0]) || broadcastWakeup){
            startIat();
        }else {
            startTtsOutput(getEchoText(), true);
        }
        if (mMediaPlayer!=null && mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
        }
    }

    private String[] echoText = Config.ECHO_TEXT_ARRAY;
    private void setEchoText(String[] texts){
        for (String s:texts){
            Log.d(TAG, "唤醒回复词: "+s);
            if (TextUtils.isEmpty(s)){
                return;
            }
        }
        echoText = texts;
    }
    private int mEchoIndex = 0;
    private String getEchoText() {
        mEchoIndex++;
        if(mEchoIndex >= echoText.length) {
            mEchoIndex = 0;
        }
        return echoText[mEchoIndex];
    }

    /**
     *                               tts
     * ==================================================================================
     */
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "vinn";
    private String[] voicers = {"xiaoyan","xiaoyu","vinn","","xiaoyan"};
    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    public void initTts() {
        mTts = SpeechSynthesizer.createSynthesizer(mContext, mTtsInitListener);
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            LogUtil.d("InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                LogUtil.d("tts init error："+code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    public int startTtsOutput(String text) {
        return startTtsOutput(text, true);
    }

    public int startTtsOutput(String text, boolean needStartIatAfterTts) {
        if (mTts == null) {
            return -1;
        }
        mIsOnTts = true;
        mStartRecognize = false;
        mIsNeedStartIat = needStartIatAfterTts;
        // 设置参数
        setTtsParam();
        int code = mTts.startSpeaking(text, mTtsListener);
        if (text.contains("哔湾")){
            text = text.replace("哔湾","Beone");
        }
        if (PETSHOW){
            petTalk(text);
        }else {
            ToastUtil.showTopToast(mContext,text);
        }
        if (code != ErrorCode.SUCCESS) {
            LogUtil.d("tts error: " + code);
        }
        return code;
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            stopIat();
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
            } else if (error != null) {
                LogUtil.d(error.getPlainDescription(true));
            }
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    mIsOnTts = false;
                    if (mIsNeedStartIat) {
                        mIsNeedStartIat = false;
                        startIat();
                    }
                }
            }, 500);
            if (PETSHOW){
                petTalk("");
            }
            if (needPlayMp3){
                playMP3Url();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private void setTtsParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "75");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "100");
        }else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
            /**
             * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
             * 开发者如需自定义参数，请参考在线合成参数设置
             */
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "60");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "100");
        }
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
//        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, mContext.getExternalFilesDir(null)+"/msc/tts.wav");
    }


    /**
     * ==================================================================================
     *                               pcm record
     * ==================================================================================
     */
    PcmRecorder mRecorder;

    PcmRecorder.PcmListener mPcmListener = new PcmRecorder.PcmListener() {

        @Override
        public void onPcmRate(long bytePerMs) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPcmData(byte[] data, int dataLen) {
//            Log.d("onPcmData", "onPcmData: !write"+dataLen);
            PCM_IS_RUN = true;
//			write2File(data);
            if (mStartRecognize && !mIsOnTts) {
                // 写入16K采样率音频，开始听写
                Log.d(TAG, "onPcmData: "+dataLen);
                mIat.writeAudio(data, 0, dataLen);
            }
        }
    };

    /**
     * ==================================================================================
     *                               speech recognition
     * ==================================================================================
     */
    private SpeechRecognizer mIat;
    private void initIat() {
        mIat = SpeechRecognizer.createRecognizer(mContext, null);
        setIatParam();
    }
    // 听写监听器
    private RecognizerListener mIatListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int arg0, byte[] arg1) {
        }
        @Override
        public void onResult(RecognizerResult result, boolean isLast) {
            Log.d(TAG, "onResult: isLast"+isLast);
            String rltStr = printResult(result);
            if(isLast) {
//                setLedOff();
                stopIat();
                if (rltStr.contains("。")){
                    rltStr = rltStr.replaceAll("。","");
                }
                if(!TextUtils.isEmpty(rltStr)) {
                    parseOrder(rltStr);
                    ToastUtil.showShort(mContext, rltStr);

                }
            }
        }
        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }
        @Override
        public void onError(SpeechError arg0) {
            stopIat();
            Log.e(TAG, "onError: "+arg0.getErrorDescription() );
        }
        @Override
        public void onBeginOfSpeech() {
        }
        @Override
        public void onEndOfSpeech() {
            stopIat();
        }

    };
    private void startIat() {
        if (mTts.isSpeaking()){
            mTts.stopSpeaking();
            mIsOnTts = false;
        }
        mStartRecognize = true;
        if (wakeupMode.contains("light")){
            setLedOn();
        }
        // start listening user
        if (mIat != null && !mIat.isListening()) {
            mIat.startListening(mIatListener);
            getSystemCurrentValume();
        }
    }
    private void stopIat() {
        mStartRecognize = false;
        if(mIat != null && mIat.isListening()) {
            mIat.stopListening();
            if (wakeupMode.contains("light")){
                setLedOff();
            }
            resetCurrentValume();
        }
    }

    private void setIatParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "5000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1500");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
//        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/wftest/iat.wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, mContext.getExternalFilesDir(null)+"/msc/wftest/iat.wav");
        mIat.setParameter(SpeechConstant.NOTIFY_RECORD_DATA, "0");
        mIat.setParameter("domain", "fariat");
        mIat.setParameter("nunum","1");
    }
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private String printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        String resultStr = resultBuffer.toString();
        LogUtil.d(resultStr);
        return resultStr;
    }

    /**
     * ==================================================================================
     *                              命令解析
     * ==================================================================================
     */


//    private String[][] parserModeOrder = {{"中国中国"},{"中国"},{"美国"}};

    private int parseMode = 0;
    private boolean lockMode4 = false;
    public void setParseMode(int newMode){
        if (newMode <= Config.MODE_NAME_ARRAY.length){
            parseMode = newMode;
        }else {
            startTtsOutput("模式值超出范围");
            Log.d(TAG, "setParseMode: =="+newMode);
        }
        if (newMode == 4){
            startTtsOutput("已为你切换到"+Config.MODE_NAME_ARRAY[newMode]);
            lockMode4 = true;
        }else {
            startTtsOutput("本宝宝已做好了准备");
            lockMode4 = false;
        }
    }

    Boolean needSendPowerOff = false;
    Boolean needSendPowerRestart = false;

    public boolean needUpdateOTA = false;

    private String orderCache = "";
    private int orderCacheNum = 0;
    private void parseOrder(String order) {
        try {
            uploadText("玲珑宝:"+order);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (order.length() >= 3 && orderCache.length() >= 3){
            if (order.substring(order.length()-3,order.length()).equals(orderCache.substring(orderCache.length()-3,orderCache.length()))){
                orderCacheNum ++;
            }else {
                orderCacheNum = 0;
            }
            if (orderCacheNum == 4){
                startTtsOutput("语音缓存错误，即将重启",false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        BroadcastManager.sendBroadcastWithCommand(BroadcastManager.ACTION_SEND_SHELL_COMMAND,"reboot");
                    }
                }).start();
                return;
            }
        }
        orderCache = order;

        if (order.equals("强制关机")){
            startTtsOutput("即将关机",false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BroadcastManager.sendBroadcastWithCommand(BroadcastManager.ACTION_SEND_SHELL_COMMAND,"reboot -p");
                }
            }).start();
            return;
        }
        if (order.equals("强制重启")){
            startTtsOutput("好的，马上重启",false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BroadcastManager.sendBroadcastWithCommand(BroadcastManager.ACTION_SEND_SHELL_COMMAND,"reboot");
                }
            }).start();
            return;
        }
        if (order.equals("确定")){
            if (needSendPowerOff){
                startTtsOutput("即将关机",false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        BroadcastManager.sendBroadcastWithCommand(BroadcastManager.ACTION_SEND_SHELL_COMMAND,"reboot -p");
                    }
                }).start();
                return;
            }
            if (needSendPowerRestart){
                startTtsOutput("好的，马上重启",false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        BroadcastManager.sendBroadcastWithCommand(BroadcastManager.ACTION_SEND_SHELL_COMMAND,"reboot");
                    }
                }).start();
                return;
            }
            if (needInstallApk){
                //am broadcast -a com.android.update_success
                //am startservice -n com.jinxin.beoneaid/com.beoneaid.service.BeoneAidService
                BroadcastManager.sendBroadcastWithCommand2(BroadcastManager.ACTION_SEND_SHELL_COMMAND,"pm install -r "+ filePath,null,"am broadcast -a com.android.update_success");
                return;
            }
            if (needUpdateOTA){
                BroadcastManager.sendBroadcast(BroadcastManager.OTA_UPDATE_CONFIRM,null);
                return;
            }
        }
        needInstallApk = false;
        needSendPowerOff = false;
        needSendPowerRestart = false;
        needUpdateOTA = false;
        if (order.equals("关机")){
            startTtsOutput("主人，您确定要关机吗，关机请回复确定");
            needSendPowerOff = true;
            return;
        }
        if (order.equals("重启")){
            startTtsOutput("主人，您确定要重启吗，重启请回复确定");
            needSendPowerRestart = true;
            return;
        }


        if (order.equals("登录平台")){
            loginBeone();
            return;
        }
        if (order.equals("关闭桌面宠物")){
            destroyPet();
            return;
        }else if (order.equals("打开桌面宠物")){
            creatPet();
            return;
        }
        //屏蔽识别出来的唤醒词
        if (order.equals("宝贝宝贝")||order.equals("小宝小宝")||order.equals("小贝小贝")){
            return;
        }

        voicer = voicers[parseMode];
        if (parseMode == 0) {
            sendOrder2xfyd(order);
        }else if (parseMode == 1) {
            if (isLogin && mBeoneSmartHomeSpeechProcessor != null) {
                JSONObject result = mBeoneSmartHomeSpeechProcessor.HandleText(order);
                String answer = result.optString("answer");
                startTtsOutput(answer);
                int pId = result.optInt("patternId");
                try {
                    getAIUIResult(pId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                try {
//                    getAIUIResult(order);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
            }else {
                startTtsOutput("登录未成功",false);
                loginBeone();
            }
        }else if (parseMode == 2){
            if (order.equals("暂停")){
                if (mMediaPlayer!=null && mMediaPlayer.isPlaying()){
                    mMediaPlayer.pause();
                    return;
                }
            }else if (order.equals("播放")){
                if (mMediaPlayer!=null && haveMusic){
                    mMediaPlayer.start();
                    return;
                }
            }else if (order.equals("停止")){
                if(mMediaPlayer!=null && haveMusic){
                    mMediaPlayer.stop();
                    return;
                }
            }
            if (checkAIUIAgent()){
                startTextNlp(order);
            }
        }
    }


    /**
     * ==================================================================================
     *                               API mode == 0
     * ==================================================================================
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);
        // Set the component to be explicit
        explicitIntent.setComponent(component);
        return explicitIntent;
    }
    private void sendOrder2xfyd(String order){

        try {
//        com.iflytek.xiri2.START --es text "今天的天气" --es startmode "text"
            Intent intent = new Intent("com.iflytek.xiri2.START");
//        intent.putExtra("startmode","text");
//        intent.putExtra("text",order);
//        intent.setPackage("com.lzpd.scheduler");
            final Intent eintent = new Intent(createExplicitFromImplicitIntent(mContext, intent));
            eintent.putExtra("startmode", "text");
            eintent.putExtra("text", order);
            mContext.startService(eintent);

        }catch (Exception e){
            startTtsOutput("讯飞语点相关应用未安装",false);
        }
    }


    private void sendOrder2App(String order){
        String od = order;
        if (od.startsWith("我要")||od.startsWith("我想")){
            od = od.substring(2);
            if (od.startsWith("听音乐")||od.startsWith("听歌")){
                openActivity("com.jinxin.cloudmusic");
                onRecognizeResultListener.onRecognizeResult("close_movie_mode");
                return;
            }else if (od.startsWith("看电影")){
//                openActivity("com.jinxin.beonemoviesearcher","com.beonemoviesearcher.activity.MainActivity");
                openActivity("com.qiyi.video.jinxinzhihui");
                onRecognizeResultListener.onRecognizeResult("open_movie_mode");
                return;
            }
        }
        if (od.startsWith("打开")){
            od = od.substring(2);
            if (od.startsWith("音乐")||od.startsWith("云音乐")){
                openActivity("com.jinxin.cloudmusic");
                onRecognizeResultListener.onRecognizeResult("close_movie_mode");
                return;
            }else if (od.startsWith("影视搜索")||od.startsWith("电影搜索")){
                onRecognizeResultListener.onRecognizeResult("open_movie_mode");
//                openActivity("com.jinxin.beonemoviesearcher","com.beonemoviesearcher.activity.MainActivity");
                return;
            }else if (od.startsWith("帮助")){
                openActivity("com.jinxin.voicehelp");
                onRecognizeResultListener.onRecognizeResult("close_movie_mode");
                return;
            }else if (od.startsWith("呼叫中心")){
                openActivity("com.jinxin.jxpensioncall");
                onRecognizeResultListener.onRecognizeResult("close_movie_mode");
                return;
            }
        }

        onRecognizeResultListener.onRecognizeResult(order);
    }

    private OnRecognizeResultListener onRecognizeResultListener = null;

    public interface OnRecognizeResultListener{
        void onRecognizeResult(String result);
    }

    public void setOnRecognizeResultListener(OnRecognizeResultListener resultListener){
        onRecognizeResultListener = resultListener;
    }

    private void openActivity(String packageName){
        try{
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
            mContext.startActivity(intent);
        }catch (Exception e){
            startTtsOutput("没有安装相关应用，请安装");
        }
    }

    private void openActivity(String packageName,String activityName){
        Intent intent = new Intent();
        ComponentName cn = new ComponentName(packageName, activityName);
        try {
            intent.setComponent(cn);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openActivity: "+e.getMessage() );
            startTtsOutput("没有安装相关应用，请安装");
        }
    }


    /**
     * ==================================================================================
     *                               智慧家居 mode == 1
     * ==================================================================================
     */

    private RequestQueue mQueue;
    private void initReqQue() {
        mQueue = Volley.newRequestQueue(mContext);
    }
    private Response.ErrorListener RsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d("Volley", "onErrorResponse1: " + error.getMessage());
            Log.d("Volley", "onErrorResponse2: " + error.getLocalizedMessage());
            Log.d("Volley", "onErrorResponse3: " + error.getNetworkTimeMs());
        }
    };
    private Response.ErrorListener getOFRErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            startTtsOutput(getErrorText());
        }
    };
    private boolean isLogin = false;
    private String mSecretKey;
    private String mAccount;
    private String mMac;

    private int mErrorIndex = 0;
    private String getErrorText() {
        mErrorIndex++;
        if(mErrorIndex >= Config.ERROR_TEXT_ARRAY.length) {
            mErrorIndex = 0;
        }
        return Config.ERROR_TEXT_ARRAY[mErrorIndex];
    }

    private void initMac(){
        WifiManager wm = (WifiManager)mContext.getSystemService(mContext.WIFI_SERVICE);
        mMac = wm.getConnectionInfo().getMacAddress();
        if(!TextUtils.isEmpty(GetMacUtil.getMacAddress())) {
            mMac = GetMacUtil.getMacAddress();
            mMac = mMac.replace(":", "");
            mMac = "0000" + mMac;
        }
        if(TextUtils.isEmpty(mMac)){
            mMac = "0000F64F73A999618";
        }
    }
    private Response.Listener<String> RsBeoneListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d(TAG, "onResponse: BeoneListener"+response);
            if (isLogin) {
                try {
                    JSONObject data = new JSONObject(response);
                    JSONObject responseJson = data.optJSONObject("response");
                    String rspCode = responseJson.optString("rspCode");
                    if (!rspCode.equals("0000")) {
                        startTtsOutput("模式执行失败",false);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    JSONObject data = new JSONObject(response);
                    JSONObject serviceContent = data.getJSONObject("serviceContent");
                    mSecretKey = serviceContent.optString("secretKey");
                    mAccount = serviceContent.optString("account");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "onResponse: BeoneListener:"+e.getMessage() );
                }
                if (TextUtils.isEmpty(mSecretKey) || TextUtils.isEmpty(mAccount)) {
                    startTtsOutput("登录失败",false);
                    isLogin = false;
                } else {
//                    startTtsOutput("登录成功");
                    isLogin = true;
                    if (needGetCheckUpdate){
                        checkUpdateFromRemote();
                    }
                    if (needGetConfig){
                        getConfigurationFromRemote();
                    }
                    if (needGetModeList){
                        getModeListFromRemote();
                    }
                }
            }

        }
    };
    private void loginBeone() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        String time = formatter.format(curDate);

        JSONObject serviceContent = new JSONObject();
        try {
            serviceContent.put("mac", mMac);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject data = new JSONObject();
        try {
            data.put("actionCode", "0");
            data.put("activityCode", "T906");
            data.put("bipCode", "B000");
            data.put("bipVer", "1.0");
            data.put("origDomain", "VA000");
            data.put("processTime", time);
            data.put("homeDomain", "P000");
            data.put("testFlag", "1");
            data.put("serviceContent", serviceContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = mContext.getString(R.string.beone_aiui_url) + data.toString();
        Log.d(TAG, "loginBeone: URL =="+url);
        StringRequest stringRequest = new StringRequest(url, RsBeoneListener, RsErrorListener);
        mQueue.add(stringRequest);

    }

    private void getAIUIResult(int patternId) throws JSONException {
        Log.d("根据模式id控制模式", "getAIUIResult: "+patternId);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        String time = formatter.format(curDate);
        String opr = null;
//        try {
//            opr = URLEncoder.encode(patternId, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        JSONObject serviceContent = new JSONObject();
        serviceContent.put("secretKey", mSecretKey);
        serviceContent.put("account", mAccount);
        serviceContent.put("mac", mMac);
        serviceContent.put("patternId", patternId);
//        serviceContent.put("patternOperation", false);
        JSONObject data = new JSONObject();
        data.put("actionCode", "0");
        data.put("activityCode", "T902");
        data.put("bipCode", "B002");
        data.put("bipVer", "1.0");
        data.put("origDomain", "VA000");
        data.put("processTime", time);
        data.put("homeDomain", "P000");
        data.put("testFlag", "1");
        data.put("serviceContent", serviceContent);

        String url = mContext.getString(R.string.beone_aiui_url) + data.toString();
        StringRequest stringRequest = new StringRequest(url, RsBeoneListener, getOFRErrorListener);
        stringRequest.setRetryPolicy(
                new DefaultRetryPolicy(
                        20*1000,//默认超时时间，应设置一个稍微大点儿的
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,//默认最大尝试次数
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
        );
        mQueue.add(stringRequest);
    }

    private void uploadText(String text) throws JSONException {
        if (!isLogin){
            Log.e(TAG, "未登录,无法上传" );
            return;
        }
        Log.d("上传识别内容", "text: "+text);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        String time = formatter.format(curDate);
        String opr = null;
//        try {
//            opr = URLEncoder.encode(patternId, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        JSONObject serviceContent = new JSONObject();
        serviceContent.put("secretKey", mSecretKey);
        serviceContent.put("account", mAccount);
        serviceContent.put("mac", mMac);
        serviceContent.put("voiceText", text);
//        serviceContent.put("patternOperation", false);
        JSONObject data = new JSONObject();
        data.put("actionCode", "0");
        data.put("activityCode", "T902");
        data.put("bipCode", "B040");
        data.put("bipVer", "1.0");
        data.put("origDomain", "VA000");
        data.put("processTime", time);
        data.put("homeDomain", "P000");
        data.put("testFlag", "1");
        data.put("serviceContent", serviceContent);

        String url = mContext.getString(R.string.beone_aiui_url) + data.toString();
        StringRequest stringRequest = new StringRequest(url, RsUploadTextListener, getOFRErrorListener);
        stringRequest.setRetryPolicy(
                new DefaultRetryPolicy(
                        20*1000,//默认超时时间，应设置一个稍微大点儿的
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,//默认最大尝试次数
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
        );
        mQueue.add(stringRequest);
    }

    private Response.Listener<String> RsUploadTextListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d(TAG, "onResponse: RsUploadTextListener"+response);
            try {
                JSONObject data = new JSONObject(response);
                JSONObject responseJson = data.optJSONObject("response");
                String rspCode = responseJson.optString("rspCode");
                if (!rspCode.equals("0000")) {
                    Log.e(TAG, "onResponse: 上传失败" );
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * ==================================================================================
     *                               AIUI mode == 2
     * ==================================================================================
     */

    public String StringFilter(String str){
        String regEx="\\[[a-z]\\d\\]";
        Pattern p   =   Pattern.compile(regEx);
        Matcher m   =   p.matcher(str);
        return  m.replaceAll("").trim();
    }

    private AIUIAgent mAIUIAgent = null;
    private int mAIUIState = AIUIConstant.STATE_IDLE;

    private String getAIUIParams() {
        String params = "";
        AssetManager assetManager = mContext.getResources().getAssets();
        try {
            InputStream ins = assetManager.open("cfg/aiui_phone.cfg");
            byte[] buffer = new byte[ins.available()];
            ins.read(buffer);
            ins.close();
            params = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return params;
    }
    private boolean checkAIUIAgent(){
        if( null == mAIUIAgent ){
            mAIUIAgent = AIUIAgent.createAgent( mContext, getAIUIParams(), mAIUIListener );
            AIUIMessage startMsg = new AIUIMessage(AIUIConstant.CMD_START, 0, 0, null, null);
            mAIUIAgent.sendMessage( startMsg );
        }

        if( null == mAIUIAgent ){
            final String strErrorTip = "创建 AIUI Agent 失败！";
            ToastUtil.showShort(mContext,strErrorTip);
        }

        return null != mAIUIAgent;
    }

    private void startTextNlp(String text){
        String params = "data_type=text";
        if( TextUtils.isEmpty(text) ){
            return;
        }
        byte[] textData = text.getBytes();
        AIUIMessage msg = new AIUIMessage(AIUIConstant.CMD_WRITE, 0, 0, params, textData);
        mAIUIAgent.sendMessage(msg);
    }

    private AIUIListener mAIUIListener = new AIUIListener() {

        @Override
        public void onEvent(AIUIEvent event) {
            switch (event.eventType) {
                case AIUIConstant.EVENT_WAKEUP:
                    ToastUtil.showShort(mContext, "进入识别状态" );
                    break;

                case AIUIConstant.EVENT_RESULT: {
                    try {
                        JSONObject bizParamJson = new JSONObject(event.info);
                        JSONObject data = bizParamJson.getJSONArray("data").getJSONObject(0);
                        JSONObject params = data.getJSONObject("params");
                        JSONObject content = data.getJSONArray("content").getJSONObject(0);
                        if (content.has("cnt_id")) {
                            String cnt_id = content.getString("cnt_id");
                            JSONObject cntJson = new JSONObject(new String(event.data.getByteArray(cnt_id), "utf-8"));

//                            mNlpText.append( "\n" );
//                            mNlpText.append(cntJson.toString());

                            String sub = params.optString("sub");
                            if ("nlp".equals(sub)) {
                                // 解析得到语义结果
                                String resultStr = cntJson.optString("intent");
                                Logger.d(resultStr);
                                JSONObject jsonObject = new JSONObject(resultStr);
                                JSONObject answer = jsonObject.optJSONObject("answer");
                                JSONObject datajson = jsonObject.optJSONObject("data");
                                JSONArray result = null;
                                if (datajson != null) {
                                     result = datajson.optJSONArray("result");
                                }
                                Log.d(TAG, "AIUI返回1: "+answer);
                                if(answer != null) {
                                    String answerText = answer.optString("text");
                                    answerText = StringFilter(answerText);
                                    Log.d(TAG, "AIUI返回2："+answerText);
                                    if (TextUtils.isEmpty(answerText)) {
                                        startTtsOutput("对不起，我不明白");
                                    } else {
                                        String mp3url = "";
                                        String playUrl = "";
                                        String newsUrl = "";
                                        String title = "";
                                        if (result != null && result.length()>0) {
                                            mp3url = result.optJSONObject(0).optString("mp3Url");
                                            playUrl = result.optJSONObject(0).optString("playUrl");
                                            newsUrl = result.optJSONObject(0).optString("url");
                                            title = result.optJSONObject(0).optString("title");

                                            Log.d(TAG, "AIUI返回mp3: " + mp3url);
                                        }
                                        if (!TextUtils.isEmpty(mp3url)){
                                            Log.d(TAG, "AIUI返回3: "+answerText);
                                            needPlayMp3 = true;
                                            playOnlineUrl = mp3url;
                                            if (!TextUtils.isEmpty(title)){
                                                answerText = title;
                                            }
                                            startTtsOutput(answerText,false);
//                                            playMP3Url(mp3url);
                                        }else if (!TextUtils.isEmpty(playUrl)) {
                                            Log.d(TAG, "AIUI返回3: "+answerText);
                                            needPlayMp3 = true;
                                            playOnlineUrl = playUrl;
                                            if (!TextUtils.isEmpty(title)){
                                                answerText = title;
                                            }
                                            startTtsOutput(answerText,false);
                                        }else if (!TextUtils.isEmpty(newsUrl)) {
                                            Log.d(TAG, "AIUI返回3: "+answerText);
                                            needPlayMp3 = true;
                                            playOnlineUrl = newsUrl;
                                            if (!TextUtils.isEmpty(title)){
                                                answerText = title;
                                            }
                                            startTtsOutput(answerText,false);
                                        }else {
                                            Log.d(TAG, "AIUI返回3: "+answerText);
                                            startTtsOutput(answerText);
                                        }
                                    }
                                }else {
                                    startTtsOutput("对不起，我不明白");
                                }
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();

                    }

                } break;

                case AIUIConstant.EVENT_ERROR: {
                } break;

                case AIUIConstant.EVENT_VAD: {
                    if (AIUIConstant.VAD_BOS == event.arg1) {
                        ToastUtil.showShort(mContext,"找到vad_bos");
                    } else if (AIUIConstant.VAD_EOS == event.arg1) {
                        ToastUtil.showShort(mContext,"找到vad_eos");
                    } else {
                        ToastUtil.showShort(mContext,"" + event.arg2);
                    }
                } break;

                case AIUIConstant.EVENT_START_RECORD: {
                    ToastUtil.showShort(mContext,"开始录音");
                } break;

                case AIUIConstant.EVENT_STOP_RECORD: {
                    ToastUtil.showShort(mContext,"停止录音");
                } break;

                case AIUIConstant.EVENT_STATE: {	// 状态事件
                    mAIUIState = event.arg1;

                    if (AIUIConstant.STATE_IDLE == mAIUIState) {
                        // 闲置状态，AIUI未开启
                        ToastUtil.showShort(mContext,"STATE_IDLE");
                    } else if (AIUIConstant.STATE_READY == mAIUIState) {
                        // AIUI已就绪，等待唤醒
                        ToastUtil.showShort(mContext,"STATE_READY");
                    } else if (AIUIConstant.STATE_WORKING == mAIUIState) {
                        // AIUI工作中，可进行交互
                        ToastUtil.showShort(mContext,"STATE_WORKING");
                    }
                } break;

                case AIUIConstant.EVENT_CMD_RETURN:{
                    if( AIUIConstant.CMD_UPLOAD_LEXICON == event.arg1 ){
                        ToastUtil.showShort(mContext, "上传"+ (0==event.arg2?"成功":"失败") );
                    }
                }break;

                default:
                    break;
            }
        }

    };
    private boolean needPlayMp3 = false;
    private String playOnlineUrl;
    private MediaPlayer mMediaPlayer;
    private boolean haveMusic = false;
    private void initMediaPlayer(){
        mMediaPlayer = new MediaPlayer();
    }
    private void playMP3Url(){
        needPlayMp3 = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(playOnlineUrl);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.prepareAsync();
                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
//                            mBufferProgress = 0;
                            mp.start();
                            haveMusic = true;
                        }
                    });

                    mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            Toast.makeText(BaseApplication.getContext(), "播放错误！", Toast.LENGTH_SHORT).show();
                            Log.d(TAG+"MediaPlayer", "onError: "+what+" extra:"+extra);
                            return true;
                        }
                    });
                    mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            haveMusic = false;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "playMP3Url: "+e.getMessage());
                }
            }
        }).start();

    }

    /**
     * ==================================================================================
     *                               桌面宠物管理
     * ==================================================================================
     */


    /**
     * 用于在线程中创建或移除悬浮窗。
     */
    private Handler handler = new Handler();

    private boolean PETSHOW = true;
    private void creatPet(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                DesktopPetManager.createdesktopPetView(mContext);
            }
        });
        PETSHOW = true;
    }
    private void destroyPet(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                DesktopPetManager.removedesktopPetView(mContext);
            }
        });
        PETSHOW = false;
    }
    private void petTalk(final String text){
        handler.post(new Runnable() {
            @Override
            public void run() {
                DesktopPetManager.updatePetTalk(text);

            }
        });
    }

    /**
     * ==================================================================================
     *                        从平台获取模式列表
     * ==================================================================================
     */

    private BeoneSmartHomeSpeechProcessor mBeoneSmartHomeSpeechProcessor;

    private boolean needGetModeList = true;
    public void getModeListFromRemote(){
        needGetModeList = false;
        if (isLogin){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Date curDate = new Date(System.currentTimeMillis());
            String time = formatter.format(curDate);
            JSONObject serviceContent = new JSONObject();
            try {
                serviceContent.put("secretKey", mSecretKey);
                serviceContent.put("account", mAccount);
                serviceContent.put("mac", mMac);
                serviceContent.put("updateTime", "20160101090000");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject data = new JSONObject();
            try {
                data.put("activityCode", "T203");
                data.put("bipCode", "B002");
                data.put("origDomain", "VA000");
                data.put("homeDomain", "0000");
                data.put("serviceContent", serviceContent);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String url = mContext.getString(R.string.beone_aiui_url) + data.toString();
            Log.d(TAG, "getOrderFromRemote: url == " +url);
            StringRequest stringRequest = new StringRequest(url, RsGetModeListListener, RsErrorListener);
            mQueue.add(stringRequest);
        }else {
            needGetConfig = true;
            loginBeone();
        }

    }

    private Response.Listener<String> RsGetModeListListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
//            Log.d(TAG, "onResponse: 获取模式列表"+response);
            Logger.d(response);
            try {
                JSONObject data = new JSONObject(response);
                JSONArray serArrary = data.getJSONArray("serviceContent");
                mBeoneSmartHomeSpeechProcessor = new BeoneSmartHomeSpeechProcessor(serArrary);
            } catch (JSONException e) {
                Log.e("TAG", "onResponse: "+e.getMessage());
                e.printStackTrace();
                startTtsOutput("从平台模式列表失败，将使用APP默认配置",false);
            }
        }
    };

     /**
     * ==================================================================================
     *                        从平台更新和获取配置
     * ==================================================================================
     */

    private boolean needGetConfig = true;
    public void getConfigurationFromRemote(){
        needGetConfig = false;
        if (isLogin){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Date curDate = new Date(System.currentTimeMillis());
            String time = formatter.format(curDate);
            JSONObject serviceContent = new JSONObject();
            try {
                serviceContent.put("secretKey", mSecretKey);
                serviceContent.put("account", mAccount);
                serviceContent.put("mac", mMac);
                serviceContent.put("updateTime", "20160101090000");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject data = new JSONObject();
            try {
                data.put("activityCode", "T902");
                data.put("bipCode", "B004");
                data.put("origDomain", "VA000");
                data.put("homeDomain", "0000");
                data.put("serviceContent", serviceContent);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String url = mContext.getString(R.string.beone_aiui_url) + data.toString();
            Log.d(TAG, "getOrderFromRemote: url == " +url);
            StringRequest stringRequest = new StringRequest(url, RsPullListener, RsErrorListener);
            mQueue.add(stringRequest);
        }else {
            needGetConfig = true;
            loginBeone();
        }

    }

    private Response.Listener<String> RsPullListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d(TAG, "onResponse: 获取平台配置"+response);
            try {
                JSONObject data = new JSONObject(response);
                JSONArray serArrary = data.getJSONArray("serviceContent");
                String funParams = serArrary.getJSONObject(0).getString("funParams");
                JSONObject fpJO = new JSONObject(funParams);
                setAllModes(fpJO.optString("hxtsSetup"),fpJO.optString("qqtsSetup"),fpJO.optString("jgtsSetup"));
                JSONObject hxhfc = fpJO.optJSONObject("hxhfc");
                if (hxhfc!=null){
                    String[] texts = new String[3];
                    texts[0] = hxhfc.optString("hxhfc1");
                    texts[1] = hxhfc.optString("hxhfc2");
                    texts[2] = hxhfc.optString("hxhfc3");

                    setEchoText(texts);
                }
                String timeString = fpJO.optString("time");
                if (!TextUtils.isEmpty(timeString)){
                    try {
                        int time = Integer.valueOf(timeString);
                        if (time > 0){
                            mIat.setParameter(SpeechConstant.VAD_BOS, time+"000");
                            Log.d("TAG", "onResponse: 获取到平台设置识别时间"+time+"秒");
                        }else {
                            ToastUtil.showLong(BaseApplication.getContext(),"从平台获取识别时间失败，默认设置为5秒");
                        }
                    }catch (Exception e){
                        ToastUtil.showLong(BaseApplication.getContext(),"从平台获取识别时间失败，默认设置为5秒");
                    }
                }
            } catch (JSONException e) {
                Log.e("TAG", "onResponse: "+e.getMessage());
                e.printStackTrace();
                startTtsOutput("从平台获取配置失败，将使用APP默认配置",false);
            }
        }
    };

    private String[] allModes = {"tslight","tsvoice","tslightvoice"};
    private String wakeupMode = allModes[0];
    private String recognizeMode = allModes[0];
    private String completeMode = allModes[1];

    public void setAllModes(String wMode,String rMode,String cMode){
        if (wMode.contains("light") && wMode.contains("voice")){
            wakeupMode = allModes[2];
        }else if (wMode.contains("light")){
            wakeupMode = allModes[0];
        }else if (wMode.contains("voice")){
            wakeupMode = allModes[1];
        }else {
            wakeupMode = allModes[0];
        }

        if (rMode.contains("light") && rMode.contains("voice")){
            recognizeMode = allModes[2];
        }else if (rMode.contains("light")){
            recognizeMode = allModes[0];
        }else if (rMode.contains("voice")){
            recognizeMode = allModes[1];
        }else {
            recognizeMode = allModes[0];
        }

        if (cMode.contains("light") && cMode.contains("voice")){
            completeMode = allModes[2];
        }else if (cMode.contains("light")){
            completeMode = allModes[0];
        }else if (cMode.contains("voice")){
            completeMode = allModes[1];
        }else {
            completeMode = allModes[1];
        }

        Log.d(TAG, "setAllModes: "+wakeupMode+"-"+recognizeMode+"-"+completeMode);
    }

    //更新
    public boolean needInstallApk = false;
    public String filePath = "";
    private boolean needGetCheckUpdate = true;
    public void checkUpdateFromRemote(){
        needGetCheckUpdate = false;
        if (isLogin){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Date curDate = new Date(System.currentTimeMillis());
            String time = formatter.format(curDate);
            JSONObject serviceContent = new JSONObject();
            try {
                serviceContent.put("secretKey", mSecretKey);
                serviceContent.put("account", mAccount);
                serviceContent.put("mac", mMac);
                serviceContent.put("updateTime", time);
                serviceContent.put("appVersion", CheckVersionTask.getVersionName(mContext));
                serviceContent.put("appType", "10");
            } catch (Exception e) {
                e.printStackTrace();
            }
            JSONObject data = new JSONObject();
            try {
                data.put("activityCode", "T901");
                data.put("bipCode", "B007");
                data.put("origDomain", "VA000");
                data.put("homeDomain", "0000");
                data.put("serviceContent", serviceContent);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final String url = mContext.getString(R.string.beone_aiui_url) + data.toString();
            Log.d(TAG, "CheckUpdateFromRemote: url == " +url);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CheckVersionTask.setHttpUrlConnGet(mContext, new IParse() {
                        @Override
                        public VersionInfo parseData(String str) throws JSONException {
                            JSONObject data = new JSONObject(str);
                            JSONObject serviceContent = data.optJSONObject("serviceContent");
                            if (serviceContent == null){
                                return null;
                            }
                            Log.d(TAG, "parseData: serviceContent = "+serviceContent.toString());
                            VersionInfo info = new VersionInfo(
                                    serviceContent.optInt("id"),
                                    serviceContent.optInt("appType"),
                                    serviceContent.optString("appVersion"),
                                    serviceContent.optInt("publishTime"),
                                    serviceContent.optInt("publishUser"),
                                    serviceContent.optInt("downloadTimes"),
                                    serviceContent.optInt("status"),
                                    serviceContent.optString("comments"),
                                    serviceContent.optString("oldName"),
                                    serviceContent.optString("newName"),
                                    serviceContent.optString("appPath"),
                                    serviceContent.optInt("appSize"));
                            Log.d(TAG, "parseData: info = "+info.getAppVersion() );
                            return info;
                        }
                    },url);
                }
            }).start();
        }else {
            needGetCheckUpdate = true;
            loginBeone();
        }

    }
     /**
     * ==================================================================================
     *                               音量设置相关
     * ==================================================================================
     */

    private AudioManager mAudioManager;
    private int currentValume = 0;
    private void initAudioManager(){
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        currentValume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void getSystemCurrentValume() {
        int getVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if ( getVolume != 0){
            currentValume = getVolume;
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
        Log.d(TAG, "getSystemCurrentValume: currentValume"+currentValume);
    }


    private void resetCurrentValume(){
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentValume, 0);
        Log.d(TAG, "resetCurrentValume: "+currentValume);
    }

     /**
     * ==================================================================================
     *                               关机语音提示
     * ==================================================================================
     */

     private String[] powerPressReminder = {"有本事你就别放手","取消关机"};
     public void sayPowerLongPress(){
         startTtsOutput(powerPressReminder[0],false);
     }
     public void sayPowerUp(){
         startTtsOutput(powerPressReminder[1],false);
     }

     /**
     * ==================================================================================
     *                               LED控制
     * ==================================================================================
     */

     private int mChannel = 0;
     public void setChannel(int channel){
         mChannel = channel;
     }
     private void setLedOn(){
         Log.d("LedController", "channel = "+ mChannel);
         LedController.setLedOn(mChannel);
     }
     private void setLedOff(){
         LedController.setLedOff();
     }
     private void setLedFlash(){
         LedController.setLedFlash();
     }

     /**
     * ==================================================================================
     *                               定时自检
     * ==================================================================================
     */

    public static boolean PCM_IS_RUN = false;
    private SelfCheckThread mSelfCheckThread = new SelfCheckThread(this);
    class SelfCheckThread extends Thread {
        private BeoneAid mBeoneAid;

        public SelfCheckThread(BeoneAid beoneAid) {
            mBeoneAid = beoneAid;
        }

        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep(10000);
                    PCM_IS_RUN = false;
                    Log.d(TAG, "run: 自检线程运行中1  PCM_IS_RUN  ==" +PCM_IS_RUN);
                    Thread.sleep(5000);
                    Log.d(TAG, "run: 自检线程运行中2  PCM_IS_RUN  ==" +PCM_IS_RUN);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!PCM_IS_RUN){
                    mBeoneAid.stop();
                    mBeoneAid.start();
                    Log.e(TAG, "PCM_RUN :restart");
                }
            }
        }
    }
}
