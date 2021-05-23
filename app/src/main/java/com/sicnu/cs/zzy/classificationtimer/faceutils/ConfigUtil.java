package com.sicnu.cs.zzy.classificationtimer.faceutils;

import android.content.Context;
import android.content.SharedPreferences;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.enums.DetectFaceOrientPriority;

//public class ConfigUtil {
//    private static final String APP_NAME = "ArcFaceDemo";
//    private static final String TRACK_ID = "trackID";
//    private static final String FT_ORIENT = "ftOrient";
//
//    public static void setTrackId(Context context, int trackId) {
//        if (context == null) {
//            return;
//        }
//        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
//        sharedPreferences.edit()
//                .putInt(TRACK_ID, trackId)
//                .apply();
//    }
//
//    public static int getTrackId(Context context){
//        if (context == null){
//            return 0;
//        }
//        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME,Context.MODE_PRIVATE);
//        return sharedPreferences.getInt(TRACK_ID,0);
//    }
//    public static void setFtOrient(Context context, int ftOrient) {
//        if (context == null) {
//            return;
//        }
//        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
//        sharedPreferences.edit()
//                .putInt(FT_ORIENT, ftOrient)
//                .apply();
//    }
//
//    public static int getFtOrient(Context context){
//        if (context == null){
//            return FaceEngine.ASF_OP_270_ONLY;
//        }
//        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME,Context.MODE_PRIVATE);
//        return sharedPreferences.getInt(FT_ORIENT, FaceEngine.ASF_OP_270_ONLY);
//    }
//}

public class ConfigUtil {
    private static final String APP_NAME = "ArcFaceDemo";
    private static final String TRACK_ID = "trackID";
    private static final String FT_ORIENT = "ftOrient";
    private static final String TRACKED_FACE_COUNT = "trackedFaceCount";
    private static final String MAC_PRIORITY = "macPriority";

    public static boolean setTrackedFaceCount(Context context, int trackedFaceCount) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.edit()
                .putInt(TRACKED_FACE_COUNT, trackedFaceCount)
                .commit();
    }

    public static int getTrackedFaceCount(Context context) {
        if (context == null) {
            return 0;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(TRACKED_FACE_COUNT, 0);
    }

    public static boolean setFtOrient(Context context, DetectFaceOrientPriority ftOrient) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.edit()
                .putString(FT_ORIENT, ftOrient.name())
                .commit();
    }

    public static DetectFaceOrientPriority getFtOrient(Context context) {
        if (context == null) {
            return DetectFaceOrientPriority.ASF_OP_270_ONLY;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return DetectFaceOrientPriority.valueOf(sharedPreferences.getString(FT_ORIENT, DetectFaceOrientPriority.ASF_OP_270_ONLY.name()));
    }
}

