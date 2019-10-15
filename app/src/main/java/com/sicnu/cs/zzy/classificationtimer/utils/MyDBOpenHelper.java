package com.sicnu.cs.zzy.classificationtimer.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDBOpenHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "own.db";

    public MyDBOpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table watchlist(" +
                "packgename varchar(50) primary key," +
                "usetime varchar(20))");

        db.execSQL("create table killlist(" +
                "packgename varchar(50) primary key)");

        db.execSQL("insert into watchlist(packgename,usetime) values (?,?)",
                new String[]{"com.sicnu.zzy.managerapp","20000"});
        db.execSQL("insert into watchlist(packgename,usetime) values (?,?)",
                new String[]{"com.example.librarytest","3000"});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
