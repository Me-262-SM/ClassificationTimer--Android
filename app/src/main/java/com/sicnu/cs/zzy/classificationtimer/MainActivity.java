package com.sicnu.cs.zzy.classificationtimer;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.orhanobut.logger.Logger;
import com.sicnu.cs.zzy.classificationtimer.faceutils.Constants;
import com.sicnu.cs.zzy.classificationtimer.javaBean.ProcessInfo;
import com.sicnu.cs.zzy.classificationtimer.utils.AppManager;
import com.sicnu.cs.zzy.classificationtimer.utils.ProcessUtils;
import com.sicnu.cs.zzy.classificationtimer.utils.UsageStatsUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends Activity {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean isFirst;
    private Toast toast = null;
    private FaceEngine faceEngine = new FaceEngine();
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getInstance().addActivity(this);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,permissions,0);
            }
        }

        //检查悬浮窗权限
        if(!Settings.canDrawOverlays(this)){
            showToast("请到设置—>应用—>权限 中打开悬浮穿权限");
            finish();
        }

        if(!checkAppUsagePermission(this)){
            showDialog(getApplicationContext());
        }else {
            sharedPreferences = getSharedPreferences("isFirst",MODE_PRIVATE);
            editor = sharedPreferences.edit();
            isFirst = sharedPreferences.getBoolean("isFirst",true);
            if(isFirst){
                //第一次启动APP
                editor.putBoolean("isFirst",false);
                editor.commit();
                finish();
            }else{
                activeEngine();
                if(!isServiceRunning(this,"com.sicnu.cs.zzy.classificationtimer.MyService")){
                    MyService.toMainService(this);
                }
                if(!isServiceRunning(this,"com.sicnu.cs.zzy.classificationtimer.MonitorService")){
                    MonitorService.toMonitorService(this);
                }
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }


    @Override
    protected void onDestroy() {
        AppManager.getInstance().finishActivity(this);
        super.onDestroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                activeEngine();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }


    /**
     *弹出对话框,提示用户跳转到授权界面
     */
    private void showDialog(Context context){
        AlertDialog.Builder builder= new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_checkpermission)
                .setCancelable(false)
                .setTitle("请开启所需要的权限")
                .setMessage("即将前往授权界面,确认后重启APP")
                .setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(intent);
                            AppManager.getInstance().AppExit(getApplicationContext());
                        } catch (ActivityNotFoundException e) {
                            Logger.e("Start usage access settings activity fail!","ActivityNotFoundException");
                        }
                    }
                });
        //builder.create().show();
        Dialog dialog=builder.create();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }else{
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        dialog.show();
    }


    /**
     * 检查应用是否有查看USAGE权限
     *
     */
    public static boolean checkAppUsagePermission(Context context) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if(usageStatsManager == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        // try to get app usage state in last 1 min
        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 60 * 1000, currentTime);
        if (stats.size() == 0) {
            return false;
        }
        return true;
    }


    /**
     * 弹出提示信息
     *
     */
    private void showToast(String s) {
        if (toast == null) {
            toast = Toast.makeText(this, s, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast.setText(s);
            toast.show();
        }
    }


    /**
     * 检查人脸识别所需要的权限
     *
     */
    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }


    /**
     * 激活引擎
     */
    public void activeEngine() {
        Logger.t("Face").d("activeEngine Start");
        if (!checkPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                int activeCode = faceEngine.activeOnline(MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            showToast(getString(R.string.active_success));
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
//                            showToast(getString(R.string.already_activated));
                        } else {
                            showToast(getString(R.string.active_failed, activeCode));
                        }
                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = faceEngine.getActiveFileInfo(MainActivity.this,activeFileInfo);
                        if (res == ErrorInfo.MOK) {
//                            Log.i(TAG, activeFileInfo.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        Logger.t("Face").d("activeEngine End");
    }


    /**
     * 判断服务是否开启
     * @param ServiceName 服务的完整路径(例:com.example.service
     */
    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }


}
