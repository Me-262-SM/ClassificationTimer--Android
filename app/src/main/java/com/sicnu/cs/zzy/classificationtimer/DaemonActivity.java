package com.sicnu.cs.zzy.classificationtimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.sicnu.cs.zzy.classificationtimer.utils.AppManager;

public class DaemonActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daemon);
        AppManager.getInstance().addActivity(this);
        MonitorService.toMonitorService(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        AppManager.getInstance().finishActivity(this);
        super.onDestroy();
    }
}
