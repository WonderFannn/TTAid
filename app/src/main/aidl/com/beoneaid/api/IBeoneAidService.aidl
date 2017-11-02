package com.beoneaid.api;
import com.beoneaid.api.IBeoneAidServiceCallback;

interface IBeoneAidService {
    void startSpeakingWithoutRecognize(String s);
    void startSpeaking(String s);
    void setMode(int i);
    void registerCallback(IBeoneAidServiceCallback cb);
    void unregisterCallback(IBeoneAidServiceCallback cb);
}
