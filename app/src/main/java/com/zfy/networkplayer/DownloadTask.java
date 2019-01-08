package com.zfy.networkplayer;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    private static final int TYPE_PAUSE = 0;
    private static final int TYPE_CANCEL = 1;
    private static final int TYPE_SUCCESS = 2;
    private static final int TYPE_FAILED = 3;
    private static final int TYPE_INVALID = 4;
    private static final int TYPE_DELETEFAILED = 5;
    private static String[] URLSCHEME = {"http", "https", "file"};
    private DownloadListener listener;
    private static boolean isPause = false;
    private static boolean isCancel = false;
    private static int lastProgress = 0;
    private long currentFileSize = 0;
    private long desFileSize = 0;
    private String saveFilePath;

    DownloadTask(DownloadListener listener,String saveFilePath) {
        this.listener = listener;
        this.saveFilePath = saveFilePath;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        try {
            String downloadUrl = strings[0];
            if (!isValidUrl(downloadUrl))
                return TYPE_INVALID;
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
            File file = new File(saveFilePath + "/" + fileName);
            if (file.exists())
                currentFileSize = file.length();
            desFileSize = getDesFileSize(downloadUrl);
            if (desFileSize == 0)
                return TYPE_FAILED;
            if (desFileSize == currentFileSize)
                return TYPE_SUCCESS;
            return startDownloadFile(downloadUrl, file);
        } catch (IOException e) {
            e.printStackTrace();
            return TYPE_FAILED;
        }
    }

    private Integer startDownloadFile(String downloadUrl, File file) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            InputStream in = response.body().byteStream();
            RandomAccessFile saveFile = new RandomAccessFile(file, "rw");
            saveFile.seek(currentFileSize);
            byte[] temp = new byte[1024 * 4];
            int tempLen;
            while ((tempLen = in.read(temp)) != -1) {
                if (isPause)
                    return TYPE_PAUSE;
                else if (isCancel) {
                    if (file.delete())
                        return TYPE_CANCEL;
                    else
                        return TYPE_DELETEFAILED;
                } else {
                    currentFileSize += tempLen;
                    saveFile.write(temp, 0, tempLen);
                    publishProgress((int) (currentFileSize*100 / desFileSize));
                }
            }
            return TYPE_SUCCESS;
        }
        return TYPE_FAILED;
    }

    private long getDesFileSize(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        long fileLen = 0;
        if (response.isSuccessful() && response.body() != null) {
            fileLen = response.body().contentLength();
            response.close();
        }
        return fileLen;
    }

    @Override
    protected void onPostExecute(Integer returnStatus) {
        switch (returnStatus) {
            case TYPE_PAUSE:
                listener.pause();
                break;
            case TYPE_CANCEL:
                listener.cancel();
                break;
            case TYPE_SUCCESS:
                listener.success();
                break;
            case TYPE_FAILED:
                listener.failed();
                break;
            case TYPE_INVALID:
                listener.invalid();
                break;
            case TYPE_DELETEFAILED:
                listener.deleteFailed();
                break;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int currentProgress = values[0];
        if (currentProgress > lastProgress) {
            lastProgress = currentProgress;
            listener.progressUpdate(currentProgress);
        }
    }

    private Boolean isValidUrl(String downloadUrl) {
        Uri uri = Uri.parse(downloadUrl);
        if (uri.getScheme() != null) {
            for (String temp : URLSCHEME) {
                if (temp.equalsIgnoreCase(uri.getScheme()))
                    return true;
            }
        }
        return false;
    }

    void setCanceled() {
        isCancel = true;
    }

    void setPaused() {
        isPause = true;
    }

    long getCurrentFileSize() {
        return currentFileSize;
    }

    long getDesFileSize() {
        return desFileSize;
    }
}
