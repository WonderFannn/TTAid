package com.beoneaid.smartecho;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.beoneaid.R;
import com.beoneaid.application.BaseApplication;
import com.beoneaid.broad.BroadcastManager;
import com.beoneaid.smartecho.audio.PcmRecorder;
import com.beoneaid.util.Config;
import com.beoneaid.util.GetMacUtil;
import com.beoneaid.util.JsonParser;
import com.beoneaid.util.LogUtil;
import com.beoneaid.util.ToastUtil;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
//        initAudioManager();
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
        getOrderFromRemote();
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
    public void onWakeUp(int angle, int chanel) {
        LogUtil.d("SmartEcho - onWakeUp");

        Log.d("TAG", "Echo  onWakeUp - angle:"+angle+"chane:"+chanel);
        startTtsOutput(getEchoText(), true);
    }

    private int mEchoIndex = 0;
    private String getEchoText() {
        mEchoIndex++;
        if(mEchoIndex >= Config.ECHO_TEXT_ARRAY.length) {
            mEchoIndex = 0;
        }
        return Config.ECHO_TEXT_ARRAY[mEchoIndex];
    }

    /**
     * ==================================================================================
     *                               tts
     * ==================================================================================
     */
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "vinn";

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
        ToastUtil.showTopToast(mContext,text);
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
            mTts.setParameter(SpeechConstant.SPEED, "50");
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
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
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
            Log.d("onPcmData", "onPcmData: !write"+dataLen);
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
//            resetCurrentValume();
            stopIat();
        }

    };
    private void startIat() {
        mStartRecognize = true;
        // start listening user
        if (mIat != null && !mIat.isListening()) {
            mIat.startListening(mIatListener);
//            getSystemCurrentValume();
        }
    }
    private void stopIat() {
        mStartRecognize = false;
        if(mIat != null && mIat.isListening()) {
            mIat.stopListening();
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
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/wftest/iat.wav");
        mIat.setParameter(SpeechConstant.NOTIFY_RECORD_DATA, "0");
        mIat.setParameter("domain", "fariat");
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

    private boolean needPull = false;
    private String[][] parserModeOrder = {{"中国中国"},{"中国"},{"美国"}};

    private int parseMode = 0;
    public void setParseMode(int newMode){
        if (newMode <= Config.MODE_NAME_ARRAY.length){
            startTtsOutput("已为你切换到"+Config.MODE_NAME_ARRAY[newMode]);
            parseMode = newMode;
        }else {
            startTtsOutput("模式值超出范围");
            Log.d(TAG, "setParseMode: =="+newMode);
        }
    }

    public void getOrderFromRemote(){
        needPull = false;
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
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject data = new JSONObject();
            try {
                data.put("activityCode", "T902");
                data.put("bipCode", "B004");
                data.put("origDomain", "M000");
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
            needPull = true;
            loginBeone();
        }

    }
    private Response.Listener<String> RsPullListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d(TAG, "onResponse: 获取命令词"+response);
            try {
                JSONObject data = new JSONObject(response);
                JSONArray serArrary = data.getJSONArray("serviceContent");
                String funParams = serArrary.getJSONObject(0).getString("funParams");
                JSONObject fpJO = new JSONObject(funParams);
                String[][] s = new String[3][3];
                s[0][0] = fpJO.optJSONObject("audio").optString("key1");
                s[0][1] = fpJO.optJSONObject("audio").optString("key2");
                s[0][2] = fpJO.optJSONObject("audio").optString("key3");
                s[1][0] = fpJO.optJSONObject("dev").optString("key1");
                s[1][1] = fpJO.optJSONObject("dev").optString("key2");
                s[1][2] = fpJO.optJSONObject("dev").optString("key3");
                s[2][0] = fpJO.optJSONObject("aiui").optString("key1");
                s[2][1] = fpJO.optJSONObject("aiui").optString("key2");
                s[2][2] = fpJO.optJSONObject("aiui").optString("key3");
                if((s[0][0]!=null || s[0][1]!=null || s[0][2]!=null)
                        && (s[1][0]!=null || s[1][1]!=null || s[1][2]!=null)
                        && (s[2][0]!=null || s[2][1]!=null || s[2][2]!=null)){
                    setParserModeOrder(s);
                    startTtsOutput("从平台获取模式命令词成功",false);
                }else {
                    startTtsOutput("从平台获取模式命令词失败，请使用默认命令词",false);
                }
            } catch (JSONException e) {
                Log.e("TAG", "onResponse: "+e.getMessage());
                e.printStackTrace();
                startTtsOutput("从平台获取模式命令词失败，请使用默认命令词",false);
            }
        }
    };

    public void setParserModeOrder(String[][] s){
        parserModeOrder = s;
        for (int i = 0; i < parserModeOrder.length; i++){
            for (int j = 0; j < parserModeOrder[i].length; j++) {
                Log.d(TAG, "setParserModeOrder: "+i+j+parserModeOrder[i][j]);
            }
        }
    }

    private void parseOrder(String order) {
        for (int i = 0; i < parserModeOrder.length; i++){
            for (int j = 0; j < parserModeOrder[i].length; j++) {
                if (order.equals(parserModeOrder[i][j])){
                    if (i == 1){
                        if (isLogin) {
                            setParseMode(1);
                        } else {
                            startTtsOutput("正在登录",false);
                            loginBeone();
                        }
                    }else {
                        setParseMode(i);
                    }
                    return;
                }
            }
        }
        if (order.equals("更新命令词")){
            getOrderFromRemote();
            return;
        }
        if(order.equals("中国中国")){
            setParseMode(0);
            return;
        }else if (order.equals("中国")){
            if (isLogin) {
                setParseMode(1);
            } else {
                startTtsOutput("正在登录");
                loginBeone();
            }
            return;
        }else if (order.equals("美国")){
            setParseMode(2);
            return;
        }

        if (parseMode == 0) {
            sendOrder2App(order);
        }else if (parseMode == 1) {
            try {
                getAIUIResult(order);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if (parseMode == 2){
            if (checkAIUIAgent()){
                startTextNlp(order);
            }
        }else if (parseMode == 4){
            sendSimulateKeyBroadcast(order);
        }
    }



    /**
     * ==================================================================================
     *                               API mode == 0
     * ==================================================================================
     */
    private void sendOrder2App(String order){
        String od = order;
        if (od.startsWith("我要")||od.startsWith("我想")){
            od = od.substring(2);
            if (od.startsWith("听音乐")||od.startsWith("听歌")){
                openActivity("com.jinxin.cloudmusic");
                return;
            }else if (od.startsWith("看电影")){
                openActivity("com.jinxin.beonemoviesearcher");
                return;
            }
        }
        if (od.startsWith("打开")){
            od = od.substring(2);
            if (od.startsWith("音乐")||od.startsWith("云音乐")){
                openActivity("com.jinxin.cloudmusic");
                return;
            }else if (od.startsWith("影视搜索")||od.startsWith("电影搜索")){
                openActivity("com.jinxin.beonemoviesearcher");
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
        }
    };
    private boolean isLogin = false;
    private String mSecretKey;
    private String mAccount;
    private String mMac;

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
                    final String serviceContentString = data.getString("serviceContent");
                    final JSONObject serviceContentJson = new JSONObject(serviceContentString);
                    if (serviceContentJson != null) {
                        startTtsOutput(serviceContentJson.optString("answer"));
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
                    startTtsOutput("登录失败");
                    isLogin = false;
                } else {
                    isLogin = true;
                    if (needPull){
                        getOrderFromRemote();
                    }else {
                        setParseMode(1);
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
            data.put("origDomain", "M000");
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

    private void getAIUIResult(String order) throws JSONException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        String time = formatter.format(curDate);
        String opr = null;
        try {
            opr = URLEncoder.encode(order, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject serviceContent = new JSONObject();
        serviceContent.put("secretKey", mSecretKey);
        serviceContent.put("account", mAccount);
        serviceContent.put("mac", mMac);
        serviceContent.put("voiceText", opr);
        serviceContent.put("patternOperation", false);
        JSONObject data = new JSONObject();
        data.put("actionCode", "0");
        data.put("activityCode", "T901");
        data.put("bipCode", "B040");
        data.put("bipVer", "1.0");
        data.put("origDomain", "M000");
        data.put("processTime", time);
        data.put("homeDomain", "P000");
        data.put("testFlag", "1");
        data.put("serviceContent", serviceContent);

        String url = mContext.getString(R.string.beone_aiui_url) + data.toString();
        StringRequest stringRequest = new StringRequest(url, RsBeoneListener, RsErrorListener);
        mQueue.add(stringRequest);
    }

    /**
     * ==================================================================================
     *                               AIUI mode == 2
     * ==================================================================================
     */
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
                                JSONObject answer = new JSONObject(resultStr).optJSONObject("answer");
                                if(answer != null) {
                                    String answerText = answer.optString("text");
                                    if (TextUtils.isEmpty(answerText)) {
                                        startTtsOutput("对不起，我不明白");
                                    } else {
                                        startTtsOutput(answerText);
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

    /**
     * ==================================================================================
     *                               按键模式 mode == 4
     * ==================================================================================
     */
    private int mSRIndex = 0;
    private String getSRText() {
        mSRIndex++;
        if(mSRIndex >= Config.SMIULATEKEY_REPLY.length) {
            mSRIndex = 0;
        }
        return Config.SMIULATEKEY_REPLY[mSRIndex];
    }
    private void sendSimulateKeyBroadcast(String text) {
        if(!TextUtils.isEmpty(text)){
            ToastUtil.showShort(BaseApplication.getContext(),text);
            if (text.contains("返回")){
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_BACK,null);
                return;
            }else if (text.contains("确定")){
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_CENTER,null);
                return;
            }else if (text.contains("上")){
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_UP,null);
                startTtsOutput(getSRText());
                return;
            }else if (text.contains("下")){
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_DOWN,null);
                startTtsOutput(getSRText());
                return;
            }else if (text.contains("左")){
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_LEFT,null);
                startTtsOutput(getSRText());
                return;
            }else if (text.contains("右")){
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_SIMULATE_KEY_DPAD_RIGHT,null);
                startTtsOutput(getSRText());
                return;
            }else{
                startTtsOutput("我不明白");
            }
        }
    }


    /**
     * ==================================================================================
     *                               音量设置相关
     * ==================================================================================
     */

//    private AudioManager mAudioManager;
//    private int mMusicValumeMin;
//    private int currentValume;
//    private void initAudioManager(){
//        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//        mMusicValumeMin = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/10+1;
//        currentValume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//    }
//
//    private void getSystemCurrentValume() {
//        int getVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        if ( getVolume > mMusicValumeMin){
//            currentValume = getVolume;
//        }
//        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mMusicValumeMin, 0);
//        Log.d(TAG, "getSystemCurrentValume: currentValume"+currentValume);
//    }
//    private void resetCurrentValume(){
//        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentValume, 0);
//        Log.d(TAG, "resetCurrentValume: "+currentValume);
//    }

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
