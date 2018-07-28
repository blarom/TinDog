package com.tindog.data;

public class MapMarker {


    public MapMarker() {}

    private String lg = ""; //longitude
    public String getLg() {
        return lg;
    }
    public void setLg(String lg) {
        this.lg = lg;
    }

    private String lt = ""; //latitude
    public String getLt() {
        return lt;
    }
    public void setLt(String lt) {
        this.lt = lt;
    }

    private String tt = ""; //title
    public String getTt() {
        return tt;
    }
    public void setTt(String tt) {
        this.tt = tt;
    }

    private String sn = ""; //snippet
    public String getSn() {
        return sn;
    }
    public void setSn(String sn) {
        this.sn = sn;
    }

    private String cl = "white"; //color
    public String getCl() {
        return cl;
    }
    public void setCl(String cl) {
        this.cl = cl;
    }

    private String uI = ""; //unique identifier
    public String getUI() {
        return uI;
    }
    public void setUI(String uI) {
        this.uI = uI;
    }

    private String oI = ""; //owner identifier
    public String getOI() {
        return oI;
    }
    public void setOI(String oI) {
        this.oI = oI;
    }
}
