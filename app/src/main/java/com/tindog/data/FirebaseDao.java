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
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tindog.BuildConfig;

import java.io.File;
import java.util.ArrayList;
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
    public void updateObjectDetailInFirebaseDb(Object object, final String key, final String value) {

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
        List<Dog> dummyUrises = new ArrayList<>();
        Dog dog = new Dog("Snickers", "Male", "Mixed", "Cairo", "Wakanda", "2.5 years");
        dummyUrises.add(dog);
        dog = new Dog("Charlie", "Female", "Mixed", "Cairo", "Wakanda", "3 years");
        dummyUrises.add(dog);
        updateObjectsOrCreateThemInFirebaseDb(dummyUrises);

        List<Family> dummyFamilies = new ArrayList<>();
        Family family = new Family("incrediblestindogprofile@gmail.com");
        family.setPseudonym("The Incredibles");
        dummyFamilies.add(family);
        family = new Family("avengerstindogprofile@gmail.com");
        family.setPseudonym("The Avengers");
        dummyFamilies.add(family);
        updateObjectsOrCreateThemInFirebaseDb(dummyFamilies);

        List<Foundation> dummyFoundations = new ArrayList<>();
        Foundation foundation = new Foundation("Leave No Man Behind", "Old York", "Wisconsin");
        dummyFoundations.add(foundation);
        foundation = new Foundation("All Lives Matter", "London", "England");
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
    public void putImageInFirebaseStorage(Object object, Uri uri, String imageName) {

        String childPath;
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef;

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            childPath = "dogs/" + dog.getUniqueIdentifier() + "/images/" + imageName + ".jpg";
            imageRef = storageRef.child(childPath);
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            childPath = "families/" + family.getUniqueIdentifier() + "/images/" + imageName + ".jpg";
            imageRef = storageRef.child(childPath);
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            childPath = "foundations/" + foundation.getUniqueIdentifier() + "/images/" + imageName + ".jpg";
            imageRef = storageRef.child(childPath);
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            childPath = "users/" + user.getUniqueIdentifier() + "/images/" + imageName + ".jpg";
            imageRef = storageRef.child(childPath);
        }
        else return;

        imageRef.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        StorageMetadata metaData = taskSnapshot.getMetadata();
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

        if (object instanceof Dog) {
            Dog dog = (Dog) object;
            folderPath = "dogs/" + dog.getUniqueIdentifier() + "/images";
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            folderPath = "families/" + family.getUniqueIdentifier() + "/images";
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            folderPath = "foundations/" + foundation.getUniqueIdentifier() + "/images";
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            folderPath = "users/" + user.getUniqueIdentifier() + "/images";
        }
        else return;

        childPath = folderPath + "/" + imageName + ".jpg";
        imageRef = storageRef.child(childPath);

        File storeInternal = new File(mContext.getFilesDir().getAbsolutePath() + "/files/" + folderPath);
        if (!storeInternal.exists()) storeInternal.mkdirs();
        File localFile = new File(storeInternal, imageName + ".jpg");
        //File localFile = File.createTempFile(folderPath, "jpg");
        final Uri imageUri = Uri.fromFile(new File(mContext.getFilesDir().getAbsolutePath() + "/files/" + folderPath + childPath));

        imageRef.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        sendImageUriToInterface(imageUri, imageName);
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

    //Firebase Storage Helper methods (prevent code repetitions in the CRUD methods)
    private void sendImageUriToInterface(Uri imageUri, String imageName) {
        mOnOperationPerformedHandler.onImageAvailable(imageUri, imageName);
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
