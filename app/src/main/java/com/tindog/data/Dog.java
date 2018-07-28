package com.tindog.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Dog implements Parcelable {

    public Dog() {}
    Dog(String name, String gender, String race, String street, String city, String country, String age) {
        this.nm = name;
        this.gn = gender;
        this.rc = race;
        this.st = street;
        this.ct = city;
        this.cn = country;
        this.ag = age;
    }
    public Dog(String uI) {
        this.uI = uI;
    }

    private String nm = ""; //name

    protected Dog(Parcel in) {
        nm = in.readString();
        uI = in.readString();
        afCP = in.readString();
        aFid = in.readString();
        fN = in.readString();
        cn = in.readString();
        ct = in.readString();
        st = in.readString();
        stN = in.readString();
        gac = in.readString();
        galt = in.readString();
        galg = in.readString();
        vU = in.createStringArrayList();
        ag = in.readString();
        sz = in.readString();
        rc = in.readString();
        gn = in.readString();
        bh = in.readString();
        it = in.readString();
        hs = in.readString();
        iUT = in.createStringArrayList();
    }

    public static final Creator<Dog> CREATOR = new Creator<Dog>() {
        @Override
        public Dog createFromParcel(Parcel in) {
            return new Dog(in);
        }

        @Override
        public Dog[] newArray(int size) {
            return new Dog[size];
        }
    };

    public String getNm() {
        return nm;
    }
    public void setNm(String nm) {
        this.nm = nm;
    }

    private String uI = ""; //Unique identifier
    public String getUI() {
        return uI;
    }
    public void setUI(String uI) {
        this.uI = uI;
    }

    private String afCP; //Associated Foundation contact phone
    public String getAFCP() {
        return afCP;
    }
    public void setAFCP(String FCP) {
        this.afCP = FCP;
    }

    private String aFid = ""; //Associated Foundation unique id
    public String getAFid() {
        return aFid;
    }
    public void setAFid(String aFid) {
        this.aFid = aFid;
    }

    private String fN = ""; //Associated Foundation name
    public String getFN() {
        return fN;
    }
    public void setFN(String fN) {
        this.fN = fN;
    }

    private String cn = ""; //country
    public String getCn() {
        return cn;
    }
    public void setCn(String cn) {
        this.cn = cn;
    }

    private String ct = ""; //city
    public String getCt() {
        return ct;
    }
    public void setCt(String ct) {
        this.ct = ct;
    }

    private String se = ""; //state
    public String getSe() {
        return se;
    }
    public void setSe(String se) {
        this.se = se;
    }

    private String st = ""; //street
    public String getSt() {
        return st;
    }
    public void setSt(String st) {
        this.st = st;
    }

    private String stN = ""; //street number
    public String getStN() {
        return stN;
    }
    public void setStN(String stN) {
        this.stN = stN;
    }

    private String gac; //Geocoder address Country (requires internet to update)
    public String getGaC() {
        return gac;
    }
    public void setGaC(String gac) {
        this.gac = gac;
    }

    private String galt = "0.0"; //Geocoder address Latitude (requires internet to update)
    public String getGaLt() {
        return galt;
    }
    public void setGaLt(String galt) {
        this.galt = galt;
    }

    private String galg = "0.0"; //Geocoder address Longitude (requires internet to update)
    public String getGaLg() {
        return galg;
    }
    public void setGaLg(String galg) {
        this.galg = galg;
    }

    private List<String> vU = new ArrayList<>(); //video urls
    public List<String> getVU() {
        return vU;
    }
    public void setVU(List<String> vU) {
        this.vU = vU;
    }

    private String ag  = "No age available"; // age
    public String getAg() {
        return ag;
    }
    public void setAg(String ag) {
        this.ag = ag;
    }

    private String sz  = "No size available"; //size
    public String getSz() {
        return sz;
    }
    public void setSz(String sz) {
        this.sz = sz;
    }

    private String rc = "No race available"; //race
    public String getRc() {
        return rc;
    }
    public void setRc(String rc) {
        this.rc = rc;
    }

    private String gn  = "No gender available"; //gender
    public String getGn() {
        return gn;
    }
    public void setGn(String gn) {
        this.gn = gn;
    }

    private String bh  = "No behavior available"; //behavior
    public String getBh() {
        return bh;
    }
    public void setBh(String bh) {
        this.bh = bh;
    }

    private String it  = "No interactions available"; //interactions
    public String getIt() {
        return it;
    }
    public void setIt(String it) {
        this.it = it;
    }

    private String hs  = "No history available"; //history
    public String getHs() {
        return hs;
    }
    public void setHs(String hs) {
        this.hs = hs;
    }

    private List<String> iUT = Arrays.asList("","","","","",""); //Image upload times
    public List<String> getIUT() {
        return iUT;
    }
    public void setIUT(List<String> iUT) {
        this.iUT = iUT;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(nm);
        parcel.writeString(uI);
        parcel.writeString(afCP);
        parcel.writeString(aFid);
        parcel.writeString(fN);
        parcel.writeString(cn);
        parcel.writeString(ct);
        parcel.writeString(st);
        parcel.writeString(stN);
        parcel.writeString(gac);
        parcel.writeString(galt);
        parcel.writeString(galg);
        parcel.writeStringList(vU);
        parcel.writeString(ag);
        parcel.writeString(sz);
        parcel.writeString(rc);
        parcel.writeString(gn);
        parcel.writeString(bh);
        parcel.writeString(it);
        parcel.writeString(hs);
        parcel.writeStringList(iUT);
    }
}
