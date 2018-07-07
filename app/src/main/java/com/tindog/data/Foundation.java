package com.tindog.data;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;

public class Foundation {

    public Foundation() { }

    Foundation(String name, String city, String country) {
        this.name = name;
        this.city = city;
        this.country = country;
    }

    public Foundation(String ownerfirebaseUid) {
        this.ownerfirebaseUid = ownerfirebaseUid;
        setUniqueIdentifierFromDetails();
    }

    private String name = "No name available";
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private String ownerfirebaseUid;
    public String getOwnerfirebaseUid() {
        return ownerfirebaseUid;
    }
    public void setOwnerfirebaseUid(String ownerfirebaseUid) {
        this.ownerfirebaseUid = ownerfirebaseUid;
        setUniqueIdentifierFromDetails();
    }

    private String uniqueIdentifier = "";
    public String getUniqueIdentifier() {
        return DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }
    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }
    public void setUniqueIdentifierFromDetails() {
        if (TextUtils.isEmpty(ownerfirebaseUid)) uniqueIdentifier = name + "-" + city + "-" + country;
        else uniqueIdentifier = ownerfirebaseUid;
        uniqueIdentifier = DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }

    private String website = "www.google.com";
    public String getWebsite() {
        return website;
    }
    public void setWebsite(String name) {
        this.website = website;
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

    private String streetNumber = "No street number available";
    public String getStreetNumber() {
        return streetNumber;
    }
    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    private String contactPhone = "No contact phone available";
    public String getContactPhone() {
        return contactPhone;
    }
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    private String contactEmail = "No contact email available";
    public String getContactEmail() {
        return contactEmail;
    }
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    private List<String> imageUploadTimes = Arrays.asList("","","","","","");
    public List<String> getImageUploadTimes() {
        return imageUploadTimes;
    }
    public void setImageUploadTimes(List<String> imageUploadTimes) {
        this.imageUploadTimes = imageUploadTimes;
    }

}
