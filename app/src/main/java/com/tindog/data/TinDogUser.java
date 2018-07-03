package com.tindog.data;

public class TinDogUser {

    public TinDogUser() {}

    public TinDogUser(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    private String name = "No name available";
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private String uniqueIdentifier = "";
    public String getUniqueIdentifier() {
        return DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }
    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = DatabaseUtilities.cleanIdentifierForFirebase(uniqueIdentifier);
    }

    private String cell = "000";
    public String getCell() {
        return cell;
    }
    public void setCell(String cell) {
        this.cell = cell;
    }

    private String email = "default@default.com";
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    private String agePref = "No preferred age available";
    public String getAgePref() {
        return agePref;
    }
    public void setAgePref(String agePref) {
        this.agePref = agePref;
    }

    private String sizePref = "No preferred size available";
    public String getSizePref() {
        return sizePref;
    }
    public void setSizePref(String sizePref) {
        this.sizePref = sizePref;
    }

    private String racePref = "No preferred race available";
    public String getRacePref() {
        return racePref;
    }
    public void setRacePref(String racePref) {
        this.racePref = racePref;
    }

    private String genderPref = "No preferred gender available";
    public String getGenderPref() {
        return genderPref;
    }
    public void setGenderPref(String genderPref) {
        this.genderPref = genderPref;
    }

    private String behaviorPref = "No preferred behavior available";
    public String getBehaviorPref() {
        return behaviorPref;
    }
    public void setBehaviorPref(String behaviorPref) {
        this.behaviorPref = behaviorPref;
    }

    private String interactionsPref = "No preferred interactions available";
    public String getInteractionsPref() {
        return interactionsPref;
    }
    public void setInteractionsPref(String interactionsPref) {
        this.interactionsPref = interactionsPref;
    }
}
