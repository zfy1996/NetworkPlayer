package com.zfy.networkplayer;

public interface DownloadListener {
    void pause();
    void cancel();
    void success();
    void failed();
    void invalid();
    void deleteFailed();
    void progressUpdate(int progress);
}
