package com.ttaid.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;

import com.ttaid.R;

import butterknife.ButterKnife;

/**
 * Created by wangfan on 2017/9/13.
 */

public class SettingActivity extends Activity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.setting_activity);
        ButterKnife.bind(this);
    }
}
