package com.tindog.data;

import android.os.Parcel;
import android.os.Parcelable;

public class MapMarker implements Parcelable {

    public MapMarker() {}

    protected MapMarker(Parcel in) {
        lg = in.readString();
        lt = in.readString();
        tt = in.readString();
        sn = in.readString();
        cl = in.readString();
        uI = in.readString();
        oI = in.readString();
    }

    public static final Creator<MapMarker> CREATOR = new Creator<MapMarker>() {
        @Override
        public MapMarker createFromParcel(Parcel in) {
            return new MapMarker(in);
        }

        @Override
        public MapMarker[] newArray(int size) {
            return new MapMarker[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(lg);
        parcel.writeString(lt);
        parcel.writeString(tt);
        parcel.writeString(sn);
        parcel.writeString(cl);
        parcel.writeString(uI);
        parcel.writeString(oI);
    }
}
