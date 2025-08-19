package com.nina.dragicevic;

public class Student {

    private int slikaResId;
    private String imePrezime;
    private String index;
    private boolean isCheckBox;

    public Student(int slikaResId, String imePrezime, String index, boolean isCheckBox) {
        this.slikaResId = slikaResId;
        this.imePrezime = imePrezime;
        this.index = index;
        this.isCheckBox = isCheckBox;
    }

    public int getSlikaResId() {
        return slikaResId;
    }

    public void setSlikaResId(int slikaResId) {
        this.slikaResId = slikaResId;
    }

    public String getImePrezime() {
        return imePrezime;
    }

    public void setImePrezime(String imePrezime) {
        this.imePrezime = imePrezime;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public boolean isCheckBox() {
        return isCheckBox;
    }

    public void setCheckBox(boolean checkBox) {
        isCheckBox = checkBox;
    }
}
