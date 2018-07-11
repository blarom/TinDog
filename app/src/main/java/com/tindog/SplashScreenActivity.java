package com.tindog;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.tindog.data.DatabaseUtilities;
import com.tindog.data.FirebaseDao;

import java.io.File;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 1500; //Miliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //Setting up Firebase
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        firebaseDb.setPersistenceEnabled(true);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        //firebaseAuth.signInWithEmailAndPassword(DatabaseUtilities.firebaseEmail, DatabaseUtilities.firebasePass);

        FirebaseDao firebaseDao = new FirebaseDao(this, null);
        firebaseDao.populateFirebaseDbWithDummyData();

        //Setting up the delay before reaching the next activity
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {

                Intent intent = new Intent(SplashScreenActivity.this, TaskSelectionActivity.class);

                SplashScreenActivity.this.overridePendingTransition(0, 0);
                SplashScreenActivity.this.startActivity(intent);
                SplashScreenActivity.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

}
