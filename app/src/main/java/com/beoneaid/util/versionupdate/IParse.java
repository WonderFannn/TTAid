package com.beoneaid.util.versionupdate;

import org.json.JSONException;

/**
 * Created by ZJ on 2017/8/30 0030.
 * 解析平台返回下来的数据
 */
public interface IParse {
    VersionInfo parseData(String str) throws JSONException;
}
