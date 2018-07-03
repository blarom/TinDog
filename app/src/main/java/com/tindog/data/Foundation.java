package com.tindog.data;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class Foundation {

    public Foundation() { }

    Foundation(String name, String city, String country) {
        this.name = name;
        this.city = city;
        this.country = country;
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

    private String mainImageUrl = "No main image available";
    public String getMainImageUrl() {
        return mainImageUrl;
    }
    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    private String address = "No address available";
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    private String contactDetails = "No contact details available";
    public String getContactDetails() {
        return contactDetails;
    }
    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    private List<String> imageUrls = new ArrayList<>();
    public List<String> getImageUrls() {
        return imageUrls;
    }
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
