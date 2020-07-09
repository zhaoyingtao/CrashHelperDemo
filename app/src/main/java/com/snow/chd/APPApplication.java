package com.snow.chd;

import android.app.Application;
import android.os.Environment;

import com.snow.crash.CrashHandler;

/**
 * Created by zhaoyingtao
 * Date: 2020/7/2
 * Describe:
 */
public class APPApplication extends Application {
    String LOCAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/Android/data/" + getApplicationContext().getPackageName() + "/";
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance()
                .init(this, LOCAL_PATH)//这个必须调用
                .setIsShowCrashActivity(true)//崩溃后是否跳转崩溃日志activity 默认不跳
                .setIsSaveCrashLog(true);//崩溃后是否将日志到指定路径 默认不存
    }
}
