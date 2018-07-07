package com.tindog.resources;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
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

    public static int getSmallestWidth(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.smallestScreenWidthDp;
    }
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
    public static int getImagesRecyclerViewPosition(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
        return layoutManager.findFirstVisibleItemPosition();
    }
    public static void shrinkImageWithUri(Uri uri, int width, int height){

        //inspired by: from: https://stackoverflow.com/questions/16954109/reduce-the-size-of-a-bitmap-to-a-specified-size-in-android

        //If the image is already small, don't change it (file.length()==0 means the image wasn't found)
        File file = new File(uri.getPath());
        while (file.length()/1024 > SharedMethods.MAX_IMAGE_FILE_SIZE) {
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = true;
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

    }
    public static Uri updateImageInLocalDirectoryAndShowIt(Context context,
                                                           Uri originalImageUri,
                                                           String directory,
                                                           String imageName,
                                                           ImageView imageViewMain,
                                                           ImagesRecycleViewAdapter imagesRecycleViewAdapter) {

        Uri copiedImageUri = moveFile(originalImageUri, directory, imageName);
        //SharedMethods.deleteFileAtUri(croppedImageTempUri);

        if (imageName.equals("mainImage")) refreshMainImageShownToUser(context, directory, imageViewMain);
        else refreshImagesListShownToUser(directory, imagesRecycleViewAdapter);

        return copiedImageUri;
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
        if (localImageUri!=null && downloadedImageUri!=null) {

            //If the downloaded image is newer, then update the image in the local directory
            if (!downloadedImageUri.toString().equals(localImageUri.toString())) {
                SharedMethods.updateImageInLocalDirectoryAndShowIt(context,
                        downloadedImageUri, localDirectory, imageName, imageViewMain, imagesRecycleViewAdapter);
            }

            //If the local image is newer, then upload it to Firebase to replace the older image
            else {
                firebaseDao.putImageInFirebaseStorage(object, localImageUri, imageName);
            }
        }
    }
}
