package com.beoneaid.smartecho;

import android.os.FileObserver;
import android.util.Log;

import com.beoneaid.util.LogUtil;

import org.apache.http.util.EncodingUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by yhc on 16-11-20.
 */

public class CaeWakeUpFileObserver extends FileObserver {
    private static final String TAG = "CaeWakeUpFileObserver";
    CaeWakeupListener mCaeWakeupListener;
    static final String CAE_WAKEUP_FILE = "/data/cae_wakeup";
    int mAngle = -1;
    int mChanel = -1;
    int mKeywordID = 0;

    String[] keywords = {"xiao3bei4xiao3bei4","xiao2bao3xiao2bao3","bao3bei4bao3bei4"};

    public CaeWakeUpFileObserver(CaeWakeupListener caeWakeupListener) {
        super(CAE_WAKEUP_FILE);
        mCaeWakeupListener = caeWakeupListener;
    }

    public boolean getCAEWakeState() {
        boolean isWakeup = false;
        try {
            String cae_wakeup_file_str = readStringFromFile(CAE_WAKEUP_FILE);
            LogUtil.d("===== read " + CAE_WAKEUP_FILE +" : " + cae_wakeup_file_str);
            String[] temp_str = cae_wakeup_file_str.split(" ");
            if(temp_str != null && !cae_wakeup_file_str.equals("")) {
                if(temp_str[0] != null && !temp_str[1].equals("")) {
                    if("true".equals(temp_str[0])) {
                        isWakeup = true;
                    }
                }
                if(temp_str[1] != null && !temp_str[1].equals("")) {
                    mAngle = Integer.parseInt(temp_str[1]);
                }
                if(temp_str[2] != null && !temp_str[2].equals("")) {
                    mChanel = Integer.parseInt(temp_str[2]);
                }
                if (temp_str.length >= 5) {
                    String jsonString = cae_wakeup_file_str.substring(cae_wakeup_file_str.indexOf("{"),cae_wakeup_file_str.indexOf("}")+1);
                    try {
                        JSONObject data = new JSONObject(jsonString);
                        String keyword = data.optString("keyword").replace(" ", "");
                        Log.d(TAG, "getCAEWakeState: " + keyword);
                        if (keyword.equals(keywords[0])) {
                            mKeywordID = 0;
                        } else if (keyword.equals(keywords[1])) {
                            mKeywordID = 1;
                        } else if (keyword.equals(keywords[2])) {
                            mKeywordID = 2;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                LogUtil.d("===== mIsWakeup: " + isWakeup + " mAngle: " + mAngle
                        + " mChanel: " + mChanel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isWakeup;
    }

    private String readStringFromFile(String fileName) throws IOException {
        String res = "";
        try {
            FileInputStream fin = new FileInputStream(new File(CAE_WAKEUP_FILE));
            int length = fin.available();
            byte [] buffer = new byte[length];
            fin.read(buffer);
            res = EncodingUtils.getString(buffer, "UTF-8");
            fin.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public void setCaeWakeupState(boolean isWakeup, int angle, int chanel) {
        String write_str = isWakeup + " " + angle + " " + chanel;
//        LogUtil.d("====== update " + CAE_WAKEUP_FILE + " : " + write_str);
        try{
            FileOutputStream fout = new FileOutputStream(CAE_WAKEUP_FILE);
            byte [] bytes = write_str.getBytes();
            fout.write(bytes);
            fout.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setCaeWakeupState(boolean isWakeup) {
        setCaeWakeupState(isWakeup, mAngle, mChanel);
    }

    @Override
    public void onEvent(int i, String s) {
        if (i == FileObserver.MODIFY) {
            LogUtil.d("====== " + CAE_WAKEUP_FILE + " has been modify, read it go!");
            boolean isWakeup = getCAEWakeState();
            if(isWakeup) {
                mCaeWakeupListener.onWakeUp(mAngle, mChanel, mKeywordID,false);
                setCaeWakeupState(false);
            }
        }
    }
}
