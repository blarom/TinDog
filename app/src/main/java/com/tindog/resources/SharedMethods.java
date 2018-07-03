package com.tindog.resources;

import android.content.Context;
import android.content.res.Configuration;

public class SharedMethods {

    public static final String PROFILE_UPDATE_PET_IMAGES_RV_POSITION = "profile_update_pet_images_rv_position";
    public static final String PROFILE_UPDATE_IMAGE_NAME = "profile_update_image_name";
    public static final long MAX_IMAGE_FILE_SIZE = 300; //kb
    public static final int FIREBASE_SIGN_IN = 123;
    public static final String CHOSEN_ACTION_KEY = "chosen_activity_key";

    public static int getSmallestWidth(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.smallestScreenWidthDp;
    }
}
