package com.tindog;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.tindog.data.Dog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WidgetUpdateJobIntentService extends JobIntentService {

    static final int JOB_ID_FOR_SERVICE = 2118;
    private List<Dog> mDogsList = new ArrayList<>();
    private Dog mSpecificDog = new Dog();


    public WidgetUpdateJobIntentService() {
        super();
    }


    //Service methods
    @Override protected void onHandleWork(@NonNull Intent intent) {

        if (intent.hasExtra(getString(R.string.intent_extra_specific_dog))) {
            mSpecificDog = intent.getParcelableExtra(getString(R.string.intent_extra_specific_dog));
        }
        if (intent.hasExtra(getString(R.string.intent_extra_dogs_list))) {
            mDogsList = intent.getParcelableArrayListExtra(getString(R.string.intent_extra_dogs_list));
        }

        final String action = intent.getAction();
        if (action != null && action.equals(getString(R.string.action_update_widget_random_dog))) {

            //Get the widget parameters
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, DogImageWidgetProvider.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_image_container);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_name);

            //Get the new dog parameters
            if (mDogsList!=null && mDogsList.size()>0) {
                Random rand = new Random();
                int randomIndex = rand.nextInt(mDogsList.size());

                //Update the widget
                DogImageWidgetProvider.updateDogImageWidgets(this, appWidgetManager, appWidgetIds, mDogsList.get(randomIndex));
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_image_container);
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_name);
            }

        }
        if (action != null && action.equals(getString(R.string.action_update_widget_specific_dog))) {

            //Get the widget parameters
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, DogImageWidgetProvider.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_image_container);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_name);

            //Update the widget
            DogImageWidgetProvider.updateDogImageWidgets(this, appWidgetManager, appWidgetIds, mSpecificDog);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_image_container);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_dog_name);

        }
    }

    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WidgetUpdateJobIntentService.class, JOB_ID_FOR_SERVICE, work);
    }


}
