package com.ttaid.activity;

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
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import com.ttaid.broad.BroadcastManager;
import com.ttaid.dao.MovieInfo;
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

    //解析模式，0为本应用模式，1为AIUI模式
    private int parseMode = 0;

    private static String TAG = MainActivity.class.getSimpleName();
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

    private boolean isLogin = false;
    private String mSecretKey;
    private String mAccount;
    private String mMac = "00003ECB2DE233A8";

    private Response.Listener<String> RsBeoneListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d(TAG, "onResponse: " +response.toString());
            if (isLogin){

            }else {
                try {
                    JSONObject data = new JSONObject(response);
                    JSONObject serviceContent = data.getJSONObject("serviceContent");
                    mSecretKey = serviceContent.getString("secretKey");
                    mAccount = serviceContent.getString("account");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

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
            if (isLast) {
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
                int index = movListIndex;
                if (order.contains("1") || order.contains("一")) {
                    index = movListIndex;
                } else if (order.contains("2") || order.contains("二")) {
                    index = movListIndex + 1;
                } else if (order.contains("3") || order.contains("三")) {
                    index = movListIndex + 2;
                }
                if (index >= movieList.size()) {
                    speakText("您说错了吧");
                    return;
                }
                String idString = movieList.get(index).getId() + "";
                Intent intent = new Intent("com.tv.kuaisou.action.DetailActivity");
                intent.setPackage("com.tv.kuaisou");
                intent.putExtra("id", idString);
                startActivity(intent);
                BroadcastManager.sendBroadcast(BroadcastManager.ACTION_VOICE_WAKE, null);
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
            } else if (order.equals("关闭")) {
                speakText("再见");
                finish();
            } else if (order.equals("中国")) {
                speakText("正在登录");
                try {
                    loginBeone();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (parseMode == 1) {
            if (order.equals("中国中国")) {
                speakText("已为你切换到TT语音助手模式");
                parseMode = 0;
            } else {
                try {
                    getAIUIResult(order);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loginBeone() throws JSONException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        String time = formatter.format(curDate);

        JSONObject serviceContent = new JSONObject();
        serviceContent.put("mac", "888");
        JSONObject data = new JSONObject();
        data.put("actionCode","0");
        data.put("activityCode", "T906");
        data.put("bipCode", "B000");
        data.put("bipVer", "1.0");
        data.put("origDomain", "M000");
        data.put("processTime",time);
        data.put("homeDomain", "P000");
        data.put("testFlag", "1");
        data.put("serviceContent", serviceContent);

        String url = getString(R.string.beone_aiui_url) + data.toString();
        Log.d(TAG, "loginBeone: "+url);
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
        data.put("actionCode","0");
        data.put("activityCode", "T901");
        data.put("bipCode", "B040");
        data.put("bipVer", "1.0");
        data.put("origDomain", "M000");
        data.put("processTime",time);
        data.put("homeDomain", "P000");
        data.put("testFlag", "1");
        data.put("serviceContent", serviceContent);

        String url = getString(R.string.beone_aiui_url) + data.toString();
        Log.d(TAG, "getAIUIResult: "+url);
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
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    private void setTTSParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "70");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "80");

        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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

    @Override
    protected void onResume() {
        BroadcastManager.sendBroadcast(BroadcastManager.ACTION_VOICE_WAKE_CLOSE, null);
        mQueue = Volley.newRequestQueue(this);
        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);
        setParam();
        mListenlingThread = new ListeningThread();
        mListenlingThread.start();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mListenlingThread != null) {
            mListenlingThread.interrupt();
            mListenlingThread = null;
        }
        super.onPause();
    }

    public class ListeningThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.d(TAG, "ListeningThread run: ");
            try {
                while (true) {
                    if (mIat != null && !mIat.isListening()) {
                        Thread.sleep(300);
                        mIatResults.clear();
                        mIat.startListening(mRecognizerListener);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
