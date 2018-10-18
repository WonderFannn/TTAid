package com.beoneaid.smartecho;

/**
 * Created by yhc on 16-11-20.
 */

public interface CaeWakeupListener {
    void onWakeUp(int angle, int channel, int keywordID,boolean broadcastWakeup);
}
