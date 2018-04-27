package com.beoneaid.util;


import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;

public class LedController {

    private static final String TAG = "LedController";
    // 麦克风 对应的 led灯
    static int mic_led_layout[][] = {{0, 2},{1, 1}, {2, 6} , {3, 5}, {4, 4}, {5, 3}};
    // 文件操作地址
    static final String LED_ADDR = "/sys/bus/i2c/drivers/sn3199_i2c/1-0067/led_switch";

    public static void setLedState(int channel, int brightness) {
       try {
           FileWriter fw = new FileWriter(LED_ADDR);
           String text = channel+""+brightness;
           fw.write(text);
           Log.d(TAG, "setLedState: "+text);
           fw.close();
       } catch (IOException e) {
           Log.d(TAG, "setLedState: "+e.getMessage());
           e.printStackTrace();
       }
    }


    public static void setLedOn(int channel) {
        setAllLedOff();
        setLedState(mic_led_layout[channel][1],1);
    }

    public static void setLedOff() {
        setAllLedOff();
        setLedState(9,1);
    }

    public static void setAllLedOff() {
        for (int i = 1; i <= 6; i++) {
            setLedState(i,0);
        }
    }


}
