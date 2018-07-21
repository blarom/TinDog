package com.tindog.resources;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SharedMethods {

    private static final String DEBUG_TAG = "Tindog SharedMethods";
    public static final long MAX_IMAGE_FILE_SIZE = 300; //kb
    public static final int FIREBASE_SIGN_IN_KEY = 123;

    //App parameters
    public static int getSmallestWidth(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.smallestScreenWidthDp;
    }

    //File and Uri utilities
    public static Uri moveFile(Uri source, String destinationDirectory, String destinationFilename) {

        if (source == null) {
            Log.i(DEBUG_TAG, "Tried to move an image with null Uri, aborting.");
            return null;
        }
        if (directoryIsInvalid(destinationDirectory)) {
            Log.i(DEBUG_TAG, "Tried to move an image to an invalid directory, aborting.");
            return null;
        }

        File sourceFile = new File(source.getPath());
        File destinationFileDirectory = new File(destinationDirectory);
        if (!destinationFileDirectory.exists()) destinationFileDirectory.mkdirs();

        File destinationFile = new File(destinationFileDirectory, destinationFilename+".jpg");
        if (destinationFile.exists()) destinationFile.delete(); //Allows replacing files

        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            destinationFile = new File(destinationFileDirectory, destinationFilename+".jpg");

            //if (!destinationFile.exists()) destinationFile.createNewFile();
            outputChannel = new FileOutputStream(destinationFile, false).getChannel();
            inputChannel = new FileInputStream(sourceFile).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            sourceFile.delete();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputChannel != null) inputChannel.close();
                if (outputChannel != null) outputChannel.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return Uri.fromFile(destinationFile);
    }
    public static boolean nameIsInvalid(String imageName) {

        if (TextUtils.isEmpty(imageName)
                || !(imageName.equals("mainImage")
                || imageName.equals("image1")
                || imageName.equals("image2")
                || imageName.equals("image3")
                || imageName.equals("image4")
                || imageName.equals("image5"))){
            Log.i(DEBUG_TAG, "Invalid filename for image in FirebaseDao storage method.");
            return true;
        }
        return false;
    }
    public static void deleteFileAtUri(Uri uri) {
        if (uri==null) {
            Log.i(DEBUG_TAG, "Tried to delete an image with null Uri, aborting.");
            return;
        }
        File fdelete = new File(uri.getPath());
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.i(DEBUG_TAG, "file deleted:" + uri.getPath());
            } else {
                Log.i(DEBUG_TAG, "file not deleted:" + uri.getPath());
            }
        }
    }
    public static void deleteAllLocalObjectImages(Context context, Object object) {
        deleteFileAtUri(getImageUriForObject(context, object, "mainImage"));
        deleteFileAtUri(getImageUriForObject(context, object, "image1"));
        deleteFileAtUri(getImageUriForObject(context, object, "image2"));
        deleteFileAtUri(getImageUriForObject(context, object, "image3"));
        deleteFileAtUri(getImageUriForObject(context, object, "image4"));
        deleteFileAtUri(getImageUriForObject(context, object, "image5"));
    }
    public static String getImagesDirectoryForObject(Context context, Object object) {
        String imageDirectory;
        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            imageDirectory = context.getFilesDir().getAbsolutePath()+"/dogs/"+dog.getUI()+"/images/";
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            imageDirectory = context.getFilesDir().getAbsolutePath()+"/families/"+ family.getUI()+"/images/";
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            imageDirectory = context.getFilesDir().getAbsolutePath()+"/foundations/"+ foundation.getUI()+"/images/";
        }
        else return null;
        return imageDirectory;
    }
    public static Uri getImageUriForObject(Context context, Object object, String imageName) {

        String imageDirectory = getImagesDirectoryForObject(context, object);
        if(directoryIsInvalid(imageDirectory)) return null;

        return SharedMethods.getImageUriWithPath(imageDirectory,imageName);
    }
    public static Uri getImageUriWithPath(String directory, String imageName) {

        if (directoryIsInvalid(directory)) return null;

        File imagesDir = new File(directory);
        if (!imagesDir.exists()) imagesDir.mkdirs();

        File imageFile = getFileWithTrials(directory, imageName+".jpg");
        long length = imageFile.length();
        boolean exists = imageFile.exists();
        if (exists && length>0) {
            return Uri.fromFile(imageFile);
        }
        else return null;
    }
    public static List<Uri> getUrisForExistingImages(String directory, boolean skipMainImage) {

        List<Uri> uris = new ArrayList<>();
        if(directoryIsInvalid(directory)) return uris;
        File imageFile;

        if (!skipMainImage) {
            imageFile = getFileWithTrials(directory, "mainImage.jpg");
            if (imageFile.exists() && imageFile.length()>0) {
                uris.add(Uri.fromFile(imageFile));
            }
        }

        imageFile = getFileWithTrials(directory, "image1.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = getFileWithTrials(directory, "image2.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = getFileWithTrials(directory, "image3.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = getFileWithTrials(directory, "image4.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = getFileWithTrials(directory, "image5.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        return uris;
    }
    private static File getFileWithTrials(String directory, String imageName) {
        File imageFile = new File(directory, imageName);

        //If somehow the the app was not able to get the uri (e.g. sometimes file is not "found"), then try up to 4 more times before giving up
        int tries = 4;
        while (!(imageFile.exists() && imageFile.length()>0) && tries>0) {
            imageFile = new File(directory, imageName);
            tries--;
        }
        return imageFile;
    }
    public static String getNameOfFirstAvailableImageInImagesList(String directory) {

        if (directoryIsInvalid(directory)) return "";

        File imagesDir = new File(directory);
        if (!imagesDir.exists()) imagesDir.mkdirs();

        File image1File = new File(directory, "image1.jpg");
        long length = image1File.length();
        boolean exists = image1File.exists();
        if (!exists || length==0) return "image1";

        File image2File = new File(directory, "image2.jpg");
        length = image2File.length();
        exists = image2File.exists();
        if (!exists || length==0) return "image2";

        File image3File = new File(directory, "image3.jpg");
        length = image3File.length();
        exists = image3File.exists();
        if (!exists || length==0) return "image3";

        File image4File = new File(directory, "image4.jpg");
        length = image4File.length();
        exists = image4File.exists();
        if (!exists || length==0) return "image4";

        File image5File = new File(directory, "image5.jpg");
        length = image5File.length();
        exists = image5File.exists();
        if (!exists || length==0) return "image5";

        return "image1";
    }
    public static boolean shrinkImageWithUri(Context context, Uri uri, int width, int height){

        if (uri==null) return false;

        //inspired by: from: https://stackoverflow.com/questions/16954109/reduce-the-size-of-a-bitmap-to-a-specified-size-in-android

        //If the image is already small, don't change it (file.length()==0 means the image wasn't found)
        File file = new File(uri.getPath());
        while (file.length()/1024 > SharedMethods.MAX_IMAGE_FILE_SIZE) {
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = false;
            Bitmap bitmap;

            int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) height);
            int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) width);

            if (heightRatio > 1 || widthRatio > 1) {
                if (heightRatio > widthRatio) {
                    bmpFactoryOptions.inSampleSize = heightRatio;
                } else {
                    bmpFactoryOptions.inSampleSize = widthRatio;
                }
            }

            bmpFactoryOptions.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(uri.toString(), bmpFactoryOptions);

            if (bitmap==null) {
                //TODO: fix decoding of large images
                Toast.makeText(context, "The image you chose is too large, try a smaller image.", Toast.LENGTH_SHORT).show();
                return false;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageInByte = stream.toByteArray();

            //this gives the size of the compressed image in kb
            long lengthbmp = imageInByte.length / 1024;

            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(uri.toString()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            height = (int) Math.ceil(height * 0.75);
            height = (int) Math.ceil(height * 0.75);
            file = new File(uri.getPath());
        }
        return true;

    }
    public static Uri updateImageInLocalDirectory(Uri originalImageUri, String directory, String imageName) {

        if (directoryIsInvalid(directory)) return null;
        Uri copiedImageUri = moveFile(originalImageUri, directory, imageName);
        return copiedImageUri;
    }
    public static void synchronizeImageOnAllDevices(Object object, FirebaseDao firebaseDao, String localDirectory, String imageName, Uri downloadedImageUri) {

        if (directoryIsInvalid(localDirectory)) return;

        //The image was downloaded only if it was newer than the local image (If it wasn't downloaded, the downloadedImageUri is the same as the local image Uri)
        Uri localImageUri = SharedMethods.getImageUriWithPath(localDirectory, imageName);

        if (downloadedImageUri != null) {
            if (localImageUri == null) {
                SharedMethods.updateImageInLocalDirectory(downloadedImageUri, localDirectory, imageName);
            }
            else {
                String localUriPath = localImageUri.getPath();
                String downloadedUriPath = downloadedImageUri.getPath();

                //If the downloaded image is newer, then update the image in the local directory
                if (!downloadedUriPath.equals(localUriPath)) {
                    SharedMethods.updateImageInLocalDirectory(downloadedImageUri, localDirectory, imageName);
                }

                //If the local image is newer, then upload it to Firebase to replace the older image
                else if (downloadedUriPath.equals(localUriPath)) {
                    firebaseDao.putImageInFirebaseStorage(object, localImageUri, imageName);
                }
            }
        }
        else {
            if (localImageUri != null) {
                firebaseDao.putImageInFirebaseStorage(object, localImageUri, imageName);
            }
        }
    }
    private static boolean directoryIsInvalid(String localDirectory) {
        return (TextUtils.isEmpty((localDirectory)) || localDirectory.contains("//"));
    }
    public static void synchronizeAllObjectImagesOnAllDevices(Object object, FirebaseDao firebaseDao, String localDirectory, Uri downloadedImageUri) {
        synchronizeImageOnAllDevices(object, firebaseDao, localDirectory, "mainImage", downloadedImageUri);
        synchronizeImageOnAllDevices(object, firebaseDao, localDirectory, "image1", downloadedImageUri);
        synchronizeImageOnAllDevices(object, firebaseDao, localDirectory, "image2", downloadedImageUri);
        synchronizeImageOnAllDevices(object, firebaseDao, localDirectory, "image3", downloadedImageUri);
        synchronizeImageOnAllDevices(object, firebaseDao, localDirectory, "image4", downloadedImageUri);
        synchronizeImageOnAllDevices(object, firebaseDao, localDirectory, "image5", downloadedImageUri);
    }

    //UI utilities
    public static void displayImages(Context context, String localDirectory, String imageName, ImageView imageViewMain, ImagesRecycleViewAdapter imagesRecycleViewAdapter) {

        if (directoryIsInvalid(localDirectory)) {
            Log.i(DEBUG_TAG, "Tried to access an invalid directory, aborting.");
            return;
        }

        if (imageName.equals("mainImage")) {
            Uri localImageUri = SharedMethods.getImageUriWithPath(localDirectory, imageName);
            if (localImageUri!=null) refreshMainImageShownToUser(context, localDirectory, imageViewMain);
        }
        else {
            if (imagesRecycleViewAdapter!=null) refreshImagesListShownToUser(localDirectory, imagesRecycleViewAdapter);
        }
    }
    public static void displayObjectImageInImageView(Context context, Object object, String imageName, ImageView imageView) {

        String localDirectory = getImagesDirectoryForObject(context, object);
        if (directoryIsInvalid(localDirectory)) {
            Log.i(DEBUG_TAG, "Tried to access an invalid directory, aborting.");
            return;
        }
        Uri localImageUri = SharedMethods.getImageUriWithPath(localDirectory, imageName);
        displayUriInImageView(context, localImageUri, imageView);
    }
    public static void displayUriInImageView(Context context, Uri uri, ImageView imageView) {
        if (uri!=null) {
            Picasso.with(context)
                    .load(uri.toString())
                    .error(R.drawable.ic_image_not_available)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .into(imageView);
        }
        else {
            loadGenericAppImageIntoImageView(context, imageView);
        }
    }
    public static void refreshMainImageShownToUser(Context context, String directory, ImageView imageViewMain) {
        if (directoryIsInvalid(directory)) return;

        Uri mainImageUri = SharedMethods.getImageUriWithPath(directory,"mainImage");
        if (imageViewMain==null) return;
        Picasso.with(context)
                .load(mainImageUri)
                .error(R.drawable.ic_image_not_available)
                .memoryPolicy(MemoryPolicy.NO_CACHE) //This is required here, since picasso doesn't detect that an image with identical path has changed
                .into(imageViewMain);
    }
    public static void refreshImagesListShownToUser(String directory, ImagesRecycleViewAdapter imagesRecycleViewAdapter) {
        if (directoryIsInvalid(directory)) return;

        List<Uri> uris = SharedMethods.getUrisForExistingImages(directory, true);
        imagesRecycleViewAdapter.setContents(uris);
    }
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getBaseContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
    public static int getImagesRecyclerViewPosition(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
        return layoutManager.findFirstVisibleItemPosition();
    }
    public static int getSpinnerPositionFromText(Context context, Spinner spinnerAdapter, String userSelection) {

        String[] spinnerArrayElements;
        switch (spinnerAdapter.getId()) {
            case R.id.preferences_age_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_age);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_size_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_size);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_gender_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_gender);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_race_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_race);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_behavior_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_behavior);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_interactions_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_interactions);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
        }

        return 0;
    }
    public static void showSignInScreen(Activity activity) {

        SharedMethods.setAppPreferenceSignInRequestState(activity, false);

        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        activity.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                SharedMethods.FIREBASE_SIGN_IN_KEY);
    }
    public static void loadGenericAppImageIntoImageView(Context context, ImageView image) {

        Uri imageUri = Uri.fromFile(new File("//android_asset/splashscreen_intro_image.jpg"));
        Picasso.with(context)
                .load(imageUri)
                .error(R.drawable.ic_image_not_available)
                .into(image);
    }

    //Location utlities
    public static Address getAddressFromCity(Context context, String location) {

        //inspired by: https://stackoverflow.com/questions/20166328/how-to-get-longitude-latitude-from-the-city-name-android-code

        List<Address> addresses = new ArrayList<>();
        if(Geocoder.isPresent() && !TextUtils.isEmpty(location)){
            try {
                Geocoder gc = new Geocoder(context);
                addresses = gc.getFromLocationName(location, 5); // get the found Address Objects

//                List<LatLng> latLong = new ArrayList<>(addresses.size()); // A list to save the coordinates if they are available
//                for (Address address : addresses){
//                    if(address.hasLatitude() && address.hasLongitude()){
//                        latLong.add(new LatLng(address.getLatitude(), address.getLongitude()));
//                    }
//                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (addresses.size()>0) return addresses.get(0);
        else return null;
    }
    public static String getCityFromLocation(Context context, double latitude, double longitude) {

        Geocoder gcd = new Geocoder(context, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                return addresses.get(0).getLocality();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    //Internet utilities
    public static boolean internetIsAvailable(Context context) {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) return false;

        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

        return activeNetworkInfo != null;
    }
    public static void goToWebLink(Context context, String url) {

        if (context==null) return;

        //Prepare the website
        if (!TextUtils.isEmpty(url)) {
            if (url.length()>8 && url.substring(0,8).equals("https://")
                    || (url.length()>7 && url.substring(0,7).equals("http://"))) {
                //Website is valid, do nothing.
            }
            else if (url.length()>6 && url.substring(0,6).equals("ftp://")) {
                Toast.makeText(context,"Sorry, we cannot open ftp links in this app", Toast.LENGTH_SHORT).show();
            }
            else if (url.length()>7 && url.substring(0,7).equals("smtp://")) {
                Toast.makeText(context,"Sorry, we cannot open smtp links in this app", Toast.LENGTH_SHORT).show();
            }
            else {
                url = "http://" + url;
            }
        }

        //Try accessing the website. If the website is still not formatted correctly (ie. gibberish), then fail silently
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Preferences
    public static void setAppPreferenceSignInRequestState(Context context, boolean requestedSignInState) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(context.getString(R.string.app_preference_sign_in_state), requestedSignInState);
            editor.apply();
        }
    }
    public static boolean getAppPreferenceSignInRequestState(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(context.getString(R.string.app_preference_sign_in_state), true);
    }
    public static void setAppPreferenceUserLongitude(Context context, double longitude) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(context.getString(R.string.user_longitude), Double.toString(longitude));
            editor.apply();
        }
    }
    public static Double getAppPreferenceUserLongitude(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
        return Double.parseDouble(sharedPref.getString(context.getString(R.string.user_longitude), "0.0"));
    }
    public static void setAppPreferenceUserLatitude(Context context, double latitude) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(context.getString(R.string.user_latitude), Double.toString(latitude));
            editor.apply();
        }
    }
    public static Double getAppPreferenceUserLatitude(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
        return Double.parseDouble(sharedPref.getString(context.getString(R.string.user_latitude), "0.0"));
    }
    public static void setAppPreferenceFirstTimeUsingApp(Context context, boolean firstTimeFlag) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(context.getString(R.string.first_time_using_app), firstTimeFlag);
            editor.apply();
        }
    }
    public static boolean getAppPreferenceFirstTimeUsingApp(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(context.getString(R.string.first_time_using_app), true);
    }

}
