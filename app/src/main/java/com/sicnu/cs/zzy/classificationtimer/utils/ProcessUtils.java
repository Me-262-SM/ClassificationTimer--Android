package com.sicnu.cs.zzy.classificationtimer.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.orhanobut.logger.Logger;
import com.sicnu.cs.zzy.classificationtimer.R;
import com.sicnu.cs.zzy.classificationtimer.javaBean.ProcessInfo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;



public class ProcessUtils {
    private static ProcessUtils instance;
    private boolean choose;
    private Context mContext;


    private ProcessUtils(Context context){
        mContext = context;
        choose = false;
    }


    public static ProcessUtils getInstance(Context context){
        if(instance == null){
            instance = new ProcessUtils(context);
        }
        return instance;
    }


    /**
     * 获取正在运行的应用列表，5.0以上需要系统签名（否则列表中永远只有自身）
     * 注：部分国产手机仍然只会返回自身
     * 可以尝试将手机ROOT或者使用系统签名文件对APP签名
     */
    public ArrayList<ProcessInfo> getRunningProcesses() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();

        PackageManager pm = mContext.getPackageManager();
        ArrayList<ProcessInfo> list = new ArrayList<ProcessInfo>();
        for (int i=0;i<runningAppProcesses.size();i++){
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo = runningAppProcesses.get(i);
            ProcessInfo info = new ProcessInfo();
            String packageName = runningAppProcessInfo.processName; //包名
            info.setPackageName(packageName);
            int pid = runningAppProcessInfo.pid;
            try {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);//根据包名获取相关应用的信息
                String name = applicationInfo.loadLabel(pm).toString();
                Drawable icon = applicationInfo.loadIcon(pm);
                info.setName(name);
                info.setIcon(icon);
                int flags = applicationInfo.flags;
                if((flags & ApplicationInfo.FLAG_SYSTEM)==ApplicationInfo.FLAG_SYSTEM){
                    //系统进程
                    info.setUser(false);
                }else{
                    //用户进程
                    info.setUser(true);
                }
            }catch (PackageManager.NameNotFoundException e) {
                //某些系统进程没有名称和图标,会走此异常
                info.setName(packageName);
                info.setIcon(mContext.getDrawable(R.drawable.ic_default));
                info.setUser(false);
                //e.printStackTrace();
                Logger.t("getRunningProcesses").e("系统进程没有名称和图标","PackageManager.NameNotFoundException");
            }
            list.add(info);
        }
        return list;
    }



    /**
     * 杀死指定的应用，使用前请仔细阅读此方法
     *
     * @param list
     */
    public void  KillProcessByPackageName(ArrayList<ProcessInfo> list){
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        if(this.choose){
            //这个方法可以彻底杀掉进程，但是需要系统签名。（莫得系统签名，即使root之后照样不行）
            //这里判断版本版本是因为本人目前只有原生Android9.0的系统签名
            Method method = null;
            try {
                method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            } catch (NoSuchMethodException e) {
                Logger.t("MyService").e("NoSuchMethodException");
            } catch (ClassNotFoundException e) {
                Logger.t("MyService").e("NoSuchMethodException");
            }
            for (ProcessInfo processInfo: list){
                try {
                    method.invoke(am, processInfo.getPackageName());  //packageName是需要强制停止的应用程序包名
                } catch (IllegalAccessException e) {
                    Logger.t("MyService").e("IllegalAccessException");
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    Logger.t("MyService").e("InvocationTargetException");
                }
            }
        }else {
            //此方法只能够杀死那些按home键退到后台的进程，而且有概率杀不死
            //如果进程A在前台(用户可见)，但是被另外一个进程B"遮挡"，则此方法A和B都杀不死
            goHome();
            for(ProcessInfo processInfo: list){
                am.killBackgroundProcesses(processInfo.getPackageName());
            }
        }
    }


    /**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     *
     * @return 应用程序是/否获取Root权限
     */
    @Deprecated
    public static boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd="chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }


    /**
     * 使所有应用退到后台，类似手机Home键位
     */
    private void goHome(){
        Intent intent_home = new Intent();
        intent_home.setAction(Intent.ACTION_MAIN);
        intent_home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //如果是服务里调用，必须加入new task标识
        intent_home.addCategory(Intent.CATEGORY_HOME);
        mContext.startActivity(intent_home);
        //停半秒...目的为确保所有应用退到后台
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
