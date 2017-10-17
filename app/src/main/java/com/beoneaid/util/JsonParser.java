package com.beoneaid.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

import com.beoneaid.dao.MovieInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Json结果解析类
 */
public class JsonParser {

	public static String parseIatResult(String json) {
		StringBuffer ret = new StringBuffer();
		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);

			JSONArray words = joResult.getJSONArray("ws");
			for (int i = 0; i < words.length(); i++) {
				// 转写结果词，默认使用第一个结果
				JSONArray items = words.getJSONObject(i).getJSONArray("cw");
				JSONObject obj = items.getJSONObject(0);
				ret.append(obj.getString("w"));
//				如果需要多候选结果，解析数组其他字段
//				for(int j = 0; j < items.length(); j++)
//				{
//					JSONObject obj = items.getJSONObject(j);
//					ret.append(obj.getString("w"));
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return ret.toString();
	}
	
	public static String parseGrammarResult(String json) {
		StringBuffer ret = new StringBuffer();
		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);

			JSONArray words = joResult.getJSONArray("ws");
			for (int i = 0; i < words.length(); i++) {
				JSONArray items = words.getJSONObject(i).getJSONArray("cw");
				for(int j = 0; j < items.length(); j++)
				{
					JSONObject obj = items.getJSONObject(j);
					if(obj.getString("w").contains("nomatch"))
					{
						ret.append("没有匹配结果.");
						return ret.toString();
					}
					ret.append("【结果】" + obj.getString("w"));
					ret.append("【置信度】" + obj.getInt("sc"));
					ret.append("\n");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret.append("没有匹配结果.");
		} 
		return ret.toString();
	}
	
	public static String parseLocalGrammarResult(String json) {
		StringBuffer ret = new StringBuffer();
		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);

			JSONArray words = joResult.getJSONArray("ws");
			for (int i = 0; i < words.length(); i++) {
				JSONArray items = words.getJSONObject(i).getJSONArray("cw");
				for(int j = 0; j < items.length(); j++)
				{
					JSONObject obj = items.getJSONObject(j);
					if(obj.getString("w").contains("nomatch"))
					{
						ret.append("没有匹配结果.");
						return ret.toString();
					}
					ret.append("【结果】" + obj.getString("w"));
					ret.append("\n");
				}
			}
			ret.append("【置信度】" + joResult.optInt("sc"));

		} catch (Exception e) {
			e.printStackTrace();
			ret.append("没有匹配结果.");
		} 
		return ret.toString();
	}

	public static String parseTransResult(String json,String key) {
		StringBuffer ret = new StringBuffer();
		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);
			String errorCode = joResult.optString("ret");
			if(!errorCode.equals("0")) {
				return joResult.optString("errmsg");
			}
			JSONObject transResult = joResult.optJSONObject("trans_result");
			ret.append(transResult.optString(key));
			/*JSONArray words = joResult.getJSONArray("results");
			for (int i = 0; i < words.length(); i++) {
				JSONObject obj = words.getJSONObject(i);
				ret.append(obj.getString(key));
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret.toString();
	}

	public static List<MovieInfo> parseMovieResult(String json){

        try {
            List<MovieInfo> movieInfoList = new ArrayList<MovieInfo>();
            JSONObject jsonObject = new JSONObject(json);
            int result = jsonObject.getInt("total");
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                MovieInfo movieInfo = new MovieInfo();
                JSONObject movieData = jsonArray.getJSONObject(i);
                try{
                    movieInfo.setId(movieData.getInt("id"));
                    movieInfo.setTitle(movieData.getString("title"));
                    movieInfo.setCid(movieData.getInt("cid"));
                    movieInfo.setTitle(movieData.getString("title"));
                    movieInfo.setPic(movieData.getString("pic"));
                    movieInfo.setDirector(movieData.getString("director"));
                    movieInfo.setActor(movieData.getString("actor"));
                    movieInfo.setPingy(movieData.getString("pingy"));
                    movieInfo.setYear(movieData.getInt("year"));
                }catch (Exception e){

                }
//                movieInfo.setScore((float) movieData.get("score"));
                movieInfoList.add(movieInfo);
            }
            return movieInfoList;

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("JsonParser", "json解析出现了问题");
        }
        return null;
    }
}
