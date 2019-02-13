package com.zfy.networkplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.acl.Owner;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CheckVersionUpdate extends AsyncTask<String, Void, Integer> {
    private static final int TYPE_SHOWDIALOG = 0;
    private static final int TYPE_ENTERHOME = 1;
    private String newVersionName;
    private String newVersionDescription;
    private String newVersionDownloadUrl;
    private int currentVersionCode;
    private Handler mHandler;

    private DownloadService.downloadBind downloadBind;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBind = (DownloadService.downloadBind) service;
            downloadBind.setActivityProgressBar(false,
                    (ProgressBar) mActivity.get().findViewById(R.id.progressBar),
                    (TextView) mActivity.get().findViewById(R.id.progressText));
            downloadBind.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private WeakReference<Context> mContext;
    private WeakReference<Activity> mActivity;

    CheckVersionUpdate(Context context, Activity activity, int currentVersionCode) {
        mContext = new WeakReference<>(context);
        this.currentVersionCode = currentVersionCode;
        mActivity = new WeakReference<>(activity);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Intent intent = new Intent(mContext.get(), DownloadService.class);
        mContext.get().startService(intent);
        mContext.get().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        mHandler = new myHandler(mContext.get(), mActivity.get());
    }

    @Override
    protected Integer doInBackground(String... strings) {
        String newVersionUrl = strings[0];
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(newVersionUrl).build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String jsonData = response.body().string();
                JSONObject jsonObject = new JSONObject(jsonData);
                int newVersionCode = jsonObject.getInt("versionCode");
                newVersionName = jsonObject.getString("versionName");
                newVersionDescription = jsonObject.getString("description");
                newVersionDownloadUrl = jsonObject.getString("downladUrl");
                if (newVersionCode > currentVersionCode) {
                    return TYPE_SHOWDIALOG;
                } else
                    return TYPE_ENTERHOME;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SHOWDIALOG:
                new AlertDialog.Builder(mContext.get())
                        .setTitle("Checked New Version")
                        .setMessage("newVersionCode:" + newVersionName + "\n" + newVersionDescription)
                        .setPositiveButton("update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String filePath = mContext.get().getApplicationContext().getCacheDir().getPath();
                                downloadBind.startDownload(newVersionDownloadUrl, filePath, null);
                                downloadBind.setIsApkFile(true);
                                String fileName = newVersionDownloadUrl.substring(newVersionDownloadUrl.lastIndexOf("/") + 1);
                                ((myHandler) mHandler).setSaveFilePath(filePath + "/" + fileName);
                                mActivity.get().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                                mActivity.get().findViewById(R.id.progressText).setVisibility(View.VISIBLE);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onPostExecute(TYPE_ENTERHOME);
                            }
                        })
                        .show();
                break;
            case TYPE_ENTERHOME:
                Intent intent = new Intent(mContext.get(), MainActivity.class);
                mContext.get().startActivity(intent);
                mActivity.get().finish();
                break;
        }
    }

    static class myHandler extends Handler {
        private static final int INSTALL_APK = 6;
        private String saveFilePath;
        private WeakReference<Context> mContext;
        private WeakReference<Activity> mActivity;

        myHandler(Context context, Activity activity) {
            mContext = new WeakReference<>(context);
            mActivity = new WeakReference<>(activity);
        }

        void setSaveFilePath(String saveFilePath) {
            this.saveFilePath = saveFilePath;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INSTALL_APK:
                    installApk();
                    break;
            }
        }

        private void installApk() {
            File apkFile = new File(saveFilePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(mContext.get(), "com.zfy.networkplayer.provider", apkFile);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            mContext.get().startActivity(intent);
        }
    }
}
