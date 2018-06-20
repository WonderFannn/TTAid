package com.beoneaid.broad;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.beoneaid.application.BaseApplication;

/**
 * 应用内部广播管理器
 * @author JackeyZhang
 * @company 金鑫智慧
 */
public class BroadcastManager {

	/**
	 * 按键模拟唤醒广播
	 * */
	public static final String WAKEUP_BYUSER = "android.intent.action.wakeup_byuser";

	/**
	 * 按键模拟唤醒广播
	 * */
	public static final String GET_BATTERY_INFO = "android.intent.action.show_batteryinfo";

	/**
	 * 固件升级广播
	 * */
	public static final String OTA_SW_UPDATE = "android.intent.action.ota_sw_update";
	public static final String OTA_UPDATE_CONFIRM = "android.intent.action.rk.updateservice";

	public static final String SPEAK_TEXT = "android.intent.action.speak_text";
	/**
	 * 安装apk及更新成功广播
	 * */
	public static final String INSTALL_APK = "com.android.install_apk";
	public static final String UPDATE_SUCCESS = "com.android.update_success";
	/**
	 * shell命令广播
	 */
	public static final String ACTION_SEND_SHELL_COMMAND = "android.intent.action.runshellcommand";

	/**
	 * 关机语音提示广播
	 * */
	public static final String POWER_KEY_LONG_PRESS = "com.android.interceptPowerKeyLongPress";
	public static final String POWER_KEY_UP = "com.android.interceptPowerKeyUp";


	public static final String ACTION_VOICE_EMULATE_KEY_OPEN = "action_voice_emulate_key_open";
	public static final String ACTION_VOICE_EMULATE_KEY_CLOSE = "action_voice_emulate_key_close";
	/**
	 * 关闭唤醒服务广播
	 */
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


	public static void sendBroadcastWithCommand(String action, String command) {
		if (action == null)
			return;
		// 指定广播目标Action
		Intent _itent = new Intent(action);
		// 可通过Intent携带消息
		if (command != null) {
			_itent.putExtra("command",command);
		}
		// 发送广播消息
		BaseApplication.getContext().sendBroadcast(_itent);
	}
	public static void sendBroadcastWithCommand2(String action, String command,String argv1,String argv2) {
		if (action == null)
			return;
		// 指定广播目标Action
		Intent _itent = new Intent(action);
		// 可通过Intent携带消息
		if (command != null) {
			_itent.putExtra("command",command);
		}
		if (argv1 != null){
			_itent.putExtra("argv1",argv1);
		}
		if (argv2 != null){
			_itent.putExtra("argv2",argv2);
		}
		// 发送广播消息
		BaseApplication.getContext().sendBroadcast(_itent);
	}
	public static void sendBroadcastWithFilePath(String action, String filePath) {
		if (action == null)
			return;
		// 指定广播目标Action
		Intent _itent = new Intent(action);
		// 可通过Intent携带消息
		if (filePath != null) {
			_itent.putExtra("filePath",filePath);
		}
		// 发送广播消息
		BaseApplication.getContext().sendBroadcast(_itent);
	}

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
