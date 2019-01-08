package com.zfy.networkplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        try {
            initSystem();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initSystem() throws PackageManager.NameNotFoundException {
        TextView proggressText = findViewById(R.id.progressText);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        proggressText.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        TextView versionText = findViewById(R.id.versionText);
        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        int currentVersionCode = packageInfo.versionCode;
        versionText.setText(packageInfo.versionName);
        CheckVersionUpdate checkVersionUpdate = new CheckVersionUpdate(this,this,currentVersionCode);
        String checkNewVersionUrl = "https://github.com/zfy1996/NetworkPlayer/raw/master/update.json";
        checkVersionUpdate.execute(checkNewVersionUrl);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
}
