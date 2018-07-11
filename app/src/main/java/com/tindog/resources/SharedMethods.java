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

public class SharedMethods {

    public static final String PROFILE_UPDATE_PET_IMAGES_RV_POSITION = "profile_update_pet_images_rv_position";
    public static final String PROFILE_UPDATE_IMAGE_NAME = "profile_update_image_name";
    public static final long MAX_IMAGE_FILE_SIZE = 300; //kb
    public static final int FIREBASE_SIGN_IN = 123;
    public static final String CHOSEN_ACTION_KEY = "chosen_activity_key";
    public static final String DOG_ID = "dog_id";

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

        long length = destinationFile.length();
        boolean exists = destinationFile.exists();

        if (destinationFile.exists()) destinationFile.delete(); //Allows replacing files

        length = destinationFile.length();
        exists = destinationFile.exists();

        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {

            destinationFile = new File(destinationFileDirectory, destinationFilename+".jpg");
            length = destinationFile.length();
            exists = destinationFile.exists();

//            if (destinationFile.exists()) {
//                deleted = destinationFile.delete(); //Allows replacing files
//                FileWriter fw = new FileWriter(destinationDirectory + "/"+destinationFilename+".jpg", false);
//            }

            //if (!destinationFile.exists()) destinationFile.createNewFile();
            outputChannel = new FileOutputStream(destinationFile, false).getChannel();
            inputChannel = new FileInputStream(sourceFile).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            sourceFile.delete();

            length = destinationFile.length();
            exists = destinationFile.exists();
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
    public static List<Uri> getExistingImageUris(String directory) {
        List<Uri> uris = new ArrayList<>();
        File imageFile;

        //Skipping the mainImage since we want to show only the remaining images
        //imageFile = new File(imageDirectory, "mainImage");
        //if (imageFile.exists()) uris.add(Uri.fromFile(imageFile));

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
    public static Uri updateImageInLocalDirectoryAndShowIt(Context context,
                                                           Uri originalImageUri,
                                                           String directory,
                                                           String imageName,
                                                           ImageView imageViewMain,
                                                           ImagesRecycleViewAdapter imagesRecycleViewAdapter) {

        Uri copiedImageUri = moveFile(originalImageUri, directory, imageName);
        //SharedMethods.deleteFileAtUri(croppedImageTempUri);

        refreshImage(context, directory, imageName, imageViewMain, imagesRecycleViewAdapter);

        return copiedImageUri;
    }
    public static void synchronizeImageOnAllDevices(Context context,
                                                    Object object,
                                                    FirebaseDao firebaseDao,
                                                    String localDirectory,
                                                    String imageName,
                                                    Uri downloadedImageUri,
                                                    ImageView imageViewMain,
                                                    ImagesRecycleViewAdapter imagesRecycleViewAdapter) {

        //The image was downloaded only if it was newer than the local image (If it wasn't downloaded, the downloadedImageUri is the same as the local image Uri)
        Uri localImageUri = SharedMethods.getUriForImage(localDirectory, imageName);

        if (localImageUri==null && downloadedImageUri==null) {
            //Do nothing
        }

        if (localImageUri!=null && downloadedImageUri==null) {
            refreshImage(context, localDirectory, imageName, imageViewMain, imagesRecycleViewAdapter);
        }

        else if (localImageUri==null && downloadedImageUri!=null) {
            SharedMethods.updateImageInLocalDirectoryAndShowIt(context,
                    downloadedImageUri, localDirectory, imageName, imageViewMain, imagesRecycleViewAdapter);
        }

        else if (localImageUri!=null && downloadedImageUri!=null) {

            String localUriPath = localImageUri.getPath();
            String downloadeUriPath = downloadedImageUri.getPath();

            //If the downloaded image is newer, then update the image in the local directory
            if (!downloadeUriPath.equals(localUriPath)) {
                SharedMethods.updateImageInLocalDirectoryAndShowIt(context,
                        downloadedImageUri, localDirectory, imageName, imageViewMain, imagesRecycleViewAdapter);
            }

            //If the local image is newer, then upload it to Firebase to replace the older image
            else if (downloadeUriPath.equals(localUriPath)) {
                firebaseDao.putImageInFirebaseStorage(object, localImageUri, imageName);
            }
        }


    }

    //UI utilities
    private static void refreshImage(Context context, String directory, String imageName, ImageView imageViewMain, ImagesRecycleViewAdapter imagesRecycleViewAdapter) {

        if (imageName.equals("mainImage")) refreshMainImageShownToUser(context, directory, imageViewMain);
        else refreshImagesListShownToUser(directory, imagesRecycleViewAdapter);

    }
    public static void refreshMainImageShownToUser(Context context, String directory, ImageView imageViewMain) {
        Uri mainImageUri = SharedMethods.getUriForImage(directory,"mainImage");
        Picasso.with(context)
                .load(mainImageUri)
                .error(R.drawable.ic_image_not_available)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .into(imageViewMain);
    }
    public static void refreshImagesListShownToUser(String directory, ImagesRecycleViewAdapter imagesRecycleViewAdapter) {
        imagesRecycleViewAdapter.setContents(SharedMethods.getExistingImageUris(directory));
    }
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getBaseContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    //Functional utlities
    public static int getImagesRecyclerViewPosition(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
        return layoutManager.findFirstVisibleItemPosition();
    }
    public static Address getAddressFromCity(Context context, String location) {

        //inspired by: https://stackoverflow.com/questions/20166328/how-to-get-longitude-latitude-from-the-city-name-android-code

        List<Address> addresses = new ArrayList<>();
        if(Geocoder.isPresent()){
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
}
