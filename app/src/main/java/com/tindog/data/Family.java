package com.tindog.data;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class Family {

    public Family() {

    }

    public Family(String ownerfirebaseUid) {
        this.ownerfirebaseUid = ownerfirebaseUid;
    }

    private String ownerfirebaseUid;
    public String getOwnerfirebaseUid() {
        return ownerfirebaseUid;
    }
    public void setOwnerfirebaseUid(String ownerfirebaseUid) {
        this.ownerfirebaseUid = ownerfirebaseUid;
    }

    private String pseudonym = "No name available";
    public String getPseudonym() {
        return pseudonym;
    }
    public void setPseudonym(String pseudonym) {
        this.pseudonym = pseudonym;
    }

    private String uniqueIdentifier = "";
    public String getUniqueIdentifier() {
        return DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }
    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }
    public void setUniqueIdentifierFromDetails() {
        if (TextUtils.isEmpty(ownerfirebaseUid)) uniqueIdentifier = pseudonym + "-" + email;
        else uniqueIdentifier = ownerfirebaseUid;
        uniqueIdentifier = DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }

    private String mainImageUri = "No main image available";
    public String getMainImageUri() {
        return mainImageUri;
    }
    public void setMainImageUri(String mainImageUri) {
        this.mainImageUri = mainImageUri;
    }

    private String email = "default@default.com";
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    private String cell = "000";
    public String getCell() {
        return cell;
    }
    public void setCell(String cell) {
        this.cell = cell;
    }

    private String country = "No country available";
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }

    private String city = "No city available";
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    private String street = "No street available";
    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }

    private String experience = "No experience available";
    public String getExperience() {
        return experience;
    }
    public void setExperience(String experience) {
        this.experience = experience;
    }

    private List<String> helpOffer = new ArrayList<>();
    public List<String> getHelpOffer() {
        return helpOffer;
    }
    public void setHelpOffer(List<String> helpOffer) {
        this.helpOffer = helpOffer;
    }

    private List<String> imageUris = initializeImageUris();
    private List<String> initializeImageUris() {
        List<String> uris = new ArrayList<>();
        for (int i=0; i<5; i++) {
            uris.add("");
        }
        return uris;
    }
    public List<String> getImageUris() {
        return imageUris;
    }
    public void setImageUris(List<String> imageUris) {
        this.imageUris = imageUris;
    }
}