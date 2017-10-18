package com.beoneaid.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.beoneaid.R;
import com.beoneaid.dao.MovieInfo;
import com.beoneaid.events.UpdateMovieEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by wangfan on 2017/10/16.
 */

public class ShowMovieActivity extends Activity {
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

    private List<MovieInfo> movieList;
    private int movListIndex = 0;
    private RequestQueue mQueue;

    private Response.ErrorListener RsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
        }
    };

    private void initReqQue(){
        mQueue = Volley.newRequestQueue(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);
        processExtraData();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initReqQue();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processExtraData();
    }

    private void processExtraData(){

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        movieList = (List<MovieInfo>) bundle.getParcelableArrayList("movieList").get(0);
        movListIndex = bundle.getInt("movListIndex");
        showMoveResult(movieList,movListIndex);
    }


    public void onEventMainThread(UpdateMovieEvent event) {
        movieList = event.getMovieList();
        movListIndex = event.getMovListIndex();
        showMoveResult(movieList,movListIndex);
    }

    private void showMoveResult(List<MovieInfo> movieList, int movListIndex) {
        if (movieList.size() - movListIndex <= 0) {
//            startTtsOutput("没有下一组了");
            this.movListIndex = movListIndex - 3;
            return;
        }
//        if (speak) {
//            startTtsOutput("现在显示第" + (movListIndex / 3 + 1) + "组结果");
//        }
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
    private void clearMovieShow() {
        ll1.setVisibility(View.GONE);
        ll2.setVisibility(View.GONE);
        ll3.setVisibility(View.GONE);
    }
}
