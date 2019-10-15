package com.sicnu.cs.zzy.classificationtimer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.sicnu.cs.zzy.classificationtimer.javaBean.ProcessInfo;
import com.sicnu.cs.zzy.classificationtimer.utils.DateTransUtils;
import com.sicnu.cs.zzy.classificationtimer.utils.MyDBOpenHelper;
import com.sicnu.cs.zzy.classificationtimer.utils.ProcessUtils;
import com.sicnu.cs.zzy.classificationtimer.utils.UsageStatsUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorService extends Service {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private SQLiteDatabase db;
    private ArrayList<ProcessInfo> list = new ArrayList<>();

    private final Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            //"循环"监视已经到达使用时长的应用是否重新启动，如果启动，则杀掉
            boolean isChildren = sharedPreferences.getBoolean("isChildren", false);
            if(isChildren){
                //判断当前应用否为需要杀死的应用
                Cursor cursor = db.rawQuery("select * from killlist",null);
                list.clear();
                for(int i = 0;i < cursor.getCount();i++){
                    cursor.moveToPosition(i);
                    String packName = cursor.getString(cursor.getColumnIndex("packgename"));
                    String nowApp = UsageStatsUtils.getTopActivityPackageName(getApplicationContext());
                    if(StringUtils.equals(packName,nowApp)){
                        ProcessInfo processInfo = new ProcessInfo();
                        processInfo.setPackageName(packName);
                        list.add(processInfo);
                    }
                }
                if(null == list || list.isEmpty()){
                    //doNothing
                }else{
                    Log.d("test","run: ");
                    ProcessUtils.getInstance(getApplicationContext()).KillProcessByPackageName(list);
//                    list.clear();
                }
            }else{
                //doNothing
            }
        }
    };


    public MonitorService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Logger.t("MyService").d("MonitorServiceCreate:  ");
        initDataBase();
        initKillList();
        initSharedPreferences();
        timer.schedule(task, 0, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.t("MyService").d("MonitorServiceDestroy: 启动守护Activity");
        Intent intent=new Intent(this,DaemonActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }


    /**
     * 启动"循环监视"服务
     */
    public static void toMonitorService(Context pContext){
        Intent intent = new Intent(pContext,MonitorService.class);
        pContext.startService(intent);
    }


    /**
     * 初始化数据库
     */
    private void initDataBase(){
        MyDBOpenHelper DBhelper = new MyDBOpenHelper(this,MyDBOpenHelper.DB_NAME,null,1);
        db = DBhelper.getWritableDatabase();
    }


    /**
     * 初始化SharedPreferences
     */
    private void initSharedPreferences(){
        sharedPreferences = getSharedPreferences("Monitor",MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }


    /**
     * 清空killist表中所有数据
     */
    public void initKillList(){
        db.execSQL("delete from killlist");
    }
}
