package com.tindog.data;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseAppClass extends android.app.Application {
    //Inpired by: https://stackoverflow.com/questions/37753991/com-google-firebase-database-databaseexception-calls-to-setpersistenceenabled
    @Override public void onCreate() {
        super.onCreate();
        /* Enable disk persistence  */
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
