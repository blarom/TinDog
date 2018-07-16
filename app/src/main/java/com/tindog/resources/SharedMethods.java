package com.tindog.resources;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.FirebaseDao;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SharedMethods {

    public static final long MAX_IMAGE_FILE_SIZE = 300; //kb
    public static final int FIREBASE_SIGN_IN_KEY = 123;

    //App parameters
    public static int getSmallestWidth(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.smallestScreenWidthDp;
    }

    //File and Uri utilities
    public static Uri moveFile(Uri source, String destinationDirectory, String destinationFilename) {

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
    public static boolean deleteFileAtUri(Uri uri) {
        File fdelete = new File(uri.getPath());
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                return true;
                //System.out.println("file Deleted :" + uri.getPath());
            } else {
                return false;
                //System.out.println("file not Deleted :" + uri.getPath());
            }
        }
        return false;
    }
    public static Uri getUriForImage(String directory, String imageName) {

        File imagesDir = new File(directory);
        if (!imagesDir.exists()) imagesDir.mkdirs();

        File imageFile = new File(directory, imageName+".jpg");
        long length = imageFile.length();
        boolean exists = imageFile.exists();
        if (exists && length>0) {
            return Uri.fromFile(imageFile);
        }
        else return null;
    }
    public static void updateImagesFromFirebaseIfRelevant(Object object, FirebaseDao firebaseDao) {
        firebaseDao.getImageFromFirebaseStorage(object, "mainImage");
        firebaseDao.getImageFromFirebaseStorage(object, "image1");
        firebaseDao.getImageFromFirebaseStorage(object, "image2");
        firebaseDao.getImageFromFirebaseStorage(object, "image3");
        firebaseDao.getImageFromFirebaseStorage(object, "image4");
        firebaseDao.getImageFromFirebaseStorage(object, "image5");
    }
    public static void updateImageFromFirebaseIfRelevant(Object object, String imageName, FirebaseDao firebaseDao) {
        firebaseDao.getImageFromFirebaseStorage(object, imageName);
    }
    public static List<Uri> getExistingImageUris(String directory, boolean skipMainImage) {
        List<Uri> uris = new ArrayList<>();
        File imageFile;

        if (!skipMainImage) {
            imageFile = new File(directory, "mainImage.jpg");
            if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));
        }

        imageFile = new File(directory, "image1.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = new File(directory, "image2.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = new File(directory, "image3.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = new File(directory, "image4.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        imageFile = new File(directory, "image5.jpg");
        if (imageFile.exists() && imageFile.length()>0) uris.add(Uri.fromFile(imageFile));

        return uris;
    }
    public static String getNameOfFirstAvailableImageInImagesList(String directory) {

        File imagesDir = new File(directory);
        if (!imagesDir.exists()) imagesDir.mkdirs();

        File image1File = new File(directory, "image1.jpg");
        long length = image1File.length();
        boolean exists = image1File.exists();
        if (!image1File.exists() || (image1File.exists() && image1File.length()==0)) {
            return "image1";
        }

        File image2File = new File(directory, "image2.jpg");
        length = image2File.length();
        exists = image2File.exists();
        if (!image2File.exists() || (image2File.exists() && image2File.length()==0)) {
            return "image2";
        }

        File image3File = new File(directory, "image3.jpg");
        length = image3File.length();
        exists = image3File.exists();
        if (!image3File.exists() || (image3File.exists() && image3File.length()==0)) {
            return "image3";
        }

        File image4File = new File(directory, "image4.jpg");
        length = image4File.length();
        exists = image4File.exists();
        if (!image4File.exists() || (image4File.exists() && image4File.length()==0)) {
            return "image4";
        }

        File image5File = new File(directory, "image5.jpg");
        length = image5File.length();
        exists = image5File.exists();
        if (!image5File.exists() || (image5File.exists() && image5File.length()==0)) {
            return "image5";
        }

        return "image1";
    }
    public static boolean shrinkImageWithUri(Context context, Uri uri, int width, int height){

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

        Uri copiedImageUri = moveFile(originalImageUri, directory, imageName);
        return copiedImageUri;
    }
    public static void synchronizeImageOnAllDevices(Object object, FirebaseDao firebaseDao, String localDirectory, String imageName, Uri downloadedImageUri) {

        //The image was downloaded only if it was newer than the local image (If it wasn't downloaded, the downloadedImageUri is the same as the local image Uri)
        Uri localImageUri = SharedMethods.getUriForImage(localDirectory, imageName);

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

        if (imageName.equals("mainImage")) {
            Uri localImageUri = SharedMethods.getUriForImage(localDirectory, imageName);
            if (localImageUri!=null) refreshMainImageShownToUser(context, localDirectory, imageViewMain);
        }
        else refreshImagesListShownToUser(localDirectory, imagesRecycleViewAdapter);
    }
    public static void refreshMainImageShownToUser(Context context, String directory, ImageView imageViewMain) {
        Uri mainImageUri = SharedMethods.getUriForImage(directory,"mainImage");
        if (imageViewMain==null) return;
        Picasso.with(context)
                .load(mainImageUri)
                .error(R.drawable.ic_image_not_available)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .into(imageViewMain);
    }
    public static void refreshImagesListShownToUser(String directory, ImagesRecycleViewAdapter imagesRecycleViewAdapter) {
        imagesRecycleViewAdapter.setContents(SharedMethods.getExistingImageUris(directory, true));
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
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_age_simple);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_size_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_size_simple);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_gender_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_gender_simple);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_race_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_race_simple);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_behavior_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_behavior_simple);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
            case R.id.preferences_interactions_spinner:
                spinnerArrayElements = context.getResources().getStringArray(R.array.dog_interactions_simple);
                for (int i=0; i<spinnerArrayElements.length; i++) {
                    if (userSelection.equals(spinnerArrayElements[i])) return i;
                }
                break;
        }

        return 0;
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
}
