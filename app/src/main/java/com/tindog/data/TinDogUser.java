package com.tindog.data;

import android.os.Parcel;
import android.os.Parcelable;

public class TinDogUser implements Parcelable {

    public TinDogUser() {}

    public TinDogUser(String uniqueIdentifier) {
        this.uI = uniqueIdentifier;
    }

    protected TinDogUser(Parcel in) {
        nm = in.readString();
        uI = in.readString();
        cp = in.readString();
        em = in.readString();
        aP = in.readString();
        sZ = in.readString();
        rP = in.readString();
        gP = in.readString();
        bP = in.readString();
        iP = in.readString();
        lC = in.readByte() != 0;
    }

    public static final Creator<TinDogUser> CREATOR = new Creator<TinDogUser>() {
        @Override
        public TinDogUser createFromParcel(Parcel in) {
            return new TinDogUser(in);
        }

        @Override
        public TinDogUser[] newArray(int size) {
            return new TinDogUser[size];
        }
    };

    private String nm = ""; //name
    public String getNm() {
        return nm;
    }
    public void setNm(String nm) {
        this.nm = nm;
    }

    private String uI = ""; //unique identifier
    public String getUI() {
        return DatabaseUtilities.cleanIdentifierForFirebase(uI);
    }
    public void setUI(String uI) {
        this.uI = DatabaseUtilities.cleanIdentifierForFirebase(uI);
    }

    private String cp = ""; //cellphone number
    public String getCp() {
        return cp;
    }
    public void setCp(String ce) {
        this.cp = ce;
    }

    private String em = ""; //email
    public String getEm() {
        return em;
    }
    public void setEm(String em) {
        this.em = em;
    }

    private String aP = ""; //age preference
    public String getAP() {
        return aP;
    }
    public void setAP(String aP) {
        this.aP = aP;
    }

    private String sZ = ""; //size preference
    public String getSP() {
        return sZ;
    }
    public void setSP(String sZ) {
        this.sZ = sZ;
    }

    private String rP = ""; //race preference
    public String getRP() {
        return rP;
    }
    public void setRP(String rP) {
        this.rP = rP;
    }

    private String gP = ""; //gender preference
    public String getGP() {
        return gP;
    }
    public void setGP(String gP) {
        this.gP = gP;
    }

    private String bP = ""; //behavior preference
    public String getBP() {
        return bP;
    }
    public void setBP(String bP) {
        this.bP = bP;
    }

    private String iP = ""; //interactions preference
    public String getIP() {
        return iP;
    }
    public void setIP(String iP) {
        this.iP = iP;
    }

    private boolean lC = true; //limit query to country preference
    public boolean getLC() {
        return lC;
    }
    public void setLC(boolean lC) {
        this.lC = lC;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(nm);
        parcel.writeString(uI);
        parcel.writeString(cp);
        parcel.writeString(em);
        parcel.writeString(aP);
        parcel.writeString(sZ);
        parcel.writeString(rP);
        parcel.writeString(gP);
        parcel.writeString(bP);
        parcel.writeString(iP);
        parcel.writeByte((byte) (lC ? 1 : 0));
    }
}
