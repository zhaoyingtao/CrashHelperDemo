# CrashHelperDemo
crash辅助工具

# 在build.gradle中引入  
` implementation 'com.snow.helper:crash-handler:0.0.7' `
# 初始化，最好放在application中初始化
```
String LOCAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/Android/data/" + getApplicationContext().getPackageName() + "/";
    @Override
    public void onCreate() {
        super.onCreate();
        //可以根据自己的项目的debug标识设置是否跳转崩溃信息页面和存储崩溃日志
        CrashHandler.getInstance()
                .init(this, LOCAL_PATH)//这个必须调用
                .setIsShowCrashActivity(true)//崩溃后是否跳转崩溃日志activity 默认不跳
                .setIsSaveCrashLog(true);//崩溃后是否将日志到指定路径 默认不存
    }
```

   
