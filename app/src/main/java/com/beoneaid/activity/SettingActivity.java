package com.beoneaid.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.beoneaid.R;
import com.beoneaid.application.BaseApplication;
import com.beoneaid.service.BeoneAidService;
import com.beoneaid.util.Config;
import com.beoneaid.util.GetMacUtil;
import com.beoneaid.util.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by wangfan on 2017/10/13.
 */

public class SettingActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SettingActivity";
    @BindView(R.id.et_api_mode)
    EditText mEtApiMode;
    @BindView(R.id.et_smarthome_mode)
    EditText mEtSmarthomeMode;
    @BindView(R.id.et_aiui_mode)
    EditText mEtAiuiMode;
    @BindView(R.id.btn_upload)
    Button mBtnUpload;
    @BindView(R.id.btn_pull)
    Button mBtnPull;

    SharedPreferences mSharedPreferences;
    private boolean needPull = false;

    String apiModeOrder = "";
    String shModeOrder = "";
    String aiuiModeOrder = "";


    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        Intent intent = new Intent(this, BeoneAidService.class);
        intent.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
        startService(intent);

//        initReqQue();
//        initData();
//        initView();
//        initMac();
//      mMac = "0000128581425294";
    }

    private void initData() {
        mSharedPreferences = getSharedPreferences(Config.SHAREDPREFRENCES_NAME,MODE_PRIVATE);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                initView();
            }
        });
    }

    private void initView() {
        mEtApiMode.setText(getSPOrder(0));
        mEtSmarthomeMode.setText(getSPOrder(1));
        mEtAiuiMode.setText(getSPOrder(2));
        mBtnUpload.setVisibility(View.GONE);
        mBtnPull.setOnClickListener(this);
    }

    private void initMac(){
        WifiManager wm = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
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

    private String getSPOrder(int index){
        String order = "";
        switch (index){
            case 0:
                order = mSharedPreferences.getString(Config.ModeSetting.API_MODE,Config.ModeSetting.API_MODE_DEFULT_ORDER);
                break;
            case 1:
                order = mSharedPreferences.getString(Config.ModeSetting.SMARTHOMR_MODE,Config.ModeSetting.SMARTHOMR_MODE_DEFULT_ORDER);
                break;
            case 2:
                order = mSharedPreferences.getString(Config.ModeSetting.AIUI_MODE,Config.ModeSetting.AIUI_MODE_DEFULT_ORDER);
        }
        return order;
    }
    @Override
    public void onClick(View view) {
        if (view == mBtnUpload){
        }else if (view == mBtnPull){
            pullOrder();
        }

    }

    /**
     *  网络请求相关
     */
    private RequestQueue mQueue;
    private boolean isLogin = false;
    private String mMac;
    private String mSecretKey;
    private String mAccount;

    private void initReqQue(){
        mQueue = Volley.newRequestQueue(this);
    }

    private Response.ErrorListener RsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "onErrorResponse: "+error.getMessage());
        }
    };

    private void pullOrder() {

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

            String url = getString(R.string.beone_aiui_url) + data.toString();
            StringRequest stringRequest = new StringRequest(url, RsBeoneListener, RsErrorListener);
            mQueue.add(stringRequest);
        }else {
            loginBeone();
            needPull = true;
        }
    }

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

        String url = getString(R.string.beone_aiui_url) + data.toString();
        Log.d(TAG, "loginBeone: " +url);
        StringRequest stringRequest = new StringRequest(url, RsBeoneListener, RsErrorListener);
        mQueue.add(stringRequest);

    }

    private Response.Listener<String> RsBeoneListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.e(TAG, "onResponse: "+response );
            if (isLogin){
                try {
                    JSONObject data = new JSONObject(response);
                    JSONArray serArrary = data.optJSONArray("serviceContent");
                    String funParams = serArrary.getJSONObject(0).optString("funParams");
                    JSONObject fpJO = new JSONObject(funParams);
                    apiModeOrder += fpJO.optJSONObject("audio").optString("key1")+",";
                    apiModeOrder += fpJO.optJSONObject("audio").optString("key2")+",";
                    apiModeOrder += fpJO.optJSONObject("audio").optString("key3");
                    shModeOrder += fpJO.optJSONObject("dev").optString("key1")+",";
                    shModeOrder += fpJO.optJSONObject("dev").optString("key2")+",";
                    shModeOrder += fpJO.optJSONObject("dev").optString("key3");
                    aiuiModeOrder += fpJO.optJSONObject("aiui").optString("key1") + ",";
                    aiuiModeOrder += fpJO.optJSONObject("aiui").optString("key2") + ",";
                    aiuiModeOrder += fpJO.optJSONObject("aiui").optString("key3") ;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (TextUtils.isEmpty(apiModeOrder)||TextUtils.isEmpty(shModeOrder)||TextUtils.isEmpty(aiuiModeOrder)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mEtApiMode.setText(apiModeOrder);
                            mEtSmarthomeMode.setText(shModeOrder);
                            mEtAiuiMode.setText(aiuiModeOrder);
                            ToastUtil.showShort(BaseApplication.getContext(),"命令词获取成功");
                        }
                    });
                }
            }else {
                try {
                    JSONObject data = new JSONObject(response);
                    JSONObject serviceContent = data.optJSONObject("serviceContent");
                    mSecretKey = serviceContent.optString("secretKey");
                    mAccount = serviceContent.optString("account");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (TextUtils.isEmpty(mSecretKey) || TextUtils.isEmpty(mAccount)) {
                    ToastUtil.showShort(BaseApplication.getContext(), "登录失败");
                    Log.d(TAG, "onResponse: 登陆失败");
                    isLogin = false;
                } else {
                    ToastUtil.showShort(BaseApplication.getContext(), "登录成功");
                    Log.d(TAG, "onResponse: 登录成功");
                    isLogin = true;
                    if (needPull) {
                        pullOrder();
                    }
                }
            }
        }
    };
}
