package com.nina.dragicevic;

public class Student {

    private int slikaResId;
    private String ime;
    private String prezime;

    private String index;

    private boolean isCheckBox;
    private String username;

    public Student(int slikaResId, String ime,String prezime, String index, boolean isCheckBox, String username) {
        this.slikaResId = slikaResId;
        this.ime = ime;
        this.prezime = prezime;
        this.index = index;
        this.isCheckBox = isCheckBox;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public int getSlikaResId() {
        return slikaResId;
    }

    public void setSlikaResId(int slikaResId) {
        this.slikaResId = slikaResId;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
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