package com.sicnu.cs.zzy.classificationtimer.utils;

import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.sicnu.cs.zzy.classificationtimer.R;
import com.sicnu.cs.zzy.classificationtimer.javaBean.ProcessInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class UsageStatsUtils {
    private static String TAG = "UsageStatsUtils";

    /**
     * 获取某个时间段应用使用情况
     *
     */
    public static ArrayList<UsageStats> getUsageList(Context context, long startTime, long endTime) {
        ArrayList<UsageStats> list = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        UsageStatsManager mUsmManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        Map<String, UsageStats> map = mUsmManager.queryAndAggregateUsageStats(startTime, endTime);
        for (Map.Entry<String, UsageStats> entry : map.entrySet()) {
            UsageStats stats = entry.getValue();
            if(stats.getTotalTimeInForeground() > 0){
                if(!isSystemApp(pm,stats.getPackageName())){
                    list.add(stats);
                }
            }
        }
        return list;
    }


    /**
     * 获取某个时间段应用具体发生的事件
     *
     */
    public static ArrayList<UsageEvents.Event> getEventList(Context context, long startTime, long endTime){
        ArrayList<UsageEvents.Event> mEventList = new ArrayList<>();

        UsageStatsManager mUsmManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = mUsmManager.queryEvents(startTime, endTime);

        while (events.hasNextEvent()) {
            UsageEvents.Event e = new UsageEvents.Event();
            events.getNextEvent(e);
            if (e != null && (e.getEventType() == 1 || e.getEventType() == 2)) {
                Log.i(TAG," EventUtils-getEventList()  "+e.getTimeStamp()+"   event:" + e.getClassName() + "   type = " + e.getEventType());
                mEventList.add(e);
            }
        }
        return mEventList;
    }


    /**
     * 查询10秒钟前应用使用统计数据并根据最后使用时间进行排序，
     * 得到的最后使用的应用。
     *
     */
    public static String getTopActivityPackageName(Context context) {
        String PACKAGE_NAME_UNKNOWN = "";
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if(usageStatsManager == null) {
            return PACKAGE_NAME_UNKNOWN;
        }

        String topActivityPackageName = PACKAGE_NAME_UNKNOWN;
        long time = System.currentTimeMillis();
        // 查询最后十秒钟使用应用统计数据
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000*10, time);
        // 以最后使用时间为标准进行排序
        if(usageStatsList != null) {
            SortedMap<Long,UsageStats> sortedMap = new TreeMap<Long,UsageStats>();
            for (UsageStats usageStats : usageStatsList) {
                sortedMap.put(usageStats.getLastTimeUsed(),usageStats);
            }
            if(sortedMap.size() != 0) {
                topActivityPackageName =  sortedMap.get(sortedMap.lastKey()).getPackageName();
                Log.d(TAG,"Top activity package name = " + topActivityPackageName);
            }
        }
        return topActivityPackageName;
    }


    /**
     * 获取已安装所有第三方应用信息
     *
     */
    public static ArrayList<ProcessInfo> getAllApplications(Context context) {
        ArrayList<ProcessInfo> processInfoList = new ArrayList<>();
        PackageManager PM = context.getPackageManager();
        try {
            Process process = Runtime.getRuntime().exec("pm list package -3");
            BufferedReader bis = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = bis.readLine()) != null) {
                ProcessInfo processInfo=new ProcessInfo();
                String packName = line.substring(line.indexOf(":")+1);
                processInfo.setPackageName(packName);
                try {
                    ApplicationInfo applicationInfo = PM.getApplicationInfo(processInfo.getPackageName(), 0);
                    processInfo.setName(applicationInfo.loadLabel(PM).toString());
                    processInfo.setIcon(applicationInfo.loadIcon(PM));
                    if ((applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==ApplicationInfo.FLAG_SYSTEM){
                        processInfo.setUser(false);
                    }else {
                        processInfo.setUser(true);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    processInfo.setName(processInfo.getPackageName());
                    processInfo.setIcon(context.getDrawable(R.drawable.ic_default));
                    processInfo.setUser(false);
                    e.printStackTrace();
                }
                if(processInfo.isUser() && !processInfo.getPackageName().equals("com.sicnu.cs.zzy.classificationtimer")){
                    processInfoList.add(processInfo);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return processInfoList;
    }


    /**
     * 获取正在运行的APP列表
     * 注：因为5.0之后谷歌做了限制，已经拿不到进程列表了，所
     * 以只能通过此方法近似的获得
     *
     */
    public static ArrayList<ProcessInfo> getProcessInfo(Context context){
        ArrayList<ProcessInfo> processInfoList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (m != null) {
                long now = System.currentTimeMillis();
                //获取60秒之内的应用数据
                List<UsageStats> stats = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 30 * 1000, now);
                ActivityManager systemService = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                Log.i(TAG, "Running app number in last 60 seconds : " + stats.size());
                //取得最近运行的一个app，即当前运行的app
                if ((stats != null) && (!stats.isEmpty())) {
                    for (int i = 0; i < stats.size(); i++) {
                       /* if (stats.get(i).getLastTimeUsed() > stats.get(j).getLastTimeUsed()) {
                            j = i;
                        }*/
                        int i1 = stats.get(i).describeContents();
                        String processName = stats.get(i).getPackageName();
                        Log.i(TAG, "top running app is : " + processName);
                        PackageManager PM = context.getPackageManager();
                        ProcessInfo processInfo=new ProcessInfo();
                        processInfo.setPackageName(processName);
                        //获取应用的名称
                        try {
                            ApplicationInfo applicationInfo = PM.getApplicationInfo(processInfo.getPackageName(), 0);
                            processInfo.setName(applicationInfo.loadLabel(PM).toString());
                            processInfo.setIcon(applicationInfo.loadIcon(PM));
                            if ((applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==ApplicationInfo.FLAG_SYSTEM){
                                processInfo.setUser(false);
                            }else {
                                processInfo.setUser(true);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            processInfo.setName(processInfo.getPackageName());
                            processInfo.setIcon(context.getDrawable(R.drawable.ic_default));
                            processInfo.setUser(false);
                            e.printStackTrace();
                        }
                        processInfoList.add(processInfo);
                    }
                }
            }
        }
        return processInfoList;
    }


    /**
     * 判断是否是系统应用
     *
     */
    private static boolean isSystemApp(PackageManager pm,String packageName){
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);//根据包名获取相关应用的信息
            int flags = applicationInfo.flags;
            if((flags & ApplicationInfo.FLAG_SYSTEM)==ApplicationInfo.FLAG_SYSTEM){
                //系统进程
                return true;
            }else{
                //用户进程
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return true;
        }
    }

}
