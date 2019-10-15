package com.sicnu.cs.zzy.classificationtimer.utils;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTransUtils {
    private static final SimpleDateFormat dateFormat  = new SimpleDateFormat("M-d-yyyy");
    public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

    /**
     * 将时间戳转换为时间
     *
     */
    public static String stampToDate(String stamp){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = new Long(stamp);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }
    public static String stampToDate(long stamp){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(stamp);
        res = simpleDateFormat.format(date);
        return res;
    }

    /**
     * 获取今日某时间的时间戳
     *
     */
    public static long getTodayStartStamp(int hour,int minute,int second){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        long todayStamp = cal.getTimeInMillis();

        Log.i("Wingbu"," DateTransUtils-getTodayStartStamp()  获取当日" + hour+ ":" + minute+ ":" + second+ "的时间戳 :" + todayStamp);

        return todayStamp;
    }

    /**
     * 获取当日00:00:00的时间戳,东八区则为早上八点
     *
     */
    public static long getZeroClockTimestamp(long time){
        long currentStamp = time;
        currentStamp -= currentStamp % DAY_IN_MILLIS;
        Log.i("Wingbu"," DateTransUtils-getZeroClockTimestamp()  获取当日00:00:00的时间戳,东八区则为早上八点 :" + currentStamp);
        return currentStamp;
    }

    /**
     * 获取当日00:00:00的时间戳，屏蔽东八区的影响
     *
     */
    public static Long getZeroClockTimestamp() {
        Calendar calendar = Calendar.getInstance();// 获取当前日期
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Long time = calendar.getTimeInMillis();
        return time;
    }

    /**
     * 获取最近7天的日期,用于查询这7天的系统数据
     *
     */
    public static ArrayList<String> getSearchDays(){
        ArrayList<String> dayList = new ArrayList<>();
        for(int i = 0 ; i < 7 ; i++){
            dayList.add(getDateString(i));
        }
        return dayList;
    }

    /**
     * 获取dayNumber天前，当天的日期字符串
     *
     */
    public static String getDateString(int dayNumber){
        long time = System.currentTimeMillis() - dayNumber * DAY_IN_MILLIS;
        Log.i("Wingbu"," DateTransUtils-getDateString()  获取查询的日期 :" + dateFormat.format(time));
        return dateFormat.format(time);
    }

    /**
     * 将毫秒数据转化为时分秒
     *
     */
    public static String msToHMS(long ms){
        SimpleDateFormat formatter = new SimpleDateFormat("H小时m分s秒");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(ms);
        return hms;
    }

}