package com.zfy.networkplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.invoke.MethodHandle;

public class DownloadService extends Service {
    private DownloadTask downloadTask;
    private NotificationManager manager;
    private String downloadUrl;
    private boolean mLocalEnableNotification;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private downloadBind mBind = new downloadBind();
    Handler mHandler;
    private static final int DOWNLOAD_SUCCESS = 0;
    private static final int DOWNLOAD_PAUSE = 1;
    private static final int DOWNLOAD_CANCEL = 2;
    private static final int DOWNLOAD_FAILED = 3;
    private static final int URL_INVALID = 4;
    private static final int DELETE_FAILED = 5;

    private DownloadListener listener = new DownloadListener() {
        Message message = new Message();

        @Override
        public void pause() {
            downloadTask = null;
            //Toast.makeText(DownloadService.this, "Download Pause", Toast.LENGTH_SHORT).show();
            message.what = DOWNLOAD_PAUSE;
            mHandler.sendMessage(message);
        }

        @Override
        public void cancel() {
            downloadTask = null;
            if (mLocalEnableNotification)
                manager.cancel(1);
            //Toast.makeText(DownloadService.this, "Download Cancel", Toast.LENGTH_SHORT).show();
            message.what = DOWNLOAD_CANCEL;
            mHandler.sendMessage(message);
        }

        @Override
        public void success() {
            downloadTask = null;
            if (mLocalEnableNotification)
                manager.cancel(1);
            //Toast.makeText(DownloadService.this, "Download Success", Toast.LENGTH_SHORT).show();
            message.what = DOWNLOAD_SUCCESS;
            mHandler.sendMessage(message);
        }

        @Override
        public void failed() {
            downloadTask = null;
            //Toast.makeText(DownloadService.this, "Download Failed", Toast.LENGTH_SHORT).show();
            message.what = DOWNLOAD_FAILED;
            mHandler.sendMessage(message);
        }

        @Override
        public void invalid() {
            downloadTask = null;
            //Toast.makeText(DownloadService.this, "URL Invalid", Toast.LENGTH_SHORT).show();
            message.what = URL_INVALID;
            mHandler.sendMessage(message);
        }

        @Override
        public void deleteFailed() {
            downloadTask = null;
            //Toast.makeText(DownloadService.this, "Delete Failed", Toast.LENGTH_SHORT).show();
            message.what = DELETE_FAILED;
            mHandler.sendMessage(message);
        }

        @Override
        public void progressUpdate(int progress) {
            if (mLocalEnableNotification)
                manager.notify(1, getNotification("Downloading...", progress));
            else if(mProgressBar != null){
                mProgressBar.setMax(100);
                mProgressBar.setProgress(progress);
                if(mProgressText != null)
                    mProgressText.setText(String.format("%dKb/%dKb",downloadTask.getCurrentFileSize()/1024,downloadTask.getDesFileSize()/1024));
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBind;        //绑定服务时就会进入这里，将downloadBind返回到主线程中，进而连通主线程和下载的子线程
    }

    class downloadBind extends Binder {
        String saveFilePath;
        void setActivityProgressBar(Boolean localEnableNotification,
                                           ProgressBar progressBar, TextView progressText){
            mLocalEnableNotification = localEnableNotification;
            mProgressBar = progressBar;
            mProgressText = progressText;
        }

        void setHandler(Handler handler){
            mHandler = handler;
        }

        void  startDownload(String url,String saveFilePath) {
            if (downloadTask == null) {
                this.saveFilePath = saveFilePath;
                downloadUrl = url;
                downloadTask = new DownloadTask(listener,saveFilePath);
                downloadTask.execute(downloadUrl);    //转到doInBackground中执行下载动作
                if (mLocalEnableNotification) {
                    manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(1, getNotification("Downloading...", 0));
                }
            }
        }

        public void pauseDownload() {
            if (downloadTask != null) {
                downloadTask.setPaused();
            }
        }

        public void pauseDownloadCancel() {
            if (downloadTask != null) {
                downloadTask.setCanceled();
            }
        }

        public void cancelDownload() {
            if (downloadTask != null) {       //正在下载状态
                downloadTask.setCanceled();
                if (mLocalEnableNotification)
                    manager.cancel(1);
                Toast.makeText(DownloadService.this, "Download Cancel", Toast.LENGTH_SHORT).show();
            } else {
                if (downloadUrl != null && manager != null) {          //暂停下载状态
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
                    File file = new File(saveFilePath + "/" + fileName);
                    if (file.exists())
                        if (!file.delete())
                            Toast.makeText(DownloadService.this, "Download File Delete Failed", Toast.LENGTH_SHORT).show();
                    if (mLocalEnableNotification)
                        manager.cancel(1);
                    Toast.makeText(DownloadService.this, "Download Cancel", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Notification getNotification(String title, int progress) {   //前台通知
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Download", "Download Notification", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
//        Intent intent = new Intent(this,MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Download");
        builder.setContentTitle(title);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        //builder.setContentIntent(pendingIntent);
        builder.setProgress(100, progress, false);
        builder.setContentText(progress + "%");
        return builder.build();
    }
}
