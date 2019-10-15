package com.sicnu.cs.zzy.classificationtimer;

import android.app.Application;

import com.orhanobut.logger.Logger;

public class MyApplication extends Application {
    private String TAG = "Application";

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化log工具类
        Logger.init(TAG).methodCount(1)     // 设置打印方法栈的个数，默认是2
                .hideThreadInfo()           // 隐藏线程信息，默认显示
                .methodOffset(0);           // 设置调用堆栈的偏移值，默认是0
    }

}
