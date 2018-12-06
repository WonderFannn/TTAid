package com.beoneaid.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.beoneaid.R;
import com.beoneaid.widget.DesktopPetView;

/**
 * Created by wangfan on 2017/11/28.
 */

public class DesktopPetManager {
    private static final String TAG = "PET";
    /**
     * 小悬浮窗View的实例
     */
    private static DesktopPetView desktopPetView;
    

    /**
     * 小悬浮窗View的参数
     */
    private static WindowManager.LayoutParams desktopPetViewParams;
    

    /**
     * 用于控制在屏幕上添加或移除悬浮窗
     */
    private static WindowManager mWindowManager;



    /**
     * 创建一个小悬浮窗。初始位置为屏幕的右部中间位置。
     *
     * @param context
     *            必须为应用程序的Context.
     */
    public static void createdesktopPetView(Context context) {
        Log.d(TAG, "createdesktopPetView: ");
        WindowManager windowManager = getWindowManager(context);
        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int screenHeight = windowManager.getDefaultDisplay().getHeight();
        if (desktopPetView == null) {
            desktopPetView = new DesktopPetView(context);
            Log.d(TAG, "createdesktopPetView: 2");
            if (desktopPetViewParams == null) {
                Log.d(TAG, "createdesktopPetView: 3å");

                desktopPetViewParams = new WindowManager.LayoutParams();
                desktopPetViewParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                desktopPetViewParams.format = PixelFormat.RGBA_8888;
                desktopPetViewParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                desktopPetViewParams.gravity = Gravity.RIGHT | Gravity.TOP;
                desktopPetViewParams.width = DesktopPetView.viewWidth;
                desktopPetViewParams.height = DesktopPetView.viewHeight;
                
                desktopPetViewParams.x = 100;
                
                desktopPetViewParams.y = 50;
            }
            desktopPetView.setParams(desktopPetViewParams);
            windowManager.addView(desktopPetView, desktopPetViewParams);
            Log.d(TAG, "createdesktopPetView: 4");
        }
    }

    /**
     * 将小悬浮窗从屏幕上移除。
     *
     * @param context
     *            必须为应用程序的Context.
     */
    public static void removedesktopPetView(Context context) {
        if (desktopPetView != null) {
            WindowManager windowManager = getWindowManager(context);
            windowManager.removeView(desktopPetView);
            desktopPetView = null;
        }
    }
    

    /**
     * 更新宠物的TextView上的数据
     *
     * @param str
     *
     */
    public static void updatePetTalk(String str) {
        if (desktopPetView != null) {
            Log.d(TAG, "updatePetTalk: ");
            TextView percentView = (TextView) desktopPetView.findViewById(R.id.tv_talk);
            ImageView petIcon = (ImageView) desktopPetView.findViewById(R.id.iv_pet);
            percentView.setText(str);
            percentView.setVisibility(View.VISIBLE);
            petIcon.setVisibility(View.VISIBLE);
            if (str.equals("")){
                percentView.setVisibility(View.INVISIBLE);
                petIcon.setVisibility(View.INVISIBLE);
            }
//            desktopPetView.setTalkText(str);
        }
    }

    /**
     * 是否有悬浮窗(包括小悬浮窗和大悬浮窗)显示在屏幕上。
     *
     * @return 有悬浮窗显示在桌面上返回true，没有的话返回false。
     */
    public static boolean isWindowShowing() {
        return desktopPetView != null ;
    }

    /**
     * 如果WindowManager还未创建，则创建一个新的WindowManager返回。否则返回当前已创建的WindowManager。
     *
     * @param context
     *            必须为应用程序的Context.
     * @return WindowManager的实例，用于控制在屏幕上添加或移除悬浮窗。
     */
    private static WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

}
