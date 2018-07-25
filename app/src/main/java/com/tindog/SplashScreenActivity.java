package com.tindog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.tindog.data.FirebaseDao;
import com.tindog.resources.Utilities;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 1500; //Miliseconds
    private static final String DEBUG_TAG = "TinDog SplashScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //Setting up Firebase
        //FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        //firebaseDb.setPersistenceEnabled(true); //See com.tindog.data/FirebaseAppClass
        //FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        //firebaseAuth.signInWithEmailAndPassword(DatabaseUtilities.firebaseEmail, DatabaseUtilities.firebasePass);

        //FirebaseDao firebaseDao = new FirebaseDao(this, null);
        //firebaseDao.populateFirebaseDbWithDummyData();

        //Loading the splashscreen image
        ImageView image = findViewById(R.id.splashscreen_image);
        Utilities.loadGenericAppImageIntoImageView(this, image);

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

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.tindog_notification_channel);
            String description = getString(R.string.notification_channel_show_progress);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.tindog_notification_channel), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager!=null) notificationManager.createNotificationChannel(channel);
            else Log.i(DEBUG_TAG, "Failed to create notification channel in SplashScreen");
        }
    }

}
