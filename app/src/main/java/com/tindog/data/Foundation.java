package com.tindog.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.tindog.resources.Utilities;

import java.util.Arrays;
import java.util.List;

public class Foundation implements Parcelable {

    public Foundation() { }
    Foundation(String name, String street, String city, String country) {
        this.st = street;
        this.nm = name;
        this.ct = city;
        this.cn = country;
    }
    public Foundation(String ownerfirebaseUid) {
        this.oFId = ownerfirebaseUid;
        setUniqueIdentifierFromDetails();
    }

    protected Foundation(Parcel in) {
        nm = in.readString();
        oFId = in.readString();
        uI = in.readString();
        wb = in.readString();
        cn = in.readString();
        ct = in.readString();
        st = in.readString();
        stN = in.readString();
        cP = in.readString();
        cE = in.readString();
        iUT = in.createStringArrayList();
        gac = in.readString();
        galt = in.readString();
        galg = in.readString();
    }

    public static final Creator<Foundation> CREATOR = new Creator<Foundation>() {
        @Override
        public Foundation createFromParcel(Parcel in) {
            return new Foundation(in);
        }

        @Override
        public Foundation[] newArray(int size) {
            return new Foundation[size];
        }
    };

    private String nm = "Default"; //name
    public String getNm() {
        return nm;
    }
    public void setNm(String nm) {
        this.nm = nm;
    }

    private String oFId; // ownerfirebaseUid
    public String getOFId() {
        return oFId;
    }
    public void setOFId(String Ouid) {
        this.oFId = Ouid;
        setUniqueIdentifierFromDetails();
    }

    private String uI = ""; //unique identifier
    public String getUI() {
        return Utilities.cleanIdentifierForFirebase(uI);
    }
    public void setUI(String uI) {
        this.uI = Utilities.cleanIdentifierForFirebase(uI);
    }
    public void setUniqueIdentifierFromDetails() {
        if (!TextUtils.isEmpty(uI)) { } //Keep the uI
        else if (TextUtils.isEmpty(oFId)) uI = nm + "-" + ct + "-" + cn;
        else uI = oFId;
        uI = Utilities.cleanIdentifierForFirebase(uI);
    }

    private String wb = ""; //website
    public String getWb() {
        return wb;
    }
    public void setWb(String wb) {
        this.wb = wb;
    }

    private String cn = ""; //country
    public String getCn() {
        return cn;
    }
    public void setCn(String cn) {
        this.cn = cn;
    }

    private String se = ""; //state
    public String getSe() {
        return se;
    }
    public void setSe(String se) {
        this.se = se;
    }

    private String ct = ""; //city
    public String getCt() {
        return ct;
    }
    public void setCt(String ct) {
        this.ct = ct;
    }

    private String st = ""; //street
    public String getSt() {
        return st;
    }
    public void setSt(String st) {
        this.st = st;
    }

    private String stN = ""; //Street number
    public String getStN() {
        return stN;
    }
    public void setStN(String stN) {
        this.stN = stN;
    }

    private String cP = ""; //contect phone
    public String getCP() {
        return cP;
    }
    public void setCP(String cP) {
        this.cP = cP;
    }

    private String cE = ""; //contact email
    public String getCE() {
        return cE;
    }
    public void setCE(String cE) {
        this.cE = cE;
    }

    private List<String> iUT = Arrays.asList("","","","","",""); //Image upload times
    public List<String> getIUT() {
        return iUT;
    }
    public void setIUT(List<String> iUT) {
        this.iUT = iUT;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(nm);
        parcel.writeString(oFId);
        parcel.writeString(uI);
        parcel.writeString(wb);
        parcel.writeString(cn);
        parcel.writeString(ct);
        parcel.writeString(st);
        parcel.writeString(stN);
        parcel.writeString(cP);
        parcel.writeString(cE);
        parcel.writeStringList(iUT);
        parcel.writeString(gac);
        parcel.writeString(galt);
        parcel.writeString(galg);
    }
}
