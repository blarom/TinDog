package com.tindog.data;

public class TinDogUser {

    public TinDogUser() {}

    public TinDogUser(String uniqueIdentifier) {
        this.uI = uniqueIdentifier;
    }

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
}
