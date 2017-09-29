package com.ttaid.activity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.ttaid.broad.NetworkStateReceiver;
import com.ttaid.dao.MovieInfo;
import com.ttaid.service.BackgroungSpeechRecongnizerService;
import com.ttaid.util.GetMacUtil;
import com.ttaid.util.JsonParser;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends Activity {

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

    private static String TAG = MainActivity.class.getSimpleName();
    NetworkStateReceiver netWorkStateReceiver;

    private SharedPreferences setting;
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    ListeningThread mListenlingThread;
    private Toast mToast;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    //讯飞AIUI
    private AIUIAgent mAIUIAgent = null;
    private int mAIUIState = AIUIConstant.STATE_IDLE;

    private RequestQueue mQueue;
    private List<MovieInfo> movieList;
    private int movListIndex = 0;

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
                            speakText("为你找到" + movieList.size() + "个结果");
                            shouMoveResult(movieList, movListIndex, false);
                        } else {
                            speakText("没有搜索到结果，请重新搜索 ");
                        }
                    }
                });
            }
        }
    };
    private int parseMode = 0;
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
                                    speakText(serviceContentJson.getString("answer"));
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
                    JSONObject serviceContent = data.getJSONObject("serviceContent");
                    mSecretKey = serviceContent.getString("secretKey");
                    mAccount = serviceContent.getString("account");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (TextUtils.isEmpty(mSecretKey) || TextUtils.isEmpty(mAccount)) {
                            speakText("登录失败");
                            isLogin = false;
                            parseMode = 0;
                        } else {
                            speakText("已经切换到智能家居模式");
                            isLogin = true;
                            parseMode = 1;
                        }
                    }
                });
            }

        }
    };
    private Response.ErrorListener RsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
        }
    };

    private void shouMoveResult(List<MovieInfo> movieList, int movListIndex) {
        shouMoveResult(movieList, movListIndex, true);
    }

    private void shouMoveResult(List<MovieInfo> movieList, int movListIndex, boolean speak) {
        if (movieList.size() - movListIndex <= 0) {
            speakText("没有下一组了");
            this.movListIndex = movListIndex - 3;
            return;
        }
        if (speak) {
            speakText("现在显示第" + (movListIndex / 3 + 1) + "组结果");
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

    @SuppressLint("ShowToast")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(com.ttaid.R.layout.main_activity);
        ButterKnife.bind(this);
        //启动后台语音识别服务
        Intent mBootIntent = new Intent(BaseApplication.getContext(), BackgroungSpeechRecongnizerService.class);
        startService(mBootIntent);

        setting = getSharedPreferences(getString(R.string.setting_prf),0);
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
        Log.d(TAG, "onCreate: mMac:"+mMac);
//        Intent intent = new Intent("com.ttaid.service.BackgroungSpeechRecongnizerService");
//        startService(intent);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        mTts = SpeechSynthesizer.createSynthesizer(this, mInitListener);
        setTTSParam();
        speakText("欢迎使用TT语音助手");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onBeginOfSpeech() {
        }

        @Override
        public void onError(SpeechError error) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            String resultText = printResult(results);
            showTip(resultText);
            if (isLast && !TextUtils.isEmpty(resultText)) {
                parseOrder(resultText);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            if (mIat.isListening()) {
                mIat.stopListening();
                if (mListenlingThread != null) {
                    mListenlingThread.interrupt();
                    mListenlingThread = null;
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
                if (mListenlingThread != null) {
                    mListenlingThread.start();
                } else {
                    mListenlingThread = new ListeningThread();
                    mListenlingThread.start();
                }
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private void clearMovieShow() {
        ll1.setVisibility(View.GONE);
        ll2.setVisibility(View.GONE);
        ll3.setVisibility(View.GONE);
    }

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
        return resultBuffer.toString();
    }

    private void parseOrder(String order) {
        if (order.equals("中国")){
            if (isLogin) {
                parseMode = 1;
                speakText("已为你切换到智能家居模式");
            } else {
                speakText("正在登录");
                try {
                    loginBeone();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return;
        }else if(order.equals("中国中国")){
            speakText("已为你切换到TT语音助手模式");
            parseMode = 0;
            return;
        }else if (order.equals("美国")){
            speakText("已为你切换到AIUI模式");
            parseMode = 2;
            return;
        }else if (order.equals("关闭")) {
            speakText("再见");
            finish();
            return;
        }
        if (parseMode == 0) {
            if (order.equals("清空")) {
                clearMovieShow();
                movieList.clear();
                movListIndex = 0;
                speakText("已经清空了显示结果，现在可以重新搜索");
            } else if (order.contains("播放")) {
                if (movieList == null || movieList.size() == 0) {
                    speakText("请先搜索电影");
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
                    speakText("您说错了吧");
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
                    speakText("没有安装影视快搜，请安装");
                }
            } else if (order.indexOf("搜索") == 0) {
                String movName = order.substring(order.indexOf("搜索") + 2, order.length());
                searchMovie(movName);
            } else if (order.contains("下一") || order.contains("向后")) {
                if (movieList == null || movieList.size() == 0) {
                    speakText("请先搜索电影");
                    return;
                }
                movListIndex += 3;
                shouMoveResult(movieList, movListIndex);
            } else if (order.contains("上一") || order.contains("向前")) {
                if (movieList == null || movieList.size() == 0) {
                    speakText("请先搜索电影");
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

    private void speakText(String text) {
        tvShowInfo.setText(text);
        mTts.startSpeaking(tvShowInfo.getText().toString(), mTtsListener);
    }

    private void searchMovie(String movName) {
        if (movieList != null) {
            movieList.clear();
        }
        movListIndex = 0;
        speakText("正在为你查找《" + movName + "》相关的内容");
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

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    /**
     * 参数设置
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE,setting.getString(SpeechConstant.LANGUAGE,"zh_cn"));
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, setting.getString(SpeechConstant.VAD_BOS,"4000"));
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, setting.getString(SpeechConstant.VAD_EOS,"1000"));
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, setting.getString(SpeechConstant.ASR_PTT,"0"));
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    private void setTTSParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME,setting.getString(SpeechConstant.VOICE_NAME, "xiaoyan"));
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED,setting.getString(SpeechConstant.SPEED, "70"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, setting.getString(SpeechConstant.PITCH,"50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME,setting.getString(SpeechConstant.VOLUME ,"80"));

        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

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
            showTip( strErrorTip );
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
                    showTip( "进入识别状态" );
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
                                        speakText("对不起，我不明白");
                                    } else {
                                        speakText(answerText);
                                    }
                                }else {
                                    speakText("对不起，我不明白");
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
                        showTip("找到vad_bos");
                    } else if (AIUIConstant.VAD_EOS == event.arg1) {
                        showTip("找到vad_eos");
                    } else {
                        showTip("" + event.arg2);
                    }
                } break;

                case AIUIConstant.EVENT_START_RECORD: {
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip("开始录音");
                } break;

                case AIUIConstant.EVENT_STOP_RECORD: {
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip("停止录音");
                } break;

                case AIUIConstant.EVENT_STATE: {	// 状态事件
                    mAIUIState = event.arg1;

                    if (AIUIConstant.STATE_IDLE == mAIUIState) {
                        // 闲置状态，AIUI未开启
                        showTip("STATE_IDLE");
                    } else if (AIUIConstant.STATE_READY == mAIUIState) {
                        // AIUI已就绪，等待唤醒
                        showTip("STATE_READY");
                    } else if (AIUIConstant.STATE_WORKING == mAIUIState) {
                        // AIUI工作中，可进行交互
                        showTip("STATE_WORKING");
                    }
                } break;

                case AIUIConstant.EVENT_CMD_RETURN:{
                    if( AIUIConstant.CMD_UPLOAD_LEXICON == event.arg1 ){
                        showTip( "上传"+ (0==event.arg2?"成功":"失败") );
                    }
                }break;

                default:
                    break;
            }
        }

    };
    @Override
    protected void onResume() {

        if (netWorkStateReceiver == null) {
            netWorkStateReceiver = new NetworkStateReceiver();
        }
        BroadcastManager.registerBoradcastReceiver1(netWorkStateReceiver, ConnectivityManager.CONNECTIVITY_ACTION);
        BroadcastManager.sendBroadcast(BroadcastManager.ACTION_VOICE_EMULATE_KEY_CLOSE, null);
        mQueue = Volley.newRequestQueue(this);
        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);
        setParam();
        mListenlingThread = new ListeningThread();
        mListenlingThread.start();
        setTTSParam();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mListenlingThread != null) {
            mListenlingThread.interrupt();
            mListenlingThread = null;
        }
        BroadcastManager.unregisterBoradcastReceiver(netWorkStateReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止后台识别服务
        Intent intent = new Intent(BaseApplication.getContext(), BackgroungSpeechRecongnizerService.class);
        stopService(intent);

        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }

        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    class ListeningThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.d(TAG, "ListeningThread run: ");
            try {
                while (true) {
                    if (mIat != null && !mIat.isListening()) {
                        Thread.sleep(50);
                        mIatResults.clear();
                        mIat.startListening(mRecognizerListener);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}



