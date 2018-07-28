package com.tindog.ui;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import com.tindog.R;
import com.tindog.services.WidgetUpdateJobIntentService;
import com.tindog.data.Dog;
import com.tindog.resources.Utilities;

import java.io.IOException;

/**
 * Implementation of App Widget functionality.
 */
public class DogImageWidgetProvider extends AppWidgetProvider {

    public static void updateDogImageWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Dog dog) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, dog);
        }
    }
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Dog dog) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_dog_image);
        views.setTextViewText(R.id.widget_dog_name, widgetText);

        //Prepare the widget layout
        //Dog dog  = SharedMethods.getRandomDog(context);
        updateWidgetDogImage(context, views, dog);
        updateWidgetDogName(context, views, dog);
        setWidgetImageClickListener(context, views, dog);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    private static void updateWidgetDogImage(Context context, RemoteViews views, Dog dog) {
        if (dog != null) {

            Uri uri = Utilities.getImageUriForObjectWithFileProvider(context, dog, "mainImage");

            if (uri!=null) {
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_image_not_available);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                views.setImageViewBitmap(R.id.widget_dog_image_container, bitmap);
            }
            else views.setImageViewResource(R.id.widget_dog_image_container, R.drawable.ic_image_not_available);
        }
        else {
            views.setImageViewResource(R.id.widget_dog_image_container, R.drawable.ic_image_not_available);
        }
    }
    private static void updateWidgetDogName(Context context, RemoteViews views, Dog dog) {
        if (dog != null) {
            views.setTextViewText(R.id.widget_dog_name, dog.getNm());
        }
        else {
            views.setTextViewText(R.id.widget_dog_name, context.getString(R.string.no_name_available));
        }
    }
    private static void setWidgetImageClickListener(Context context, RemoteViews views, Dog dog) {
        Intent intent = new Intent(context, SearchResultsActivity.class);
        intent.putExtra(context.getString(R.string.profile_type), context.getString(R.string.dog_profile));
        intent.putExtra(context.getString(R.string.requested_specific_dog_profile), dog.getUI());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Note: FLAG_UPDATE_CURRENT makes sure that the updated extras are send with the pendingIntent, instead of using old ones
        views.setOnClickPendingIntent(R.id.widget_dog_image_container, pendingIntent);

    }
    private static void startWidgetUpdateService(Context context) {
        Intent intent = new Intent(context, WidgetUpdateJobIntentService.class);
        intent.setAction(context.getString(R.string.action_update_widget_random_dog));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        WidgetUpdateJobIntentService.enqueueWork(context, intent);
    }

    //Widget Provider methods
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        startWidgetUpdateService(context);

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }
    @Override public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        startWidgetUpdateService(context);
    }
    @Override public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }
    @Override public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

