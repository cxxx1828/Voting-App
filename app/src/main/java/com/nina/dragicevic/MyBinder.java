package com.nina.dragicevic;

import android.os.Binder;
//binder

public class MyBinder extends Binder {

    private boolean serviceRunning = false;
    private int checkedSessionsCount = 0;

    //Dobija trenutni status servisa

    public boolean isServiceRunning() {
        return serviceRunning;
    }

    //Postavlja status servisa

    public void setServiceStatus(boolean isRunning) {
        this.serviceRunning = isRunning;
    }

    //Dobija broj proverenih sesija

    public int getCheckedSessionsCount() {
        return checkedSessionsCount;
    }

    //Postavlja broj proverenih sesija
    public void setCheckedSessionsCount(int count) {
        this.checkedSessionsCount = count;
    }
}