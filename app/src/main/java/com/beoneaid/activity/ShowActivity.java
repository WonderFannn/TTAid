package com.beoneaid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.beoneaid.R;
import com.beoneaid.service.BeoneAidService;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by wangfan on 2017/10/13.
 */

public class ShowActivity extends Activity {

    private static final String TAG = "ShowActivity";
    @BindView(R.id.textView)
    TextView mTextView;

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        Intent intent = new Intent(this, BeoneAidService.class);
        intent.setAction(BeoneAidService.SMART_ECHO_ACTION_START);
        startService(intent);
        try {
            InputStream is = getAssets().open("updateInfo.txt");
            int size = is.available();

            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            // Convert the buffer into a string.
            String text = new String(buffer, "UTF-8");

            // Finally stick the string into the text view
            mTextView.setText(text);
        } catch (IOException e) {
            // Should never happen!
            throw new RuntimeException(e);
        }

//        initData();
//        initView();
    }

    private void initData() {

    }

    private void initView() {

    }




}
