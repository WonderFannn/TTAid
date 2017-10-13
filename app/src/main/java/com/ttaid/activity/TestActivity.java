package com.ttaid.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import com.ttaid.R;
import com.ttaid.application.BaseApplication;
import com.ttaid.broad.BroadcastManager;
import com.ttaid.dao.MovieInfo;
import com.ttaid.service.BackgroundEchoService;
import com.ttaid.smartecho.CaeWakeUpFileObserver;
import com.ttaid.smartecho.CaeWakeupListener;
import com.ttaid.smartecho.Config;
import com.ttaid.smartecho.audio.PcmRecorder;
import com.ttaid.util.GetMacUtil;
import com.ttaid.util.JsonParser;
import com.ttaid.util.LogUtil;
import com.ttaid.util.ToastUtil;

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
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by wangfan on 2017/10/11.
 */

public class TestActivity extends Activity implements CaeWakeupListener {

    private static final String TAG = TestActivity.class.getName();
    //控件绑定
    @BindView(R.id.tv_show_info)
    TextView tvShowInfo;
    @BindView(R.id.ll_1)
    LinearLayout ll1;
    @BindView(R.id.iv_1)
    ImageView iv1;
    @BindView(R.id.tv_1)
    TextView tv1;

    @BindView(R.id.ll_2)
    LinearLayout ll2;
    @BindView(R.id.iv_2)
    ImageView iv2;
    @BindView(R.id.tv_2)
    TextView tv2;

    @BindView(R.id.ll_3)
    LinearLayout ll3;
    @BindView(R.id.iv_3)
    ImageView iv3;
    @BindView(R.id.tv_3)
    TextView tv3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(com.ttaid.R.layout.main_activity);
        ButterKnife.bind(this);
//        //启动后台语音识别服务
//        Intent mBootIntent = new Intent(BaseApplication.getContext(), BackgroundEchoService.class);
//        startService(mBootIntent);
        mContext = getApplicationContext();
        WifiManager wm = (WifiManager)getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);
        mMac = wm.getConnectionInfo().getMacAddress();
        if(!TextUtils.isEmpty(GetMacUtil.getMacAddress())) {
            mMac = GetMacUtil.getMacAddress();
            mMac = mMac.replace(":", "");
            mMac = "0000" + mMac;
        }
        if(TextUtils.isEmpty(mMac)){
            mMac = "0000F64F73A999618";
        }

        mQueue = Volley.newRequestQueue(this);
    }

    @Override
    protected void onResume() {

        Log.d("TAG", "onResume: ");
        BroadcastManager.sendBroadcast(BroadcastManager.ACTION_VOICE_EMULATE_KEY_CLOSE, null);
        Intent mBootIntent = new Intent(BaseApplication.getContext(), BackgroundEchoService.class);
        stopService(mBootIntent);
        startService(mBootIntent);

        init();
        start();

        stop();

        init();
        start();

        startTtsOutput("欢迎使用哔湾语音助手");

        super.onResume();
}

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private Context mContext;

    private CaeWakeUpFileObserver mCaeWakeUpFileObserver;

    boolean mStartRecognize = false;
    boolean mIsOnTts = false;
    boolean mIsNeedStartIat = false;

    public void init() {
        initTts();
        initIat();
        mCaeWakeUpFileObserver = null;
        mCaeWakeUpFileObserver = new CaeWakeUpFileObserver(this);
    }

    public void start() {
        mRecorder = new PcmRecorder();
        mRecorder.startRecording(mPcmListener);
        if (mCaeWakeUpFileObserver != null) {
            mCaeWakeUpFileObserver.startWatching();
        }else {
            mCaeWakeUpFileObserver = new CaeWakeUpFileObserver(this);
            mCaeWakeUpFileObserver.startWatching();
        }
    }

    public void stop() {
        mRecorder.stopRecording();
        mRecorder = null;
        if (mCaeWakeUpFileObserver != null) {
            mCaeWakeUpFileObserver.stopWatching();
            mCaeWakeUpFileObserver = null;
        }
        stopIat();
    }

    @Override
    public void onWakeUp(int angle, int chanel) {

        Log.d("TAG", "Echo  onWakeUp - angle:"+angle+"chane:"+chanel);
        startTtsOutput(getEchoText(), true);
//        LedController.flashAllLed();
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
        return startTtsOutput(text, false);
    }

    public int startTtsOutput(final String text, boolean needStartIatAfterTts) {
        if (mTts == null) {
            return -1;
        }
        mIsOnTts = true;
        mStartRecognize = false;
        mIsNeedStartIat = needStartIatAfterTts;
        // 设置参数
        setTtsParam();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(text.contains("哔湾") ){
                    String a = text.replace("哔湾","Beone");
                    tvShowInfo.setText(a);
                }else {
                    tvShowInfo.setText(text);
                }
            }
        });

        int code = mTts.startSpeaking(text, mTtsListener);
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
            LogUtil.d("tts - start play");
        }

        @Override
        public void onSpeakPaused() {
            LogUtil.d("tts - pause play");
        }

        @Override
        public void onSpeakResumed() {
            LogUtil.d("tts - resume play");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                LogUtil.d("tts - play completed");
            } else if (error != null) {
                LogUtil.d(error.getPlainDescription(true));
            }
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    mIsOnTts = false;
                    if (mIsNeedStartIat) {
                        LogUtil.d("tts - onCompleted - need start iat after tts");
                        mIsNeedStartIat = false;
                        startIat();
                    }
                }
            }, 500);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
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
            mTts.setParameter(SpeechConstant.SPEED, "60");
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
//			write2File(data);
            if (mStartRecognize && !mIsOnTts) {
                // 写入16K采样率音频，开始听写
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
        LogUtil.d("SmartEcho - initIat");


        mIat = SpeechRecognizer.createRecognizer(mContext, null);
        setIatParam();
    }

    // 听写监听器
    private RecognizerListener mIatListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int arg0, byte[] arg1) {
            LogUtil.d("====== mIatListener - onVolumeChanged");
        }

        @Override
        public void onResult(RecognizerResult result, boolean isLast) {
            LogUtil.d("====== mIatListener - onResult");
            String resultText = printResult(result);

            if(isLast) {
                parseOrder(resultText);
                ToastUtil.showShort(mContext,resultText);
                stopIat();
            }
        }

        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
            LogUtil.d("====== mIatListener - onEvent");
        }

        @Override
        public void onError(SpeechError arg0) {
            LogUtil.d("====== mIatListener - onError");
            stopIat();
        }

        @Override
        public void onEndOfSpeech() {
            LogUtil.d("====== mIatListener - onEndOfSpeech");
//			mIsWakeup = false;
//			LogUtil.d("====== mIsWakeup = false");
        }

        @Override
        public void onBeginOfSpeech() {
            LogUtil.d("====== mIatListener - onBeginOfSpeech");
        }
    };

    private void startIat() {
        LogUtil.d("SmartEcho - startIat");
        mStartRecognize = true;
        // start listening user
        if(mIat != null && !mIat.isListening()) {
            mIat.startListening(mIatListener);
        }
//        showLedOnListener(true);
    }

    private void stopIat() {
        LogUtil.d("SmartEcho - stopIat");
        mStartRecognize = false;
        if(mIat != null && mIat.isListening()) {
            mIat.stopListening();
        }
//        mRecorder.stopRecording();
//        showLedOnListener(false);
    }

    private void setIatParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
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
        return resultStr;
    }



    /**
     * ==================================================================================
     *                               原界面逻辑处理
     * ==================================================================================
     */
    private RequestQueue mQueue;
    private List<MovieInfo> movieList;
    private int parseMode = 0;

    private int movListIndex = 0;

    private Response.ErrorListener RsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
        }
    };
    private Response.Listener<String> RsListener = new Response.Listener<String>() {
        @Override
        public void onResponse(final String response) {
            Log.d(TAG, "onResponse: " + response.toString());
            movieList = JsonParser.parseMovieResult(response);
            if (movieList != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (movieList.size() > 0) {
                            startTtsOutput("为你找到" + movieList.size() + "个结果");
                            shouMoveResult(movieList, movListIndex, false);
                        } else {
                            startTtsOutput("没有搜索到结果，请重新搜索 ");
                        }
                    }
                });
            }
        }
    };

    private void shouMoveResult(List<MovieInfo> movieList, int movListIndex) {
        shouMoveResult(movieList, movListIndex, true);
    }

    private void shouMoveResult(List<MovieInfo> movieList, int movListIndex, boolean speak) {
        if (movieList.size() - movListIndex <= 0) {
            startTtsOutput("没有下一组了");
            this.movListIndex = movListIndex - 3;
            return;
        }
        if (speak) {
            startTtsOutput("现在显示第" + (movListIndex / 3 + 1) + "组结果");
        }
        clearMovieShow();
        if ((movieList.size() - movListIndex) >= 3) {
            ll1.setVisibility(View.VISIBLE);
            ll2.setVisibility(View.VISIBLE);
            ll3.setVisibility(View.VISIBLE);
            tv1.setText(movieList.get(movListIndex).getTitle());
            tv2.setText(movieList.get(movListIndex + 1).getTitle());
            tv3.setText(movieList.get(movListIndex + 2).getTitle());
            ImageRequest imageRequest1 = new ImageRequest(
                    movieList.get(movListIndex).getPic(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            iv1.setImageBitmap(response);
                        }
                    }, 0, 0, Bitmap.Config.RGB_565, RsErrorListener);
            ImageRequest imageRequest2 = new ImageRequest(
                    movieList.get(movListIndex + 1).getPic(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            iv2.setImageBitmap(response);
                        }
                    }, 0, 0, Bitmap.Config.RGB_565, RsErrorListener);
            ImageRequest imageRequest3 = new ImageRequest(
                    movieList.get(movListIndex + 2).getPic(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            iv3.setImageBitmap(response);
                        }
                    }, 0, 0, Bitmap.Config.RGB_565, RsErrorListener);
            mQueue.add(imageRequest1);
            mQueue.add(imageRequest2);
            mQueue.add(imageRequest3);
        } else if ((movieList.size() - movListIndex) == 2) {
            ll1.setVisibility(View.VISIBLE);
            ll3.setVisibility(View.VISIBLE);
            tv1.setText(movieList.get(movListIndex).getTitle());
            tv3.setText(movieList.get(movListIndex + 1).getTitle());
            ImageRequest imageRequest1 = new ImageRequest(
                    movieList.get(movListIndex).getPic(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            iv1.setImageBitmap(response);
                        }
                    }, 0, 0, Bitmap.Config.RGB_565, RsErrorListener);
            ImageRequest imageRequest2 = new ImageRequest(
                    movieList.get(movListIndex + 1).getPic(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            iv3.setImageBitmap(response);
                        }
                    }, 0, 0, Bitmap.Config.RGB_565, RsErrorListener);
            mQueue.add(imageRequest1);
            mQueue.add(imageRequest2);
        } else if ((movieList.size() - movListIndex) == 1) {
            ll2.setVisibility(View.VISIBLE);
            tv2.setText(movieList.get(movListIndex).getTitle());
            ImageRequest imageRequest1 = new ImageRequest(
                    movieList.get(movListIndex).getPic(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            iv2.setImageBitmap(response);
                        }
                    }, 0, 0, Bitmap.Config.RGB_565, RsErrorListener);
            mQueue.add(imageRequest1);
        }
    }

    private void parseOrder(String order) {
        if (order.equals("中国")){
            if (isLogin) {
                parseMode = 1;
                startTtsOutput("已为你切换到哔湾智慧家居模式");
            } else {
                startTtsOutput("正在登录");
                try {
                    loginBeone();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return;
        }else if(order.equals("中国中国")){
            startTtsOutput("已为你切换到影视搜素模式");
            parseMode = 0;
            return;
        }else if (order.equals("美国")){
            startTtsOutput("已为你切换到AIUI模式");
            parseMode = 2;
            return;
        }else if (order.equals("退出应用")) {
            startTtsOutput("再见");
            finish();
            return;
        }
        if (parseMode == 0) {
            if (order.equals("清空")) {
                clearMovieShow();
                movieList.clear();
                movListIndex = 0;
                startTtsOutput("已经清空了显示结果，现在可以重新搜索");
            } else if (order.contains("播放")) {
                if (movieList == null || movieList.size() == 0) {
                    startTtsOutput("请先搜索电影");
                    return;
                }
                int index;
                if (order.contains("1") || order.contains("一")) {
                    index = movListIndex;
                } else if (order.contains("2") || order.contains("二")) {
                    index = movListIndex + 1;
                } else if (order.contains("3") || order.contains("三")) {
                    index = movListIndex + 2;
                } else {
                    index = movListIndex + movieList.size();//下标越界
                }
                //例外情况
                if (order.equals("播放")){
                    index = movListIndex;
                }
                if (index >= movieList.size()) {
                    startTtsOutput("您说错了吧");
                    return;
                }
                String idString = movieList.get(index).getId() + "";
                try{
                    Intent intent = new Intent("com.tv.kuaisou.action.DetailActivity");
                    intent.setPackage("com.tv.kuaisou");
                    intent.putExtra("id", idString);
                    startActivity(intent);
                    BroadcastManager.sendBroadcast(BroadcastManager.ACTION_VOICE_EMULATE_KEY_OPEN, null);
                }catch (Exception e){
                    startTtsOutput("没有安装影视快搜，请安装");
                }
            } else if (order.indexOf("搜索") == 0) {
                String movName = order.substring(order.indexOf("搜索") + 2, order.length());
                searchMovie(movName);
            } else if (order.contains("下一") || order.contains("向后")) {
                if (movieList == null || movieList.size() == 0) {
                    startTtsOutput("请先搜索电影");
                    return;
                }
                movListIndex += 3;
                shouMoveResult(movieList, movListIndex);
            } else if (order.contains("上一") || order.contains("向前")) {
                if (movieList == null || movieList.size() == 0) {
                    startTtsOutput("请先搜索电影");
                    return;
                }
                movListIndex -= 3;
                if (movListIndex < 0) {
                    movListIndex = 0;
                }
                shouMoveResult(movieList, movListIndex);
            }  else if(order.equals("设置")){
                Intent intent = new Intent(this,SettingActivity.class);
                startActivity(intent);
            }
        } else if (parseMode == 1) {
            try {
                getAIUIResult(order);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if (parseMode == 2){
            if (checkAIUIAgent()){
                startTextNlp(order);
            }
        }
    }
    private void searchMovie(String movName) {
        if (movieList != null) {
            movieList.clear();
        }
        movListIndex = 0;
        startTtsOutput("正在为你查找《" + movName + "》相关的内容");
        String codes = null;
        try {
            codes = URLEncoder.encode(movName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = getString(R.string.search_movie_url) + codes;
        Log.d(TAG, "searchMovie: " + url);
        StringRequest stringRequest = new StringRequest(url, RsListener, RsErrorListener);
        mQueue.add(stringRequest);
    }
    private void clearMovieShow() {
        ll1.setVisibility(View.GONE);
        ll2.setVisibility(View.GONE);
        ll3.setVisibility(View.GONE);
    }
    /**
     * ==================================================================================
     *                               哔湾智慧家居模式
     * ==================================================================================
     */
    private boolean isLogin = false;
    private String mSecretKey;
    private String mAccount;
    private String mMac;
    private Response.Listener<String> RsBeoneListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d(TAG, "onResponse: " + response.toString());
            if (isLogin) {
                try {
                    JSONObject data = new JSONObject(response);
                    final String serviceContentString = data.getString("serviceContent");
                    final JSONObject serviceContentJson = new JSONObject(serviceContentString);
                    if (serviceContentJson != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    startTtsOutput(serviceContentJson.getString("answer"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    JSONObject data = new JSONObject(response);
                    JSONObject serviceContent = data.optJSONObject("serviceContent");
                    mSecretKey = serviceContent.optString("secretKey");
                    mAccount = serviceContent.optString("account");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (TextUtils.isEmpty(mSecretKey) || TextUtils.isEmpty(mAccount)) {
                            startTtsOutput("登录失败");
                            isLogin = false;
                            parseMode = 0;
                        } else {
                            startTtsOutput("已经切换到哔湾智慧家居模式");
                            isLogin = true;
                            parseMode = 1;
                        }
                    }
                });
            }

        }
    };
    private void loginBeone() throws JSONException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        String time = formatter.format(curDate);

        JSONObject serviceContent = new JSONObject();
        serviceContent.put("mac", mMac);
        JSONObject data = new JSONObject();
        data.put("actionCode", "0");
        data.put("activityCode", "T906");
        data.put("bipCode", "B000");
        data.put("bipVer", "1.0");
        data.put("origDomain", "M000");
        data.put("processTime", time);
        data.put("homeDomain", "P000");
        data.put("testFlag", "1");
        data.put("serviceContent", serviceContent);

        String url = getString(R.string.beone_aiui_url) + data.toString();
        Log.d(TAG, "loginBeone: " + url);
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

        String url = getString(R.string.beone_aiui_url) + data.toString();
        Log.d(TAG, "getAIUIResult: " + url);
        StringRequest stringRequest = new StringRequest(url, RsBeoneListener, RsErrorListener);
        mQueue.add(stringRequest);
    }

    /**
     * ==================================================================================
     *                               讯飞AIUI
     * ==================================================================================
     */
    private AIUIAgent mAIUIAgent = null;
    private int mAIUIState = AIUIConstant.STATE_IDLE;


    private String getAIUIParams() {
        String params = "";

        AssetManager assetManager = getResources().getAssets();
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
            Log.i( TAG, "create aiui agent" );
            mAIUIAgent = AIUIAgent.createAgent( this, getAIUIParams(), mAIUIListener );
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
        Log.i( TAG, "start text nlp" );
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
                    Log.i( TAG,  "on event: "+ event.eventType );
                    ToastUtil.showShort(mContext, "进入识别状态" );
                    break;

                case AIUIConstant.EVENT_RESULT: {
                    Log.i( TAG,  "on event: "+ event.eventType );
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
                                    Log.i(TAG, resultStr);
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
//                        mNlpText.append( "\n" );
//                        mNlpText.append( e.getLocalizedMessage() );
                    }

//                    mNlpText.append( "\n" );
                } break;

                case AIUIConstant.EVENT_ERROR: {
                    Log.i( TAG,  "on event: "+ event.eventType );
//                    mNlpText.append( "\n" );
//                    mNlpText.append( "错误: "+event.arg1+"\n"+event.info );
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
                    Log.i( TAG,  "on event: "+ event.eventType );
                    ToastUtil.showShort(mContext,"开始录音");
                } break;

                case AIUIConstant.EVENT_STOP_RECORD: {
                    Log.i( TAG,  "on event: "+ event.eventType );
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


}
