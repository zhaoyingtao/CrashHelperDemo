package com.snow.crash;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zyt on 2017/11/20.
 * 自定义崩溃日志收集---需要在application中进行初始化
 * 初始化时必须的传日志存储路径
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private String appLocalPath = "";
    public static final String TAG = "CrashHandler";

    //系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    //程序的Context对象
    private Context mContext;
    //用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<String, String>();

    //用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    //是否崩溃跳转崩溃日志activity
    private static boolean isShowCrashAct;
    //是否存储崩溃日志到指定路径
    private static boolean isSaveCrashJournal;

    private static CrashHandler crashHandler;

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        if (crashHandler == null) {
            synchronized (CrashHandler.class) {
                if (crashHandler == null) {
                    crashHandler = new CrashHandler();
                }
            }
        }
        return crashHandler;
    }

    /**
     * 初始化
     *
     * @param context
     * @param localPath 崩溃日志存放路径
     */
    public CrashHandler init(Context context, String localPath) {
        this.appLocalPath = localPath;
        mContext = context;
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
        return crashHandler;
    }


    /**
     * 是否崩溃跳转崩溃日志activity
     *
     * @param isShowCrashActivity
     * @return
     */
    public CrashHandler setIsShowCrashActivity(boolean isShowCrashActivity) {
        isShowCrashAct = isShowCrashActivity;
        return crashHandler;
    }

    /**
     * 是否存储崩溃日志到指定路径
     *
     * @param isSaveCrashLog
     * @return
     */
    public CrashHandler setIsSaveCrashLog(boolean isSaveCrashLog) {
        isSaveCrashJournal = isSaveCrashLog;
        return crashHandler;
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override

    public void uncaughtException(Thread thread, Throwable ex) {
        if (isShowCrashAct){
            CrashModelBean model = parseCrash(ex);
            skipToCrashActivity(model);
        }
        if (isSaveCrashJournal){
            if (!handleException(ex) && mDefaultHandler != null) {
                //如果用户没有处理则让系统默认的异常处理器来处理
                mDefaultHandler.uncaughtException(thread, ex);
            } else {
                //退出程序
                android.os.Process.killProcess(android.os.Process.myPid());
//                //0 正常退出， 1 非正常退出
                System.exit(1);
            }
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //收集设备参数信息
        collectDeviceInfo(mContext);
        //保存日志文件
        saveCrashInfo2File(ex);
        return true;
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        infos.put("versionName", CrashUtils.init().getAppVersionName(ctx));
        infos.put("versionCode", CrashUtils.init().getAppVersionCode(ctx) + "");
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.e(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = appLocalPath + "crash/";
                File dir = new File(path);
                if (!dir.exists()) {
                    boolean createResult = dir.mkdirs();
                    if (!createResult) {
                        Log.e(TAG, "文件目录创建失败 不存在这个目录" + path);
                    }
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

    /**
     * 跳转到崩溃日志的activity
     * @param model
     */
    private void skipToCrashActivity(CrashModelBean model) {
        Intent intent = new Intent(mContext, CrashActivity.class);
        intent.putExtra(CrashActivity.CRASH_MODEL, model);
        intent.putExtra("appLocalPath", appLocalPath);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private CrashModelBean parseCrash(Throwable ex) {
        CrashModelBean model = new CrashModelBean();
        try {
            model.setEx(ex);
            model.setTime(new Date().getTime());
            if (ex.getCause() != null) {
                ex = ex.getCause();
            }
            model.setExceptionMsg(ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.flush();
            String exceptionType = ex.getClass().getName();

            if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
                StackTraceElement element = ex.getStackTrace()[0];
                model.setLineNumber(element.getLineNumber());
                model.setClassName(element.getClassName());
                model.setFileName(element.getFileName());
                model.setMethodName(element.getMethodName());
                model.setExceptionType(exceptionType);
            }

            model.setFullException(sw.toString());
        } catch (Exception e) {
            return model;
        }
        return model;
    }
}

