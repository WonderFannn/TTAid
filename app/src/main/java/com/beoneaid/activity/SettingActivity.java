package com.beoneaid.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.beoneaid.util.ToastUtil;

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

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        Intent intent = new Intent(this, BeoneAidService.class);
        intent.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
        startService(intent);

        initReqQue();
        initData();
        initView();
        mMac = "0000128581425294";
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
        mBtnUpload.setOnClickListener(this);
        mBtnPull.setOnClickListener(this);
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
            ToastUtil.showShort(this,"平台说没时间开发接口，我也很无奈");
//            uploadOrder(mEtApiMode.getText().toString(),mEtSmarthomeMode.getText().toString(),mEtAiuiMode.getText().toString());
        }else if (view == mBtnPull){
            ToastUtil.showShort(this,"平台说没法测，我也很无奈");
//            pullOrder();
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

    private Response.Listener<String> RsUploadListener = new Response.Listener<String>() {
        @Override
        public void onResponse(final String response) {
            Log.e(TAG, "onResponse: " + response.toString());
        }
    };

    private Response.Listener<String> RsPullListener = new Response.Listener<String>() {
        @Override
        public void onResponse(final String response) {
            Log.e(TAG, "onResponse: " + response.toString());
        }
    };
    private Response.ErrorListener RsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "onErrorResponse: "+error.getMessage());
        }
    };

    private void uploadOrder(String s1, String s2, String s3){
        if (s1.equals(s2) || s1.equals(s3) || s2.equals(s3)){
            ToastUtil.showShort(this,"命令词不能相同");
            return;
        }
        if (s1.equals(getSPOrder(0)) && s2.equals(getSPOrder(1)) && s3.equals(getSPOrder(2))){
            ToastUtil.showShort(this,"命令词未更新，无需上传");
            return;
        }

        String url = getString(R.string.beone_aiui_url) ;
        StringRequest stringRequest = new StringRequest(url, RsUploadListener, RsErrorListener);
        mQueue.add(stringRequest);
    }


    private void pullOrder() {

        //{"activityCode":"T902",
        // "bipCode":"B004",
        // "origDomain":"P000",
        // "homeDomain":"0000",
        // "serviceContent":{
        //      "account":"C280010034",
        //      "updateTime":"20171019150100",
        //      "mac":"0000128581425294",
        //      "secretKey":"EFBFEAF6F4E044412640FD374A5E9296"}
        // }
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

            String url = getString(R.string.beone_aiui_url_test) + data.toString();
            StringRequest stringRequest = new StringRequest(url, RsPullListener, RsErrorListener);
            mQueue.add(stringRequest);
        }else {
            loginBeone();
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

        String url = getString(R.string.beone_aiui_url_test) + data.toString();
        Log.d(TAG, "loginBeone: " +url);
        StringRequest stringRequest = new StringRequest(url, RsBeoneListener, RsErrorListener);
        mQueue.add(stringRequest);

    }

    private Response.Listener<String> RsBeoneListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            try {
                JSONObject data = new JSONObject(response);
                JSONObject serviceContent = data.optJSONObject("serviceContent");
                mSecretKey = serviceContent.optString("secretKey");
                mAccount = serviceContent.optString("account");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (TextUtils.isEmpty(mSecretKey) || TextUtils.isEmpty(mAccount)) {
                ToastUtil.showShort(BaseApplication.getContext(),"登录失败");
                Log.d(TAG, "onResponse: 登陆失败");
                isLogin = false;
            } else {
                ToastUtil.showShort(BaseApplication.getContext(),"登录成功");
                Log.d(TAG, "onResponse: 登录成功");
                isLogin = true;
            }
        }
    };
}
