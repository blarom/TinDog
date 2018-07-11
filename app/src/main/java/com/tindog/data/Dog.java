package com.tindog.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Dog {

    public Dog() {}

    Dog(String name, String gender, String race, String city, String country, String age) {
        this.name = name;
        this.gender = gender;
        this.race = race;
        this.city = city;
        this.country = country;
        this.age = age;
    }

    private String name = "";
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private String associatedFoundationContactPhone;
    public String getAssociatedFoundationContactPhone() {
        return associatedFoundationContactPhone;
    }
    public void setAssociatedFoundationContactPhone(String associatedFoundationContactPhone) {
        this.associatedFoundationContactPhone = associatedFoundationContactPhone;
    }

    private String uniqueIdentifier = "";
    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }
    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    private String associatedFoundationUniqueId = "";
    public String getAssociatedFoundationUniqueId() {
        return associatedFoundationUniqueId;
    }
    public void setAssociatedFoundationUniqueId(String associatedFoundationUniqueId) {
        this.associatedFoundationUniqueId = associatedFoundationUniqueId;
    }

    private String associatedFoundationName = "";
    public String getAssociatedFoundationName() {
        return associatedFoundationName;
    }
    public void setAssociatedFoundationName(String associatedFoundationName) {
        this.associatedFoundationName = associatedFoundationName;
    }

    private String country;
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }

    private String city;
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    private List<String> videoUrls = new ArrayList<>();
    public List<String> getVideoUrls() {
        return videoUrls;
    }
    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
    }

    private String age  = "No age available";
    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
    }

    private String size  = "No size available";
    public String getSize() {
        return size;
    }
    public void setSize(String size) {
        this.size = size;
    }

    private String race = "No race available";
    public String getRace() {
        return race;
    }
    public void setRace(String race) {
        this.race = race;
    }

    private String gender  = "No gender available";
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }

    private String behavior  = "No behavior available";
    public String getBehavior() {
        return behavior;
    }
    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }

    private String interactions  = "No interactions available";
    public String getInteractions() {
        return interactions;
    }
    public void setInteractions(String interactions) {
        this.interactions = interactions;
    }

    private String history  = "No history available";
    public String getHistory() {
        return history;
    }
    public void setHistory(String history) {
        this.history = history;
    }

    private List<String> imageUploadTimes = Arrays.asList("","","","","","");
    public List<String> getImageUploadTimes() {
        return imageUploadTimes;
    }
    public void setImageUploadTimes(List<String> imageUploadTimes) {
        this.imageUploadTimes = imageUploadTimes;
    }

}
