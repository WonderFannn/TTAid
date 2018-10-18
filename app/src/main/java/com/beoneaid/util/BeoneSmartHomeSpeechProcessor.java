package com.beoneaid.util;

import android.util.Log;

import com.beoneaid.util.TextCompute.Computeclass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wangfan on 2018/9/10.
 */

public class BeoneSmartHomeSpeechProcessor {
    private static final String TAG = "BeoneSmartHomeSpeechPro";
    private JSONArray mModeList;
    private JSONArray mFiltrateModeList;
    private Boolean isInit = false;

    private String answer;
    private int patternId = -1;
    private JSONObject result;

    public BeoneSmartHomeSpeechProcessor(JSONArray jsonArray){
        mModeList = jsonArray;
        mFiltrateModeList = new JSONArray();
        if (mModeList != null && mModeList.length() > 0){
            isInit = true;
        }
    }
    public JSONObject HandleText(String text){
//        if (text.startsWith("我想")||text.startsWith("我要")){
//            text = text.substring(2);
//        }
        if (!isInit){
            answer = "没有从平台获取模式列表哦";
            try {
                result = new JSONObject();
                result.put("answer",answer);
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
        if (text.contains("第") && text.indexOf("第") < text.length() && mFiltrateModeList.length() > 0){

            String num = text.substring(text.indexOf("第")+1,text.length());
            int number = 0;
            if (num.length() >= 2 && isNumeric(num.substring(0,2))){
                number = Integer.parseInt(num.substring(0,2));
            }else if (num.length() >= 1 && isNumeric(num.substring(0,1))) {
                number = Integer.parseInt(num.substring(0,1));
            }else if (num.length() >= 1){
                switch (num.substring(0,1)){
                    case "一":number = 1;break;
                    case "二":number = 2;break;
                    case "三":number = 3;break;
                    case "四":number = 4;break;
                    case "五":number = 5;break;
                    case "六":number = 6;break;
                    case "七":number = 7;break;
                    case "八":number = 8;break;
                    case "九":number = 9;break;
                    case "十":number = 10;break;
                    default:number = 0;break;
                }
            }else {
                number = 0;
            }
            Log.d(TAG, "HandleText: number = "+number);
            if (number <= 0 || number > mFiltrateModeList.length()){
                answer = "无效的数字";
            }else {
                try {
                    answer = "好的，为您执行模式-"+mFiltrateModeList.getJSONObject(number-1).getString("paternName");
                    patternId = mFiltrateModeList.getJSONObject(number-1).optInt("patternId");
                } catch (JSONException e) {
                    answer = "列表中没有这个模式";
                    e.printStackTrace();
                }
            }
            JSONObject resultJson = new JSONObject();
            try {
                resultJson.put("mode","1");
                resultJson.put("answer",answer);
                resultJson.put("patternId",patternId);
                mFiltrateModeList = new JSONArray();
            return resultJson;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        mFiltrateModeList = new JSONArray();
        for (int i = 0; i < mModeList.length() ; i++) {
            JSONObject jsonObject;
            try {
                jsonObject = mModeList.optJSONObject(i);
                String paternName = jsonObject.optString("paternName");
                int score = Computeclass.SimilarDegree(text,paternName);
                if (score == 100){
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("mode","1");
                    resultJson.put("answer","好的，为您执行模式-"+text);
                    resultJson.put("patternId",jsonObject.optInt("patternId"));
                    return resultJson;
                }
                if (score >= 66 || Computeclass.containResult(text,paternName) ){
                    mFiltrateModeList.put(jsonObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        if (mFiltrateModeList.length()>0){
            if (mFiltrateModeList.length() == 1){
                answer = "好的，为您执行模式-" + mFiltrateModeList.optJSONObject(0).optString("paternName");
                patternId = mFiltrateModeList.optJSONObject(0).optInt("patternId");
                JSONObject resultJson = new JSONObject();
                try {
                    resultJson.put("mode", "1");
                    resultJson.put("answer", answer);
                    resultJson.put("patternId",patternId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mFiltrateModeList = new JSONArray();
                return resultJson;
            }else {
                answer = "主人,你想执行哪个模式？";
                for (int i = 0; i < mFiltrateModeList.length(); i++) {
                    answer = answer+"\n"+ (i+1)+ "," + mFiltrateModeList.optJSONObject(i).optString("paternName");
                }
                JSONObject resultJson = new JSONObject();
                try {
                    resultJson.put("mode", "2");
                    resultJson.put("answer", answer);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return resultJson;
            }
        }else {
            answer = "主人，我不明白";
            JSONObject resultJson = new JSONObject();
            try {
                resultJson.put("mode","3");
                resultJson.put("answer",answer);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return resultJson;
        }

    }



    public static boolean isNumeric(String str) {
        try{
            Integer.parseInt(str);
            return true;
        }catch(NumberFormatException e) {
            return false;
        }
    }
}
