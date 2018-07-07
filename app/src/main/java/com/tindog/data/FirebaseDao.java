package com.tindog.data;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tindog.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FirebaseDao {

    public static final String firebaseEmail = BuildConfig.firebaseEmail;
    public static final String firebasePass = BuildConfig.firebasePass;
    private static final String DEBUG_TAG = "TinDog DB Debug";
    private final Context mContext;

    public FirebaseDao(Context context, FirebaseOperationsHandler listener) {
        this.mContext = context;
        this.mOnOperationPerformedHandler = listener;
    }

    //Firebase Database CRUD methods
    private void addObjectToFirebaseDb(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            dog.setUniqueIdentifierFromDetails();
            firebaseDbReference.child("dogsList").child(dog.getUniqueIdentifier()).setValue(dog);
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            family.setUniqueIdentifierFromDetails();
            firebaseDbReference.child("familiesList").child(family.getUniqueIdentifier()).setValue(family);
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            foundation.setUniqueIdentifierFromDetails();
            firebaseDbReference.child("foundationsList").child(foundation.getUniqueIdentifier()).setValue(foundation);
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            firebaseDbReference.child("usersList").child(user.getUniqueIdentifier()).setValue(user);
        }

    }
    public void addObjectsToFirebaseDb(Object objectsData) {

        try {
            List<Object> objectsList = (List<Object>) objectsData;
            for (int i = 0; i < objectsList.size(); i++) {
                Object object = objectsList.get(i);
                if (object != null) addObjectToFirebaseDb(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void getUniqueObjectFromFirebaseDb(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            DatabaseReference reference = firebaseDbReference.child("dogsList").child(dog.getUniqueIdentifier());
            ValueEventListener eventListener = createListenerForUniqueObject(new Dog());
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            DatabaseReference reference = firebaseDbReference.child("familiesList").child(family.getUniqueIdentifier());
            ValueEventListener eventListener = createListenerForUniqueObject(new Family());
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            DatabaseReference reference = firebaseDbReference.child("foundationsList").child(foundation.getUniqueIdentifier());
            ValueEventListener eventListener = createListenerForUniqueObject(new Foundation());
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            DatabaseReference reference = firebaseDbReference.child("usersList").child(user.getUniqueIdentifier());
            ValueEventListener eventListener = createListenerForUniqueObject(new TinDogUser());
            reference.addListenerForSingleValueEvent(eventListener);
        }

    }
    public void getObjectsByKeyFromFirebaseDb(Object object, String key) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            DatabaseReference reference = firebaseDbReference.child("dogsList");
            Query objectWithKeyQuery = reference.equalTo(key);
            ValueEventListener eventListener = createListenerForObjectList(new Dog());
            objectWithKeyQuery.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Family) {
            DatabaseReference reference = firebaseDbReference.child("familiesList");
            Query objectWithKeyQuery = reference.equalTo(key);
            ValueEventListener eventListener = createListenerForObjectList(new Family());
            objectWithKeyQuery.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Foundation) {
            DatabaseReference reference = firebaseDbReference.child("foundationsList");
            Query objectWithKeyQuery = reference.equalTo(key);
            ValueEventListener eventListener = createListenerForObjectList(new Foundation());
            objectWithKeyQuery.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof TinDogUser) {
            DatabaseReference reference = firebaseDbReference.child("usersList");
            Query objectWithKeyQuery = reference.equalTo(key);
            ValueEventListener eventListener = createListenerForUniqueObject(new TinDogUser());
            objectWithKeyQuery.addListenerForSingleValueEvent(eventListener);
        }

    }
    public void getFullObjectsListFromFirebase(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            DatabaseReference reference = firebaseDbReference.child("dogsList");
            ValueEventListener eventListener = createListenerForObjectList(new Dog());
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Family) {
            DatabaseReference reference = firebaseDbReference.child("familiesList");
            ValueEventListener eventListener = createListenerForObjectList(new Family());
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Foundation) {
            DatabaseReference reference = firebaseDbReference.child("foundationsList");
            ValueEventListener eventListener = createListenerForObjectList(new Foundation());
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof TinDogUser) {
            DatabaseReference reference = firebaseDbReference.child("usersList");
            ValueEventListener eventListener = createListenerForUniqueObject(new TinDogUser());
            reference.addListenerForSingleValueEvent(eventListener);
        }

    }
    public void updateObjectDetailInFirebaseDb(Object object, final String key, final Object value) {

        final DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            final Dog dog = (Dog) object;
            final DatabaseReference reference = firebaseDbReference.child("dogsList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(dog.getUniqueIdentifier())) {
                        reference.child(dog.getUniqueIdentifier()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Family) {
            final Family family = (Family) object;
            final DatabaseReference reference = firebaseDbReference.child("familiesList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(family.getUniqueIdentifier())) {
                        reference.child(family.getUniqueIdentifier()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(eventListener);
            firebaseDbReference.child("familiesList").child(family.getUniqueIdentifier()).child(key).setValue(value);
        }
        else if (object instanceof Foundation) {
            final Foundation foundation = (Foundation) object;
            final DatabaseReference reference = firebaseDbReference.child("foundationsList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(foundation.getUniqueIdentifier())) {
                        reference.child(foundation.getUniqueIdentifier()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof TinDogUser) {
            final TinDogUser user = (TinDogUser) object;
            final DatabaseReference reference = firebaseDbReference.child("usersList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(user.getUniqueIdentifier())) {
                        reference.child(user.getUniqueIdentifier()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(eventListener);
        }
    }
    public void updateObjectOrCreateItInFirebaseDb(Object object) {

        final DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            final Dog dog = (Dog) object;
            final DatabaseReference reference = firebaseDbReference.child("dogsList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(dog.getUniqueIdentifier())) {
                        reference.child(dog.getUniqueIdentifier()).setValue(dog);
                    }
                    else {
                        addObjectToFirebaseDb(dog);
                        Log.i(DEBUG_TAG,"TinDog: Firebase event - tried to update non-existent object, creating it instead");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    addObjectToFirebaseDb(dog);}
            };
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Family) {
            final Family family = (Family) object;
            final DatabaseReference reference = firebaseDbReference.child("familiesList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(family.getUniqueIdentifier())) {
                        reference.child(family.getUniqueIdentifier()).setValue(family);
                    }
                    else {
                        addObjectToFirebaseDb(family);
                        Log.i(DEBUG_TAG,"TinDog: Firebase event - tried to update non-existent object, creating it instead");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    addObjectToFirebaseDb(family);}
            };
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof Foundation) {
            final Foundation foundation = (Foundation) object;
            final DatabaseReference reference = firebaseDbReference.child("foundationsList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(foundation.getUniqueIdentifier())) {
                        reference.child(foundation.getUniqueIdentifier()).setValue(foundation);
                    }
                    else {
                        addObjectToFirebaseDb(foundation);
                        Log.i(DEBUG_TAG,"TinDog: Firebase event - tried to update non-existent object, creating it instead");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    addObjectToFirebaseDb(foundation);}
            };
            reference.addListenerForSingleValueEvent(eventListener);
        }
        else if (object instanceof TinDogUser) {
            final TinDogUser user = (TinDogUser) object;
            final DatabaseReference reference = firebaseDbReference.child("usersList");
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(user.getUniqueIdentifier())) {
                        reference.child(user.getUniqueIdentifier()).setValue(user);
                    }
                    else {
                        addObjectToFirebaseDb(user);
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    addObjectToFirebaseDb(user);
                }


            };
            reference.addListenerForSingleValueEvent(eventListener);
        }
    }
    public void updateObjectsOrCreateThemInFirebaseDb(Object objectsData) {

        try {
            List<Object> objectsList = (List<Object>) objectsData;
            for (int i = 0; i < objectsList.size(); i++) {
                Object object = objectsList.get(i);
                if (object != null) updateObjectOrCreateItInFirebaseDb(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void deleteObjectFromFirebase(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            firebaseDbReference.child("dogsList").child(dog.getUniqueIdentifier()).removeValue();
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            firebaseDbReference.child("familiesList").child(family.getUniqueIdentifier()).removeValue();
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            firebaseDbReference.child("foundationsList").child(foundation.getUniqueIdentifier()).removeValue();
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            firebaseDbReference.child("foundationsList").child(user.getUniqueIdentifier()).removeValue();
        }
    }

    //Firebase Database Helper methods (prevent code repetitions in the CRUD methods)
    private void sendObjectListToInterface(DataSnapshot dataSnapshot, Object object) {

        if (object instanceof Dog) {
            List<Dog> dogsList = new ArrayList<>();
            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                dogsList.add(ds.getValue(Dog.class));
            }
            mOnOperationPerformedHandler.onDogsListFound(dogsList);
        }
        else if (object instanceof Family) {
            List<Family> familiesList = new ArrayList<>();
            for(DataSnapshot ds : dataSnapshot.getChildren()) {
                familiesList.add(ds.getValue(Family.class));
            }
            mOnOperationPerformedHandler.onFamiliesListFound(familiesList);
        }
        else if (object instanceof Foundation) {
            List<Foundation> foundationsList = new ArrayList<>();
            for(DataSnapshot ds : dataSnapshot.getChildren()) {
                foundationsList.add(ds.getValue(Foundation.class));
            }
            mOnOperationPerformedHandler.onFoundationsListFound(foundationsList);
        }
        else if (object instanceof TinDogUser) {
            List<TinDogUser> usersList = new ArrayList<>();
            for(DataSnapshot ds : dataSnapshot.getChildren()) {
                usersList.add(ds.getValue(TinDogUser.class));
            }
            mOnOperationPerformedHandler.onTinDogUserListFound(usersList);
        }
    }
    private void sendUniqueObjectListToInterface(DataSnapshot dataSnapshot, Object object) {

        if (object instanceof Dog) {
            List<Dog> dogsList = new ArrayList<>();
            dogsList.add(dataSnapshot.getValue(Dog.class));
            mOnOperationPerformedHandler.onDogsListFound(dogsList);
        }
        else if (object instanceof Family) {
            List<Family> familiesList = new ArrayList<>();
            familiesList.add(dataSnapshot.getValue(Family.class));
            mOnOperationPerformedHandler.onFamiliesListFound(familiesList);
        }
        else if (object instanceof Foundation) {
            List<Foundation> foundationsList = new ArrayList<>();
            foundationsList.add(dataSnapshot.getValue(Foundation.class));
            mOnOperationPerformedHandler.onFoundationsListFound(foundationsList);
        }
        else if (object instanceof TinDogUser) {
            List<TinDogUser> usersList = new ArrayList<>();
            usersList.add(dataSnapshot.getValue(TinDogUser.class));
            mOnOperationPerformedHandler.onTinDogUserListFound(usersList);
        }
    }
    private ValueEventListener createListenerForObjectList(final Object object) {
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                sendObjectListToInterface(dataSnapshot, object);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        return eventListener;
    }
    private ValueEventListener createListenerForUniqueObject(final Object object) {
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                sendUniqueObjectListToInterface(dataSnapshot, object);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        return eventListener;
    }
    public void populateFirebaseDbWithDummyData() {
        List<Dog> dummyDogs = new ArrayList<>();
        Dog dog = new Dog("Snickers", "Male", "Mixed", "Cairo", "Wakanda", "2.5 years");
        dummyDogs.add(dog);
        dog = new Dog("Charlie", "Female", "Mixed", "Cairo", "Wakanda", "3 years");
        dummyDogs.add(dog);
        updateObjectsOrCreateThemInFirebaseDb(dummyDogs);

        List<Family> dummyFamilies = new ArrayList<>();
        Family family = new Family("The Incredibles", "incrediblestindogprofile@gmail.com");
        family.setUniqueIdentifierFromDetails();
        dummyFamilies.add(family);
        family = new Family("The Avengers", "avengerstindogprofile@gmail.com");
        family.setUniqueIdentifierFromDetails();
        dummyFamilies.add(family);
        updateObjectsOrCreateThemInFirebaseDb(dummyFamilies);

        List<Foundation> dummyFoundations = new ArrayList<>();
        Foundation foundation = new Foundation("Leave No Man Behind", "Old York", "Wisconsin");
        foundation.setUniqueIdentifierFromDetails();
        dummyFoundations.add(foundation);
        foundation = new Foundation("All Lives Matter", "London", "England");
        foundation.setUniqueIdentifierFromDetails();
        dummyFoundations.add(foundation);
        updateObjectsOrCreateThemInFirebaseDb(dummyFoundations);
    }
    public void removeDummyData() {
        Dog dog = new Dog("Snickers", "Male", "Mixed", "Cairo", "Wakanda", "2.5 years");
        deleteObjectFromFirebase(dog);
        dog = new Dog("Charlie", "Female", "Mixed", "Cairo", "Wakanda", "2.5 years");
        deleteObjectFromFirebase(dog);

        Family family = new Family("incrediblestindogprofile@gmail.com");
        deleteObjectFromFirebase(family);
        family = new Family("avengerstindogprofile@gmail.com");
        deleteObjectFromFirebase(family);

        Foundation foundation = new Foundation("Leave No Man Behind", "Old York", "Wisconsin");
        deleteObjectFromFirebase(foundation);
        foundation = new Foundation("All Lives Matter", "London", "England");
        deleteObjectFromFirebase(foundation);
    }

    //Firebase Storage methods
    public void putImageInFirebaseStorage(final Object object, Uri localUri, final String imageName) {

        String childPath;
        String folderPath;
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef;

        List<String> uploadTimes;
        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            folderPath = "dogs/" + dog.getUniqueIdentifier() + "/images";
            uploadTimes = dog.getImageUploadTimes();
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            folderPath = "families/" + family.getUniqueIdentifier() + "/images";
            uploadTimes = family.getImageUploadTimes();
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            folderPath = "foundations/" + foundation.getUniqueIdentifier() + "/images";
            uploadTimes = foundation.getImageUploadTimes();
        }
        else return;

        childPath = folderPath + "/" + imageName + ".jpg";
        imageRef = storageRef.child(childPath);

        final List<String> finalUploadTimes = uploadTimes;
        imageRef.putFile(localUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //StorageMetadata metaData = taskSnapshot.getMetadata();
                        long currentTime= System.currentTimeMillis();
                        if (finalUploadTimes.size()>0) {
                            switch (imageName) {
                                case "mainImage": finalUploadTimes.set(0,String.valueOf(currentTime)); break;
                                case "image1": finalUploadTimes.set(1,String.valueOf(currentTime)); break;
                                case "image2": finalUploadTimes.set(2,String.valueOf(currentTime)); break;
                                case "image3": finalUploadTimes.set(3,String.valueOf(currentTime)); break;
                                case "image4": finalUploadTimes.set(4,String.valueOf(currentTime)); break;
                                case "image5": finalUploadTimes.set(5,String.valueOf(currentTime)); break;
                            }
                            updateObjectDetailInFirebaseDb(object, "imageUploadTimes", finalUploadTimes);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        exception.printStackTrace();
                        Toast.makeText(mContext, "Failed to upload image, check log.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void getImageFromFirebaseStorage(Object object, final String imageName) {

        if (TextUtils.isEmpty(imageName)
                || !(imageName.equals("mainImage")
                    || imageName.equals("image1")
                    || imageName.equals("image2")
                    || imageName.equals("image3")
                    || imageName.equals("image4")
                    || imageName.equals("image5"))){
            Log.i(DEBUG_TAG, "Invalid filename for image in FirebaseDao.getImageFromFirebaseStorage() method.");
            return;
        }

        String childPath;
        String folderPath;
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef;

        List<String> uploadTimes;
        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            folderPath = "dogs/" + dog.getUniqueIdentifier() + "/images";
            uploadTimes = dog.getImageUploadTimes();
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            folderPath = "families/" + family.getUniqueIdentifier() + "/images";
            uploadTimes = family.getImageUploadTimes();
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            folderPath = "foundations/" + foundation.getUniqueIdentifier() + "/images";
            uploadTimes = foundation.getImageUploadTimes();
        }
        else return;

        childPath = folderPath + "/" + imageName + ".jpg";
        imageRef = storageRef.child(childPath);

        final Uri localImageUri = getLocalImageUri(folderPath, imageName);
        if (uploadTimes==null || uploadTimes.size()==0) {
            sendImageUriToInterface(localImageUri, imageName);
            return;
        }

        //If the image loaded into Firebase is newer than the image saved onto the local device (if it exists), then download it. Otherwise, use the local image.
        File storeInternal = new File(mContext.getFilesDir().getAbsolutePath() + "/" + folderPath);
        if (!storeInternal.exists()) storeInternal.mkdirs();
        File localFile = new File(storeInternal, imageName + ".jpg");
        Date lastModified = new Date(localFile.lastModified());
        long lastModifiedTime = lastModified.getTime();

        long imageUploadTime = 0;

        switch (imageName) {
            case "mainImage": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(0)) ? Long.parseLong(uploadTimes.get(0)) : 0; break;
            case "image1": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(1)) ? Long.parseLong(uploadTimes.get(0)) : 0; break;
            case "image2": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(2)) ? Long.parseLong(uploadTimes.get(0)) : 0; break;
            case "image3": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(3)) ? Long.parseLong(uploadTimes.get(0)) : 0; break;
            case "image4": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(4)) ? Long.parseLong(uploadTimes.get(0)) : 0; break;
            case "image5": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(5)) ? Long.parseLong(uploadTimes.get(0)) : 0; break;
        }

        //If the local file is older than the Firebase file, then download the Firebase file to the cache directory
        if (!localFile.exists() || (localFile.exists() && imageUploadTime!=0 && imageUploadTime > lastModifiedTime)) {

            File cacheDirectory = new File(mContext.getFilesDir().getAbsolutePath() + "/cache");
            if (!cacheDirectory.exists()) cacheDirectory.mkdirs();
            File localFirebaseTempImage = new File(storeInternal, imageName + ".jpg");
            final Uri localFirebaseTempImageUri = Uri.fromFile(localFirebaseTempImage);

            imageRef.getFile(localFirebaseTempImage)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            sendImageUriToInterface(localFirebaseTempImageUri, imageName);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            exception.printStackTrace();
                            //Toast.makeText(mContext, "Failed to retrieve image from Firebase storage, check log.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            sendImageUriToInterface(localImageUri, imageName);
        }

    }

    //Firebase Storage Helper methods (prevent code repetitions in the CRUD methods)
    private void sendImageUriToInterface(Uri imageUri, String imageName) {
        mOnOperationPerformedHandler.onImageAvailable(imageUri, imageName);
    }
    private Uri getLocalImageUri(String folderPath, String imageName) {

        File storeInternal = new File(mContext.getFilesDir().getAbsolutePath() + "/" + folderPath);
        if (!storeInternal.exists()) storeInternal.mkdirs();
        File localFile = new File(storeInternal, imageName + ".jpg");
        return Uri.fromFile(localFile);
    }

    //Communication with other activities/fragments
    final private FirebaseOperationsHandler mOnOperationPerformedHandler;
    public interface FirebaseOperationsHandler {
        void onDogsListFound(List<Dog> dogsList);
        void onFamiliesListFound(List<Family> familiesList);
        void onFoundationsListFound(List<Foundation> foundationsList);
        void onTinDogUserListFound(List<TinDogUser> usersList);
        void onImageAvailable(android.net.Uri imageUri, String imageName);
    }
}
