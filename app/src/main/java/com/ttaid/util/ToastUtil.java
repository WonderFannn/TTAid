package com.ttaid.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ttaid.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by XTER on 2016/9/20.
 * Toast统一管理类
 */
public class ToastUtil {

	private static Toast toast;
	private static Handler mHandler = new Handler(Looper.getMainLooper());

	/**
	 * 短时间显示Toast
	 *
	 * @param context 上下文
	 * @param message 消息内容
	 */
	public static void showShort(final Context context, final CharSequence message) {
		mHandler.post(new Runnable() {
			@Override
			public synchronized void run() {
				if (null == toast) {
					toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
				} else {
					toast.setText(message);
				}
				toast.show();
			}
		});
	}

	/**
	 * 短时间显示Toast
	 *
	 * @param context 上下文
	 * @param message 消息内容ID
	 */
	public static void showShort(final Context context, final int message) {
		mHandler.post(new Runnable() {
			@Override
			public synchronized void run() {
				if (null == toast) {
					toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
				} else {
					toast.setText(message);
				}
				toast.show();
			}
		});

	}

	/**
	 * 长时间显示Toast
	 *
	 * @param context 上下文
	 * @param message 消息内容
	 */
	public static void showLong(final Context context, final CharSequence message) {
		mHandler.post(new Runnable() {
			@Override
			public synchronized void run() {
				if (null == toast) {
					toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
				} else {
					toast.setText(message);
				}
				toast.show();
			}
		});

	}

	/**
	 * 长时间显示Toast
	 *
	 * @param context 上下文
	 * @param message 消息内容ID
	 */
	public static void showLong(final Context context, final int message) {
		mHandler.post(new Runnable() {
			@Override
			public synchronized void run() {
				if (null == toast) {
					toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
				} else {
					toast.setText(message);
				}
				toast.show();
			}
		});
	}

	private static TextView text;
	/**
	 * 显示Toast
	 * @param context
	 * @param tvString
	 */
	public static void showTopToast(Context context, String tvString){
		showTopToast(context, tvString, 1000);
	}
	public static void showTopToast(Context context, String tvString, int cntime) {

		if (toast == null) {
			toast = new Toast(context);
			toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			View layout = LayoutInflater.from(context).inflate(R.layout.toast_xml, null);
//            ImageView mImageView = (ImageView) layout.findViewById(R.id.iv);
//            mImageView.setBackgroundResource(R.drawable.ic_launcher);
			text = (TextView) layout.findViewById(R.id.text);
			toast.setView(layout);
			showMyToast(toast, cntime);
		}
		text.setText(tvString);
		text.setTextColor(0xFFFFFFFF);
		text.setTextSize(16);
		if(toast!=null){
			toast.show();
		}

	}


	//自定义停留时间
	public static void showMyToast(final Toast toast, final int cnt) {
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				toast.show();
			}

		}, 0, 3000);
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				toast.cancel();
				timer.cancel();
			}

		}, cnt);

	}
}
