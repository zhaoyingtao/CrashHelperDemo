package com.snow.crash;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Created by zyt on 2018/7/18.
 * 应用的一些工具类
 */

public class CrashUtils {
    private static CrashUtils crashUtils;

    public static CrashUtils init() {
        if (crashUtils == null) {
            synchronized (CrashUtils.class) {
                if (crashUtils == null) {
                    crashUtils = new CrashUtils();
                }
            }
        }
        return crashUtils;
    }

    /**
     * 判断某个Activity 界面是否在前台
     *
     * @param context
     * @param className 某个界面名称 MainActivity.class.getName()
     * @return
     */
    public boolean isForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className)) {
            return false;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * app的版本名
     *
     * @param context
     * @return
     */
    public String getAppVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }

    /**
     * app的版本号
     *
     * @param context
     * @return
     */
    public int getAppVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    private PackageInfo getPackageInfo(Context context) {
        PackageInfo pi = null;

        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);

            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pi;
    }

    /**
     * 关闭键盘
     */
    public void closeInputMethod(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive() && activity.getCurrentFocus() != null) {
            if (activity.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
//        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null && imm.isActive() && activity.getCurrentFocus() != null) {
//            IBinder binder = activity.getCurrentFocus().getWindowToken();
//            if (null != binder) {
//                imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
//            }
//        }
    }


    /**
     * 隐藏键盘
     */
    public void closeInputMethod(Activity activity, EditText v) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        if (null != v) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /**
     * 显示键盘
     * 调用不起作用时===可以延迟调用，让view.requestFocus(),获取焦点的方法需要做线程之外调用否则会报错
     */
    public void showSoftInputMethod(View view, Context mContext) {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    /**
     * 修改Manifest中的meta-data的值
     *
     * @param mContext
     * @param metaKey   meta-data的name
     * @param metaValue
     */
    public void modifyManifestMetaDataValue(Context mContext, String metaKey, String metaValue) {
        ApplicationInfo appInfo = null;
        try {
            appInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            //这里不要使用getString获取，值可能是其他类型如boolean、long、int等获取就会为null
//            String msg = String.valueOf(appInfo.metaData.get(metaKey));
//            Log.e("zyt", "before: " + msg);
            appInfo.metaData.putString(metaKey, metaValue);
//            String msg = appInfo.metaData.getString(metaKey);
//            Log.e("zyt", "after: " + msg);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * 判断当前设备是手机还是平板，代码来自 Google I/O App for Android
     *
     * @param context
     * @return 平板返回 True，手机返回 False
     */
    public boolean isPad(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * 获取渠道包的渠道号
     *
     * @param context
     * @return
     */
    public String getChannelName(Context context) {
        ApplicationInfo appInfo = null;
        String channelIdStr;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            //UMENG_CHANNEL是在AndroidManifest中配置的name名
            Object channelId = appInfo.metaData.get("UMENG_CHANNEL");
            channelIdStr = String.valueOf(channelId);
        } catch (PackageManager.NameNotFoundException e) {
            channelIdStr = "";
            e.printStackTrace();
        }
        return channelIdStr;
    }

    /**
     * 获取手机安装应用列表
     */
    public String getAppList(Context context) {
        PackageManager pm = context.getPackageManager();
        // Return a List of all packages that are installed on the device.
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        StringBuffer appListBuffer = new StringBuffer();
        int appNums = 0;
        for (PackageInfo packageInfo : packages) {
            // 判断系统/非系统应用
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) // 非系统应用
            {
                appNums++;
                appListBuffer.append("packageInfo=" + packageInfo.packageName + "\n");
                Log.e("snow", "MainActivity.getAppList, packageInfo=" + packageInfo.packageName);
            } else {
                // 系统应用
            }
        }
        String appNumsStr = "总应用数：" + packages.size() + "==非系统应用数==" + appNums + "\n";
        return appNumsStr + appListBuffer;
    }

    /**
     *   * 获取android当前可用运行内存大小
     *   * @param context
     *   *
     */
    public String getAvailMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
// mi.availMem; 当前系统的可用内存
        return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化
    }


    /**
     *   * 获取android总运行内存大小
     *   * @param context
     *   *
     */
    public String getTotalMemory(Context context) {
        String str1 = "/proc/meminfo";// 系统内存信息文件
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
            arrayOfString = str2.split("\\s+");
            for (String num : arrayOfString) {
                Log.i(str2, num + "\t");
            }
            // 获得系统总内存，单位是KB
            int i = Integer.valueOf(arrayOfString[1]).intValue();
            //int值乘以1024转换为long类型
            initial_memory = new Long((long) i * 1024);
            localBufferedReader.close();
        } catch (IOException e) {
        }
        return Formatter.formatFileSize(context, initial_memory);// Byte转换为KB或者MB，内存大小规格化
    }

    /**
     * 获取当前应用使用的内存大小
     *
     * @return
     */
    public double sampleMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        double mem = 0.0D;
        try {
            // 统计进程的内存信息 totalPss
            final Debug.MemoryInfo[] memInfo = activityManager.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
            if (memInfo.length > 0) {
                // TotalPss = dalvikPss + nativePss + otherPss, in KB
                final int totalPss = memInfo[0].getTotalPss();
                if (totalPss >= 0) {
                    // Mem in MB
                    mem = totalPss / 1024.0D;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mem;
    }

    /**
     * 当前手机可用存储内存大小
     *
     * @return xx GB
     */
    public String getAvailableInternalMemorySize(Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(context, availableBlocks * blockSize);
    }

    /**
     * 当前的手机总存储内存大小
     *
     * @return xx GB
     */
    public String getTotalInternalMemorySize(Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return Formatter.formatFileSize(context, totalBlocks * blockSize);
    }

    /**
     * 调用系统分享分享文件
     *
     * @param context
     * @param filePath
     */
    public void useSystemShareFile(Context context, String filePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            builder.detectFileUriExposure();
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.setType("application/txt");
        context.startActivity(shareIntent);
    }

    /**
     * 功能：已知字符串内容，输出到文件
     *
     * @param string
     * @param filePath LOCAL_PATH + "crash"
     * @param fileName exceptionLog.txt
     */
    public void writeContentToFile(String string, String filePath, String fileName) {
        File file = new File(filePath);
        try {
            // 首先判断文件夹是否存在
            if (!file.exists()) {
                if (!file.mkdirs()) {   // 文件夹不存在则创建文件
//                    Toast.makeText(MyApplication.getInstance(), "文件夹创建失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                File fileWrite = new File(filePath + File.separator + fileName);
                // 实例化对象：文件输出流
                FileOutputStream fileOutputStream = new FileOutputStream(fileWrite, true);
                // 写入文件
                fileOutputStream.write(string.getBytes());
                // 清空输出流缓存
                fileOutputStream.flush();
                // 关闭输出流
                fileOutputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
