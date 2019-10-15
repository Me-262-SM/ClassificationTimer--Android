package com.sicnu.cs.zzy.classificationtimer.javaBean;

import android.graphics.drawable.Drawable;

public class ProcessInfo {
    private String name;
    private String packageName;
    private Drawable icon;
    private boolean isUser; //true表示用户进程

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean user) {
        isUser = user;
    }
}