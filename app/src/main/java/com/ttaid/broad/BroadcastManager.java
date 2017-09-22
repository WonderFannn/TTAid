package com.ttaid.broad;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.ttaid.application.BaseApplication;

/**
 * 应用内部广播管理器
 * @author JackeyZhang
 * @company 金鑫智慧
 */
public class BroadcastManager {

	public static final String ACTION_VOICE_EMULATE_KEY_OPEN = "action_voice_emulate_key_open";
	/**
	 * 关闭唤醒服务广播
	 */
	public static final String ACTION_VOICE_EMULATE_KEY_CLOSE = "action_voice_emulate_key_close";
	public static final String ACTION_VOICE_WAKE= "action_voice_wake";
	/**
	 * 关闭唤醒服务广播
	 */
	public static final String ACTION_VOICE_WAKE_CLOSE= "action_voice_wake_close";
	/**
	 * 重启唤醒服务广播
	 */
	public static final String ACTION_VOICE_WAKE_AGAIN = "action_voice_wake_again";
	/**
	* 按键模拟广播
	*/
	public static final String ACTION_SIMULATE_KEY_HOME = "action_simulate_key_home";
	public static final String ACTION_SIMULATE_KEY_BACK = "action_simulate_key_back";
	public static final String ACTION_SIMULATE_KEY_DPAD_CENTER = "action_simulate_key_pad_center";
	public static final String ACTION_SIMULATE_KEY_DPAD_UP = "action_simulate_key_dpad_up";
	public static final String ACTION_SIMULATE_KEY_DPAD_DOWN = "action_simulate_key_dpad_down";
	public static final String ACTION_SIMULATE_KEY_DPAD_LEFT = "action_simulate_key_dpad_left";
	public static final String ACTION_SIMULATE_KEY_DPAD_RIGHT = "action_simulate_key_dpad_right";

	/**
	 * 发送广播消息
	 * 
	 * @param action
	 *            广播消息名称
	 * @param bundle
	 *            传参
	 */
	public static void sendBroadcast(String action, Bundle bundle) {
		if (action == null)
			return;
		// 指定广播目标Action
		Intent _itent = new Intent(action);
		// 可通过Intent携带消息
		if (bundle != null) {
			_itent.putExtras(bundle);
		}
		// 发送广播消息
		BaseApplication.getContext().sendBroadcast(_itent);
	}
	/**
	 * 注册广播
	 */
	public static void registerBoradcastReceiver1(BroadcastReceiver broadcastReceiver, String... action) {
		if(broadcastReceiver == null || action == null)return;
		IntentFilter myIntentFilter = new IntentFilter();
			// 添加捕获的广播事件
			for(int i = 0;i < action.length;i++){
				myIntentFilter.addAction(action[i]);
			}

		// 注册广播
		BaseApplication.getContext().registerReceiver(broadcastReceiver, myIntentFilter);
	}
	/**
	 * 注册广播
	 */
	public static void registerBoradcastReceiver2(BroadcastReceiver broadcastReceiver, String[] action) {
		if(broadcastReceiver == null || action == null)return;
		IntentFilter myIntentFilter = new IntentFilter();
			// 添加捕获的广播事件
			for(int i = 0;i < action.length;i++){
				myIntentFilter.addAction(action[i]);
			}

		// 注册广播
		BaseApplication.getContext().registerReceiver(broadcastReceiver, myIntentFilter);
	}
	/**
	 * 注销广播接收器
	 * @param broadcastReceiver
	 */
	public static void unregisterBoradcastReceiver(BroadcastReceiver broadcastReceiver){
		if(broadcastReceiver == null)return;
		BaseApplication.getContext().unregisterReceiver(broadcastReceiver);
	}
}
