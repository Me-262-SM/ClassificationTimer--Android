package com.sicnu.cs.zzy.classificationtimer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.orhanobut.logger.Logger;
import com.sicnu.cs.zzy.classificationtimer.javaBean.ProcessInfo;
import com.sicnu.cs.zzy.classificationtimer.utils.DateTransUtils;
import com.sicnu.cs.zzy.classificationtimer.utils.MyDBOpenHelper;
import com.sicnu.cs.zzy.classificationtimer.utils.ProcessUtils;
import com.sicnu.cs.zzy.classificationtimer.utils.ScreenBroadcastListener;
import com.sicnu.cs.zzy.classificationtimer.utils.ScreenManager;
import com.sicnu.cs.zzy.classificationtimer.utils.UsageStatsUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MyService extends Service {
    public final static String BroadcastName = "face_parameter";
    public static int LIMIT_AGE = 25;   //年龄阈值
    public static int LIMIT_TIME = 12; //每五秒为一次周期，控制此变量来控制定时检测的时间间隔,默认一分钟
    private MyReceiver myReciever;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private int age;
    private boolean isRealActive;
    private boolean isFirst = true;
    private int count = 0;
    private SQLiteDatabase db;
    private Cursor watchlist_cursor;
    private RemoteViews mRemoteViews;
    private NotificationManager notificationManager;
    private Notification notification;
    private ArrayList<ProcessInfo> list = new ArrayList<>();    //正在运行的列表
    private ArrayList<ProcessInfo> kill_list = new ArrayList<>();    //要杀死的列表
    private ArrayList<UsageStats>  usageStats_list = new ArrayList<>();    //使用情况列表

    private final Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            mhandler.sendMessage(message);
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mhandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    timeCount();
                    break;
                case 2:
                    showDialog(getApplicationContext());
                    break;
                case 3:
                    verificationHandle();
                    break;
                default:
                    break;
            }
        }
    };


    public MyService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        Logger.t("MyService").d("MainServiceCreate");
        super.onCreate();
        initSharedPreferences();
        initNotification();
        initDataBase();
        initBroadcastReceiver();
        timer.schedule(task, 5000, 5000);
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(myReciever);
        Logger.t("MyService").d("MainServiceDestroy");
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(isFirst){
            final ScreenManager screenManager = ScreenManager.getInstance(this);
            ScreenBroadcastListener listener = new ScreenBroadcastListener(this);
            listener.registerListener(new ScreenBroadcastListener.ScreenStateListener() {
                @Override
                public void onScreenOn() {
                    screenManager.finishActivity();
                }

                @Override
                public void onScreenOff() {
                    screenManager.startActivity();
                }
            });
            startForeground(10, notification);
            isFirst = !isFirst;
        }
        return START_STICKY;
    }


    /**
     * 启动主服务
     */
    public static void toMainService(Context pContext){
        Intent intent = new Intent(pContext,MyService.class);
        pContext.startService(intent);
    }


    /**
     * 定时任务计数
     */
    private void timeCount(){
        if(count == 0 ){
            //Logger.t("MyService").d("One Minite Start");
        }
        count++;
        if(count == LIMIT_TIME ){
            count = 0;
            Logger.t("MyService").d("Time Coming");
            Message message = new Message();
            message.what = 2;
            mhandler.sendMessage(message);
        }
    }


    /**
     * 显示提示框
     */
    private void showDialog(Context context){
        AlertDialog.Builder builder= new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_face_recognition)
                .setCancelable(false)
                .setTitle("通知")
                .setMessage("请进行定时检测!!!")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(getBaseContext(), PreviewActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(intent);
                    }
                });
        Dialog dialog=builder.create();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }else{
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        dialog.show();
    }


    /**
     * 弹出框
     */
    private void showToast(String string){
        Toast.makeText(getApplicationContext(),string,Toast.LENGTH_SHORT).show();
    }


    /**
     * 将已经使用超时的应用列表添加到数据库中
     */
    private void addKillListToDB(ArrayList<ProcessInfo> list){
        for(ProcessInfo info:list){
            String packName = info.getPackageName();
            Cursor cursor = db.rawQuery("select * from killlist"+" WHERE packgename = ?",
                    new String[]{packName});
            if(cursor.getCount() == 1){
                //如果存在则不插入
            }else {
                //不存在则插入
                db.execSQL("insert into killlist"+"(packgename) values(?)",
                        new String[]{packName});
            }
        }
    }


    /**
     * 用于人脸识别之后的处理
     * 注：这里可以直接判断监视列表中的应用是否超出了使用时间,但是可鞥会出现
     * 应用虽然使用超时但是并未启动，造成"杀空"，所以这里需要先拿到运行中的进
     * 程列表。(但是会不准确)
     */
    private void verificationHandle(){
        watchlist_cursor = db.rawQuery("select * from watchlist",null);
        list = UsageStatsUtils.getProcessInfo(getApplicationContext());
        for(ProcessInfo processInfo:list){
            if(processInfo.isUser()){
                if(!StringUtils.equals(getPackageName(),processInfo.getPackageName())){
                    if(isOnTheWatchList(processInfo.getPackageName())){
                        Logger.t("MyService").d("RuningProcesses: "+processInfo.getName());
                        kill_list.add(processInfo);
                    }
                }
            }
        }

        for(int i = 0;i < watchlist_cursor.getCount();i++){
            watchlist_cursor.moveToPosition(i);
            String packgename = watchlist_cursor.getString(watchlist_cursor.getColumnIndex("packgename"));
            String usetime = watchlist_cursor.getString(watchlist_cursor.getColumnIndex("usetime"));

            long startTime = DateTransUtils.getZeroClockTimestamp();
            long endTime = System.currentTimeMillis();
            usageStats_list = UsageStatsUtils.getUsageList(this,startTime,endTime);

            int index_usageStats = indexUsageStats(packgename);
            int index_processInfo = indexProcessInfo(packgename);
            if(index_usageStats == -1 || index_processInfo == -1){
                //列表为空
            }else{
                long totaltime = usageStats_list.get(index_usageStats).getTotalTimeInForeground();
                if(totaltime < Long.parseLong(usetime)){
                    kill_list.remove(index_processInfo);
                }
            }
        }
        if(null == kill_list || kill_list.isEmpty()){
            //没有应用程序正在运行或者没有需要杀死的应用程序
        }else {
            editor.putBoolean("isChildren",true);
            editor.commit();
            ProcessUtils.getInstance(getApplicationContext()).KillProcessByPackageName(kill_list);
            addKillListToDB(kill_list);
            kill_list.clear();
        }
    }


    /*
    * 根据包名查找处于各自列表中的位置
    */
    private int indexUsageStats(String packgename){
        for(int i=0;i<usageStats_list.size();i++){
            if(StringUtils.equals(usageStats_list.get(i).getPackageName(),packgename)){
                return i;
            }
        }
        return -1;
    }


    private int indexProcessInfo(String packgename){
        for(int i=0;i<kill_list.size();i++){
            if(StringUtils.equals(kill_list.get(i).getPackageName(),packgename)){
                return i;
            }
        }
        return -1;
    }


    /**
     * 是否在监控名单上
     *
     */
    private boolean isOnTheWatchList(String packName){
        for(int i = 0;i < watchlist_cursor.getCount();i++){
            watchlist_cursor.moveToPosition(i);
            String packgename = watchlist_cursor.getString(watchlist_cursor.getColumnIndex("packgename"));
            if(StringUtils.equals(packName,packgename)){
                return true;
            }
        }
        return false;
    }


    /**
     * 初始化消息栏
     */
    private void initNotification(){
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.mynotification_window_layout);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra("isToSetting",true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0x51,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        //判断API版本
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            Notification.Builder builder =new Notification.Builder(this);
            builder.setContentTitle("后台服务正在运行")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setWhen(System.currentTimeMillis());
            notification = builder.build();
        }else {
            NotificationChannel channel = new NotificationChannel("monitor", "check", NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            //channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLightColor(0xFFFF0000);
            //channel.setVibrationPattern(new long[]{0,300});
            notificationManager.createNotificationChannel(channel);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"monitor");
            builder.setContent(mRemoteViews)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_notification);
            notification = builder.build();
        }
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
     * 初始化广播接收器（注册动态广播）
     */
    private void initBroadcastReceiver(){
        myReciever = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastName);
        registerReceiver(myReciever,intentFilter);
    }


    /**
     *  广播接收器
     */
    class MyReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isToSetting = intent.getBooleanExtra("isToSetting",false);
            age = intent.getIntExtra("Age",-1);
            Toast.makeText(getApplicationContext(),"检测年龄为：" + age, Toast.LENGTH_SHORT).show();
            isRealActive = intent.getBooleanExtra("isRealActive", true);
            if(isRealActive){
                if(age > 0 && age < LIMIT_AGE){
                    if(isToSetting){
                        showToast("您没有权限进入设置界面");
                    }else{
                        //处理
                        Message message = new Message();
                        message.what = 3;
                        mhandler.sendMessage(message);
                    }
                }else{
                    if(isToSetting){
                        Intent intent_setting = new Intent(MyService.this, SettingActivity.class);
                        intent_setting.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent_setting);
                    }else{
                        editor.putBoolean("isChildren",false);
                        editor.commit();
                    }
                }
            }else{
                //认证失败,非活体
                showToast("检测失败！！！");
            }
        }
    }



}
