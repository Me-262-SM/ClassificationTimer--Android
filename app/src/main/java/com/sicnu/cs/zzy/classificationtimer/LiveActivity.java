package com.sicnu.cs.zzy.classificationtimer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.orhanobut.logger.Logger;
import com.sicnu.cs.zzy.classificationtimer.utils.AppManager;
import com.sicnu.cs.zzy.classificationtimer.utils.ScreenManager;

public class LiveActivity extends Activity {

//    public static final String TAG = LiveActivity.class.getSimpleName();

    public static void actionToLiveActivity(Context pContext) {
        Intent intent = new Intent(pContext, LiveActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pContext.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.t("LiveActivity").d("onCreate");
        setContentView(R.layout.activity_test);
        AppManager.getInstance().addActivity(this);

        Window window = getWindow();
        //放在左上角
        window.setGravity(Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams attributes = window.getAttributes();
        //宽高设计为1个像素
        attributes.width = 1;
        attributes.height = 1;
        //起始坐标
        attributes.x = 0;
        attributes.y = 0;
        window.setAttributes(attributes);

        ScreenManager.getInstance(this).setActivity(this);
    }

    @Override
    protected void onDestroy() {
        Logger.t("LiveActivity").d("onDestroy");
        AppManager.getInstance().finishActivity(this);
        super.onDestroy();
    }
}

