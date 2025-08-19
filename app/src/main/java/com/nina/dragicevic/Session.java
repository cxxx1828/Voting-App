package com.nina.dragicevic;

public class Session {

    private String datum;

    private String naziv;

    private String atribut;

    public Session(String datum, String naziv, String atribut) {
        this.datum = datum;
        this.naziv = naziv;
        this.atribut = atribut;
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getAtribut() {
        return atribut;
    }

    public void setAtribut(String atribut) {
        this.atribut = atribut;
    }
}
