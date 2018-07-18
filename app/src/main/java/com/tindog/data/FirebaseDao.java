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
import com.tindog.resources.SharedMethods;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FirebaseDao {

    public static final String firebaseEmail = BuildConfig.firebaseEmail;
    public static final String firebasePass = BuildConfig.firebasePass;
    private static final String DEBUG_TAG = "TinDog DB Debug";
    private final Context mContext;
    private final DatabaseReference mFirebaseDbReference;
    private ValueEventListener mEventListenerGetUniqueObject;
    private ValueEventListener mEventListenerGetObjectByValuePair;
    private ValueEventListener mEventListenerUpdateKeyValuePair;
    private ValueEventListener mEventListenerUpdateObject;
    private ValueEventListener mEventListenerGetFullObjectsList;

    public FirebaseDao(Context context, FirebaseOperationsHandler listener) {
        this.mContext = context;
        this.mOnOperationPerformedHandler = listener;
        mFirebaseDbReference = FirebaseDatabase.getInstance().getReference();
    }

    //Firebase Database CRUD methods
    public String addObjectToFirebaseDb(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        String key = "";
        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            if (dog.getUI().equals("")) key = firebaseDbReference.child("dogsList").push().getKey();
            else key = dog.getUI();
            if (key!=null) firebaseDbReference.child("dogsList").child(key).setValue(dog);
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            family.setUniqueIdentifierFromDetails();
            key = family.getUI();
            firebaseDbReference.child("familiesList").child(key).setValue(family);
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            foundation.setUniqueIdentifierFromDetails();
            key = foundation.getUI();
            firebaseDbReference.child("foundationsList").child(key).setValue(foundation);
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            key = user.getUI();
            firebaseDbReference.child("usersList").child(key).setValue(user);
        }

        return key;
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
    public void getUniqueObjectFromFirebaseDbOrCreateIt(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            DatabaseReference reference = firebaseDbReference.child("dogsList").child(dog.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new Dog());
            reference.addListenerForSingleValueEvent(mEventListenerGetUniqueObject);
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            DatabaseReference reference = firebaseDbReference.child("familiesList").child(family.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new Family());
            reference.addListenerForSingleValueEvent(mEventListenerGetUniqueObject);
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            DatabaseReference reference = firebaseDbReference.child("foundationsList").child(foundation.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new Foundation());
            reference.addListenerForSingleValueEvent(mEventListenerGetUniqueObject);
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            DatabaseReference reference = firebaseDbReference.child("usersList").child(user.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new TinDogUser());
            reference.addListenerForSingleValueEvent(mEventListenerGetUniqueObject);
        }

    }
    public void getObjectsByKeyValuePairFromFirebaseDb(Object object, String key, String value) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            DatabaseReference reference = firebaseDbReference.child("dogsList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new Dog());
            objectWithKeyQuery.addListenerForSingleValueEvent(mEventListenerGetObjectByValuePair);
        }
        else if (object instanceof Family) {
            DatabaseReference reference = firebaseDbReference.child("familiesList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new Family());
            objectWithKeyQuery.addListenerForSingleValueEvent(mEventListenerGetObjectByValuePair);
        }
        else if (object instanceof Foundation) {
            DatabaseReference reference = firebaseDbReference.child("foundationsList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new Foundation());
            objectWithKeyQuery.addListenerForSingleValueEvent(mEventListenerGetObjectByValuePair);
        }
        else if (object instanceof TinDogUser) {
            DatabaseReference reference = firebaseDbReference.child("usersList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForUniqueObject(new TinDogUser());
            objectWithKeyQuery.addListenerForSingleValueEvent(mEventListenerGetObjectByValuePair);
        }

    }
    public void getFullObjectsListFromFirebaseDb(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            DatabaseReference reference = firebaseDbReference.child("dogsList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new Dog());
            reference.addListenerForSingleValueEvent(mEventListenerGetFullObjectsList);
        }
        else if (object instanceof Family) {
            DatabaseReference reference = firebaseDbReference.child("familiesList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new Family());
            reference.addListenerForSingleValueEvent(mEventListenerGetFullObjectsList);
        }
        else if (object instanceof Foundation) {
            DatabaseReference reference = firebaseDbReference.child("foundationsList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new Foundation());
            reference.addListenerForSingleValueEvent(mEventListenerGetFullObjectsList);
        }
        else if (object instanceof TinDogUser) {
            DatabaseReference reference = firebaseDbReference.child("usersList");
            mEventListenerGetFullObjectsList = createListenerForUniqueObject(new TinDogUser());
            reference.addListenerForSingleValueEvent(mEventListenerGetFullObjectsList);
        }

    }
    public void updateObjectKeyValuePairInFirebaseDb(Object object, final String key, final Object value) {

        final DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            final Dog dog = (Dog) object;
            final DatabaseReference reference = firebaseDbReference.child("dogsList");
            mEventListenerUpdateKeyValuePair = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(dog.getUI())) {
                        reference.child(dog.getUI()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(mEventListenerUpdateKeyValuePair);
        }
        else if (object instanceof Family) {
            final Family family = (Family) object;
            final DatabaseReference reference = firebaseDbReference.child("familiesList");
            mEventListenerUpdateKeyValuePair = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(family.getUI())) {
                        reference.child(family.getUI()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(mEventListenerUpdateKeyValuePair);
            firebaseDbReference.child("familiesList").child(family.getUI()).child(key).setValue(value);
        }
        else if (object instanceof Foundation) {
            final Foundation foundation = (Foundation) object;
            final DatabaseReference reference = firebaseDbReference.child("foundationsList");
            mEventListenerUpdateKeyValuePair = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(foundation.getUI())) {
                        reference.child(foundation.getUI()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(mEventListenerUpdateKeyValuePair);
        }
        else if (object instanceof TinDogUser) {
            final TinDogUser user = (TinDogUser) object;
            final DatabaseReference reference = firebaseDbReference.child("usersList");
            mEventListenerUpdateKeyValuePair = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(user.getUI())) {
                        reference.child(user.getUI()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addListenerForSingleValueEvent(mEventListenerUpdateKeyValuePair);
        }
    }
    public void updateObjectOrCreateItInFirebaseDb(Object object) {

        final DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            final Dog dog = (Dog) object;
            final DatabaseReference reference = firebaseDbReference.child("dogsList");
            mEventListenerUpdateObject = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(dog.getUI())) {
                        reference.child(dog.getUI()).setValue(dog);
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
            reference.addListenerForSingleValueEvent(mEventListenerUpdateObject);
        }
        else if (object instanceof Family) {
            final Family family = (Family) object;
            final DatabaseReference reference = firebaseDbReference.child("familiesList");
            mEventListenerUpdateObject = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(family.getUI())) {
                        reference.child(family.getUI()).setValue(family);
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
            reference.addListenerForSingleValueEvent(mEventListenerUpdateObject);
        }
        else if (object instanceof Foundation) {
            final Foundation foundation = (Foundation) object;
            final DatabaseReference reference = firebaseDbReference.child("foundationsList");
            mEventListenerUpdateObject = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(foundation.getUI())) {
                        reference.child(foundation.getUI()).setValue(foundation);
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
            reference.addListenerForSingleValueEvent(mEventListenerUpdateObject);
        }
        else if (object instanceof TinDogUser) {
            final TinDogUser user = (TinDogUser) object;
            final DatabaseReference reference = firebaseDbReference.child("usersList");
            mEventListenerUpdateObject = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(user.getUI())) {
                        reference.child(user.getUI()).setValue(user);
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
            reference.addListenerForSingleValueEvent(mEventListenerUpdateObject);
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
    public void deleteObjectFromFirebaseDb(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            firebaseDbReference.child("dogsList").child(dog.getUI()).removeValue();
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            firebaseDbReference.child("familiesList").child(family.getUI()).removeValue();
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            firebaseDbReference.child("foundationsList").child(foundation.getUI()).removeValue();
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            firebaseDbReference.child("foundationsList").child(user.getUI()).removeValue();
        }
    }
    private List<String> getUniqueIds(Object object) {
        List<Object> objectsList = (List<Object>) object;
        List<String> ids = new ArrayList<>();
        Object listElement = objectsList.get(0);
        if (listElement instanceof Dog) {
            Dog dog;
            for (int i=0; i<objectsList.size(); i++) {
                dog = (Dog) objectsList.get(i);
                ids.add(dog.getUI());
            }
        }
        else if (listElement instanceof Family) {
            Family family;
            for (int i=0; i<objectsList.size(); i++) {
                family = (Family) objectsList.get(i);
                ids.add(family.getUI());
            }
        }
        else if (listElement instanceof Foundation) {
            Foundation foundation;
            for (int i=0; i<objectsList.size(); i++) {
                foundation = (Foundation) objectsList.get(i);
                ids.add(foundation.getUI());
            }
        }
        return ids;
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
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //If the object was not found, then try to update it. If that fails, then the object is truly missing so create it
                updateObjectOrCreateItInFirebaseDb(object);
            }
        };
        return eventListener;
    }
    public void populateFirebaseDbWithDummyData() {
        List<Dog> dummyDogs = new ArrayList<>();
        Dog dog = new Dog("Snickers", "Male", "Mixed", "Cairo", "Wakanda", "2.5 years");
        dog.setUI("testDog1");
        dummyDogs.add(dog);
        dog = new Dog("Charlie", "Female", "Mixed", "Cairo", "Wakanda", "3 years");
        dog.setUI("testDog2");
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
        deleteObjectFromFirebaseDb(dog);
        dog = new Dog("Charlie", "Female", "Mixed", "Cairo", "Wakanda", "2.5 years");
        deleteObjectFromFirebaseDb(dog);

        Family family = new Family("incrediblestindogprofile@gmail.com");
        deleteObjectFromFirebaseDb(family);
        family = new Family("avengerstindogprofile@gmail.com");
        deleteObjectFromFirebaseDb(family);

        Foundation foundation = new Foundation("Leave No Man Behind", "Old York", "Wisconsin");
        deleteObjectFromFirebaseDb(foundation);
        foundation = new Foundation("All Lives Matter", "London", "England");
        deleteObjectFromFirebaseDb(foundation);
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
            folderPath = "dogs/" + dog.getUI() + "/images";
            uploadTimes = dog.getIUT();
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            folderPath = "families/" + family.getUI() + "/images";
            uploadTimes = family.getIUT();
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            folderPath = "foundations/" + foundation.getUI() + "/images";
            uploadTimes = foundation.getIUT();
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
                            updateObjectKeyValuePairInFirebaseDb(object, "iut", finalUploadTimes);
                            mOnOperationPerformedHandler.onImageUploaded(finalUploadTimes);
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
    public void getAllObjectImagesFromFirebaseStorage(Object object) {
        getImageFromFirebaseStorage(object, "mainImage");
        getImageFromFirebaseStorage(object, "image1");
        getImageFromFirebaseStorage(object, "image2");
        getImageFromFirebaseStorage(object, "image3");
        getImageFromFirebaseStorage(object, "image4");
        getImageFromFirebaseStorage(object, "image5");
    }
    public void getImageFromFirebaseStorage(Object object, final String imageName) {

        if (SharedMethods.nameIsInvalid(imageName)) return;

        String childPath;
        String folderPath;

        List<String> uploadTimes;
        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            folderPath = "dogs/" + dog.getUI() + "/images";
            uploadTimes = dog.getIUT();
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            folderPath = "families/" + family.getUI() + "/images";
            uploadTimes = family.getIUT();
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            folderPath = "foundations/" + foundation.getUI() + "/images";
            uploadTimes = foundation.getIUT();
        }
        else return;

        childPath = folderPath + "/" + imageName + ".jpg";

        final Uri localImageUri = getLocalImageUri(folderPath, imageName);
        if (uploadTimes==null || uploadTimes.size()==0) {
            sendImageUriToInterface(localImageUri, imageName);
            return;
        }

        //If the image loaded into Firebase is newer than the image saved onto the local device (if it exists), then download it. Otherwise, use the local image.
        File internalStorageDir = new File(mContext.getFilesDir().getAbsolutePath() + "/" + folderPath);
        if (!internalStorageDir.exists()) internalStorageDir.mkdirs();

        File localFile = new File(internalStorageDir, imageName + ".jpg");
        if (localFile.exists()) {
            Date lastModified = new Date(localFile.lastModified());
            long lastModifiedTime = lastModified.getTime();

            long imageUploadTime = 0;

            switch (imageName) {
                case "mainImage": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(0)) ? Long.parseLong(uploadTimes.get(0)) : 0; break;
                case "image1": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(1)) ? Long.parseLong(uploadTimes.get(1)) : 0; break;
                case "image2": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(2)) ? Long.parseLong(uploadTimes.get(2)) : 0; break;
                case "image3": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(3)) ? Long.parseLong(uploadTimes.get(3)) : 0; break;
                case "image4": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(4)) ? Long.parseLong(uploadTimes.get(4)) : 0; break;
                case "image5": imageUploadTime = !TextUtils.isEmpty(uploadTimes.get(5)) ? Long.parseLong(uploadTimes.get(5)) : 0; break;
            }

            //If the local file is older than the Firebase file, then download the Firebase file to the cache directory
            if (imageUploadTime!=0 && imageUploadTime > lastModifiedTime && SharedMethods.internetIsAvailable(mContext)) {
                downloadFromFirebase(internalStorageDir, imageName, childPath, localImageUri);
            }
            else {
                sendImageUriToInterface(localImageUri, imageName);
            }
        }
        else {
            downloadFromFirebase(internalStorageDir, imageName, childPath, localImageUri);
        }

    }
    private void downloadFromFirebase(File internalStorageDir, final String imageName, String childPath, final Uri localImageUri) {

        File cacheDirectory = new File(mContext.getFilesDir().getAbsolutePath() + "/cache");
        if (!cacheDirectory.exists()) cacheDirectory.mkdirs();

        File localFirebaseTempImage = new File(internalStorageDir, imageName + ".jpg");
        final Uri localFirebaseTempImageUri = Uri.fromFile(localFirebaseTempImage);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child(childPath);
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
                        sendImageUriToInterface(localImageUri, imageName);
                        exception.printStackTrace();
                        //Toast.makeText(mContext, "Failed to retrieve image from Firebase storage, check log.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void deleteAllObjectImagesFromFirebaseStorage(Object object) {
        deleteImageFromFirebaseStorage(object, "mainImage");
        deleteImageFromFirebaseStorage(object, "image1");
        deleteImageFromFirebaseStorage(object, "image2");
        deleteImageFromFirebaseStorage(object, "image3");
        deleteImageFromFirebaseStorage(object, "image4");
        deleteImageFromFirebaseStorage(object, "image5");
    }
    public void deleteImageFromFirebaseStorage(Object object, final String imageName) {

        if (SharedMethods.nameIsInvalid(imageName)) return;

        String folderPath;

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            folderPath = "dogs/" + dog.getUI() + "/images";
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            folderPath = "families/" + family.getUI() + "/images";
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            folderPath = "foundations/" + foundation.getUI() + "/images";
        }
        else return;

        final String childPath = folderPath + "/" + imageName + ".jpg";

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child(childPath);
        imageRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(DEBUG_TAG, "Deleted image at: " + childPath);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.i(DEBUG_TAG, "Failed to delete image at: " + childPath);
                    }
                });
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
        void onImageUploaded(List<String> uploadTimes);
    }
    public void removeListeners() {
        if (mEventListenerGetUniqueObject!=null) mFirebaseDbReference.removeEventListener(mEventListenerGetUniqueObject);
        if (mEventListenerGetObjectByValuePair!=null) mFirebaseDbReference.removeEventListener(mEventListenerGetObjectByValuePair);
        if (mEventListenerUpdateKeyValuePair!=null) mFirebaseDbReference.removeEventListener(mEventListenerUpdateKeyValuePair);
        if (mEventListenerUpdateObject!=null) mFirebaseDbReference.removeEventListener(mEventListenerUpdateObject);
        if (mEventListenerGetFullObjectsList!=null) mFirebaseDbReference.removeEventListener(mEventListenerGetFullObjectsList);
    }
}
