package com.ttaid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.ttaid.R;
import com.ttaid.service.BeoneAidService;

/**
 * Created by wangfan on 2017/10/13.
 */

public class Test2Activity extends Activity {
    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Intent intent = new Intent(this, BeoneAidService.class);
        intent.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
        startService(intent);
    }
}
