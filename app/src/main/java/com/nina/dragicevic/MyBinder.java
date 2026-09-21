package com.nina.dragicevic;

import android.os.Binder;
//binder

public class MyBinder extends Binder {

    private boolean serviceRunning = false;
    private int checkedSessionsCount = 0;


    public boolean isServiceRunning() {
        return serviceRunning;
    }


    public void setServiceStatus(boolean isRunning) {
        this.serviceRunning = isRunning;
    }


    public int getCheckedSessionsCount() {
        return checkedSessionsCount;
    }

    public void setCheckedSessionsCount(int count) {
        this.checkedSessionsCount = count;
    }
}
