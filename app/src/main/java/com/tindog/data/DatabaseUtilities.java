package com.tindog.data;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.tindog.BuildConfig;
import com.tindog.R;
import com.tindog.resources.SharedMethods;

import java.io.File;

public class DatabaseUtilities {

    public static final String firebaseEmail = BuildConfig.firebaseEmail;
    public static final String firebasePass = BuildConfig.firebasePass;
    private static final String DEBUG_TAG = "TinDog DB";

    static String cleanIdentifierForFirebase(String string) {
        string = string.replaceAll("\\.","*");
        string = string.replaceAll("#","*");
        string = string.replaceAll("\\$","*");
        string = string.replaceAll("\\[","*");
        string = string.replaceAll("]","*");
        //string = string.replaceAll("\\{","*");
        //string = string.replaceAll("}","*");
        return string;
    }

    public static void updateFirebaseUserName(final Context context, final FirebaseUser user, String password, final String newInfo) {

        if (user.getEmail()==null) {
            Toast.makeText(context, R.string.error_accessing_user_info, Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(DEBUG_TAG, "User re-authenticated.");

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(newInfo)
                                    //.setPhotoUri(Uri.parse("https://example.com/jane-q-user/profile.jpg"))
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, R.string.successfully_updated_name, Toast.LENGTH_SHORT).show();
                                                Log.d(DEBUG_TAG, "User profile updated.");
                                            }
                                            else {
                                                Toast.makeText(context, R.string.failed_to_update_name, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                        else {
                            Toast.makeText(context, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

}
    public static void updateFirebaseUserPassword(final Context context, final FirebaseUser user, String password, final String newInfo) {

        if (user.getEmail()==null) {
            Toast.makeText(context, R.string.error_accessing_user_info, Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(DEBUG_TAG, "User re-authenticated.");
                            user.updatePassword(newInfo)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, R.string.successfully_updated_password, Toast.LENGTH_SHORT).show();
                                                Log.d(DEBUG_TAG, "User password updated.");
                                            }
                                            else {
                                                Toast.makeText(context, R.string.failed_to_update_password, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                        else {
                            Toast.makeText(context, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    public static void updateFirebaseUserEmail(final Context context, final FirebaseUser user, String password, final String newInfo) {

        if (user.getEmail()==null) {
            Toast.makeText(context, R.string.error_accessing_user_info, Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(DEBUG_TAG, "User re-authenticated.");
                            user.updateEmail(newInfo)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, R.string.successfully_updated_email, Toast.LENGTH_SHORT).show();
                                                Log.d(DEBUG_TAG, "User email updated.");
                                            }
                                            else {
                                                Toast.makeText(context, R.string.failed_to_update_email, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                        else {
                            Toast.makeText(context, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    public static Uri getImageUri(Context context, Object object, String imageName) {

        String imageDirectory;
        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            imageDirectory = context.getFilesDir().getAbsolutePath()+"/dogs/"+dog.getUniqueIdentifier()+"/images/";
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            imageDirectory = context.getFilesDir().getAbsolutePath()+"/families/"+ family.getUniqueIdentifier()+"/images/";
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            imageDirectory = context.getFilesDir().getAbsolutePath()+"/foundations/"+ foundation.getUniqueIdentifier()+"/images/";
        }
        else return null;

        return SharedMethods.getUriForImage(imageDirectory,imageName);
    }
}
