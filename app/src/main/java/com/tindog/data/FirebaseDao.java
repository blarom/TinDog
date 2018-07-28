package com.tindog.data;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

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
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tindog.BuildConfig;
import com.tindog.R;
import com.tindog.resources.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FirebaseDao {

    //See this link to understand why a single value event listener is not used here:
    //https://stackoverflow.com/questions/37185418/android-firebase-complex-or-not-query-issue-with-unique-ids#51565273

    public static final String firebaseEmail = BuildConfig.firebaseEmail;
    public static final String firebasePass = BuildConfig.firebasePass;
    private static final String DEBUG_TAG = "TinDog DB Debug";
    private static final int FIREBASE_IMAGE_DOWNLOAD_NOTIFICATION_ID = 4569;
    private static final int PROGRESS_MAX = 100;
    private final Context mContext;
    private final DatabaseReference mFirebaseDbReference;
    private ValueEventListener mEventListenerGetUniqueObject;
    private ValueEventListener mEventListenerGetObjectByValuePair;
    private ValueEventListener mEventListenerUpdateKeyValuePair;
    private ValueEventListener mEventListenerUpdateObject;
    private ValueEventListener mEventListenerGetFullObjectsList;
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManagerCompat mNotificationManager;

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

            //Setting the dog's unique identifier
            if (dog.getUI().equals("")) {
                key = firebaseDbReference.child("dogsList").push().getKey();
                dog.setUI(key);
            }
            else {
                key = dog.getUI();
            }

            //Creating the dog in Firebase
            if (key!=null) firebaseDbReference.child("dogsList").child(key).setValue(dog);
        }
        else if (object instanceof Family) {
            Family family = (Family) object;

            //Setting the family's unique identifier
            family.setUniqueIdentifierFromDetails();

            //Creating the family in Firebase
            key = family.getUI();
            firebaseDbReference.child("familiesList").child(key).setValue(family);
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;

            //Setting the foundation's unique identifier
            foundation.setUniqueIdentifierFromDetails();

            //Creating the foundation in Firebase
            key = foundation.getUI();
            firebaseDbReference.child("foundationsList").child(key).setValue(foundation);
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            key = user.getUI();
            firebaseDbReference.child("usersList").child(key).setValue(user);
        }
        else if (object instanceof MapMarker) {
            MapMarker marker = (MapMarker) object;

            //Setting the marker's unique identifier
            if (marker.getUI().equals("")) {
                key = firebaseDbReference.child("markersList").push().getKey();
                marker.setUI(key);
            }
            else {
                key = marker.getUI();
            }
            if (key!=null) firebaseDbReference.child("markersList").child(key).setValue(marker);
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
            reference.addValueEventListener(mEventListenerGetUniqueObject);
        }
        else if (object instanceof Family) {
            Family family = (Family) object;
            DatabaseReference reference = firebaseDbReference.child("familiesList").child(family.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new Family());
            reference.addValueEventListener(mEventListenerGetUniqueObject);
        }
        else if (object instanceof Foundation) {
            Foundation foundation = (Foundation) object;
            DatabaseReference reference = firebaseDbReference.child("foundationsList").child(foundation.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new Foundation());
            reference.addValueEventListener(mEventListenerGetUniqueObject);
        }
        else if (object instanceof TinDogUser) {
            TinDogUser user = (TinDogUser) object;
            DatabaseReference reference = firebaseDbReference.child("usersList").child(user.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new TinDogUser());
            reference.addValueEventListener(mEventListenerGetUniqueObject);
        }
        else if (object instanceof MapMarker) {
            MapMarker marker = (MapMarker) object;
            DatabaseReference reference = firebaseDbReference.child("markersList").child(marker.getUI());
            mEventListenerGetUniqueObject = createListenerForUniqueObject(new MapMarker());
            reference.addValueEventListener(mEventListenerGetUniqueObject);
        }

    }
    public void getObjectsByKeyValuePairFromFirebaseDb(Object object, String key, String value) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            DatabaseReference reference = firebaseDbReference.child("dogsList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new Dog());
            objectWithKeyQuery.addValueEventListener(mEventListenerGetObjectByValuePair);
        }
        else if (object instanceof Family) {
            DatabaseReference reference = firebaseDbReference.child("familiesList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new Family());
            objectWithKeyQuery.addValueEventListener(mEventListenerGetObjectByValuePair);
        }
        else if (object instanceof Foundation) {
            DatabaseReference reference = firebaseDbReference.child("foundationsList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new Foundation());
            objectWithKeyQuery.addValueEventListener(mEventListenerGetObjectByValuePair);
        }
        else if (object instanceof TinDogUser) {
            DatabaseReference reference = firebaseDbReference.child("usersList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new TinDogUser());
            objectWithKeyQuery.addValueEventListener(mEventListenerGetObjectByValuePair);
        }
        else if (object instanceof MapMarker) {
            DatabaseReference reference = firebaseDbReference.child("markersList");
            Query objectWithKeyQuery = reference.orderByChild(key).equalTo(value);
            mEventListenerGetObjectByValuePair = createListenerForObjectList(new MapMarker());
            objectWithKeyQuery.addValueEventListener(mEventListenerGetObjectByValuePair);
        }

    }
    public void getFullObjectsListFromFirebaseDb(Object object) {

        DatabaseReference firebaseDbReference = FirebaseDatabase.getInstance().getReference();

        if (object instanceof Dog) {
            DatabaseReference reference = firebaseDbReference.child("dogsList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new Dog());
            reference.addValueEventListener(mEventListenerGetFullObjectsList);
        }
        else if (object instanceof Family) {
            DatabaseReference reference = firebaseDbReference.child("familiesList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new Family());
            reference.addValueEventListener(mEventListenerGetFullObjectsList);
        }
        else if (object instanceof Foundation) {
            DatabaseReference reference = firebaseDbReference.child("foundationsList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new Foundation());
            reference.addValueEventListener(mEventListenerGetFullObjectsList);
        }
        else if (object instanceof TinDogUser) {
            DatabaseReference reference = firebaseDbReference.child("usersList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new TinDogUser());
            reference.addValueEventListener(mEventListenerGetFullObjectsList);
        }
        else if (object instanceof MapMarker) {
            DatabaseReference reference = firebaseDbReference.child("markersList");
            mEventListenerGetFullObjectsList = createListenerForObjectList(new MapMarker());
            reference.addValueEventListener(mEventListenerGetFullObjectsList);
        }

    }
    private void updateObjectKeyValuePairInFirebaseDb(Object object, final String key, final Object value) {

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
            reference.addValueEventListener(mEventListenerUpdateKeyValuePair);
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
            reference.addValueEventListener(mEventListenerUpdateKeyValuePair);
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
            reference.addValueEventListener(mEventListenerUpdateKeyValuePair);
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
            reference.addValueEventListener(mEventListenerUpdateKeyValuePair);
        }
        else if (object instanceof MapMarker) {
            final MapMarker marker = (MapMarker) object;
            final DatabaseReference reference = firebaseDbReference.child("markersList");
            mEventListenerUpdateKeyValuePair = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(marker.getUI())) {
                        reference.child(marker.getUI()).child(key).setValue(value);
                    }
                    else {
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            reference.addValueEventListener(mEventListenerUpdateKeyValuePair);
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
            reference.addValueEventListener(mEventListenerUpdateObject);
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
            reference.addValueEventListener(mEventListenerUpdateObject);
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
            reference.addValueEventListener(mEventListenerUpdateObject);
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
            reference.addValueEventListener(mEventListenerUpdateObject);
        }
        else if (object instanceof MapMarker) {
            final MapMarker marker = (MapMarker) object;
            final DatabaseReference reference = firebaseDbReference.child("markersList");
            mEventListenerUpdateObject = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(marker.getUI())) {
                        reference.child(marker.getUI()).setValue(marker);
                    }
                    else {
                        addObjectToFirebaseDb(marker);
                        Log.i(DEBUG_TAG,"TinDog: Firebase error - tried to update non-existent object!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    addObjectToFirebaseDb(marker);
                }


            };
            reference.addValueEventListener(mEventListenerUpdateObject);
        }
    }
    private void updateObjectsOrCreateThemInFirebaseDb(Object objectsData) {

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
            firebaseDbReference.child("usersList").child(user.getUI()).removeValue();
        }
        else if (object instanceof MapMarker) {
            MapMarker marker = (MapMarker) object;
            firebaseDbReference.child("markersList").child(marker.getUI()).removeValue();
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
        else if (object instanceof MapMarker) {
            List<MapMarker> markersList = new ArrayList<>();
            for(DataSnapshot ds : dataSnapshot.getChildren()) {
                markersList.add(ds.getValue(MapMarker.class));
            }
            mOnOperationPerformedHandler.onMapMarkerListFound(markersList);
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
        else if (object instanceof MapMarker) {
            List<MapMarker> markersList = new ArrayList<>();
            markersList.add(dataSnapshot.getValue(MapMarker.class));
            mOnOperationPerformedHandler.onMapMarkerListFound(markersList);
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
    public void populateFirebaseDbWithDummyData(Context context, double userLatitude, double userLongitude) {

        String[] newAddress; //A new geolocation is created in the vicinity of the user location
        double newLatitude;
        double newLongitude;


        List<Family> dummyFamilies = new ArrayList<>();
        newLatitude = userLatitude-0.05;
        newLongitude = userLongitude+0.005;
        newAddress = Utilities.getExactAddressFromGeoCoordinates(context, newLatitude, newLongitude);
        if (newAddress!=null) {
            Family family = new Family("International Test Family 1", newAddress[0], newAddress[1], newAddress[3]);
            family.setGaLt(Double.toString(newLatitude));
            family.setGaLg(Double.toString(newLongitude));
            family.setUI("international-test-family-1");
            dummyFamilies.add(family);
        }
        newLatitude = userLatitude+0.02;
        newLongitude = userLongitude-0.03;
        newAddress = Utilities.getExactAddressFromGeoCoordinates(context, newLatitude, newLongitude);
        if (newAddress!=null) {
            Family family = new Family("International Test Family 2", newAddress[0], newAddress[1], newAddress[3]);
            family.setGaLt(Double.toString(newLatitude));
            family.setGaLg(Double.toString(newLongitude));
            family.setUI("international-test-family-2");
            dummyFamilies.add(family);
        }
        updateObjectsOrCreateThemInFirebaseDb(dummyFamilies);


        List<Foundation> dummyFoundations = new ArrayList<>();
        newLatitude = userLatitude+0.045;
        newLongitude = userLongitude+0.06;
        newAddress = Utilities.getExactAddressFromGeoCoordinates(context, newLatitude, newLongitude);
        if (newAddress!=null) {
            Foundation foundation = new Foundation("International Test Foundation 1", newAddress[0], newAddress[1], newAddress[3]);
            foundation.setGaLt(Double.toString(newLatitude));
            foundation.setGaLg(Double.toString(newLongitude));
            foundation.setUI("international-test-foundation-1");
            dummyFoundations.add(foundation);
        }
        newLatitude = userLatitude-0.04;
        newLongitude = userLongitude+0.02;
        newAddress = Utilities.getExactAddressFromGeoCoordinates(context, newLatitude, newLongitude);
        if (newAddress!=null) {
            Foundation foundation = new Foundation("International Test Foundation 2", newAddress[0], newAddress[1], newAddress[3]);
            foundation.setGaLt(Double.toString(newLatitude));
            foundation.setGaLg(Double.toString(newLongitude));
            foundation.setUI("international-test-foundation-2");
            dummyFoundations.add(foundation);
        }
        updateObjectsOrCreateThemInFirebaseDb(dummyFoundations);


        List<Dog> dummyDogs = new ArrayList<>();
        newLatitude = userLatitude+0.01;
        newLongitude = userLongitude+0.01;
        newAddress = Utilities.getExactAddressFromGeoCoordinates(context, newLatitude, newLongitude);
        if (newAddress!=null) {
            Dog dog = new Dog("International Test Dog 1", "Male", "Mixed", newAddress[0], newAddress[1], newAddress[3], "2.5 years");
            dog.setGaLt(Double.toString(newLatitude));
            dog.setGaLg(Double.toString(newLongitude));
            dog.setUI("international-test-dog-1");
            dog.setAFid("international-test-foundation-1");
            dog.setFN("International Test Foundation 1");
            dummyDogs.add(dog);
        }
        newLatitude = userLatitude-0.01;
        newLongitude = userLongitude+0.02;
        newAddress = Utilities.getExactAddressFromGeoCoordinates(context, newLatitude, newLongitude);
        if (newAddress!=null) {
            Dog dog = new Dog("International Test Dog 2", "Female", "Mixed", newAddress[0], newAddress[1], newAddress[3], "3 years");
            dog.setGaLt(Double.toString(newLatitude));
            dog.setGaLg(Double.toString(newLongitude));
            dog.setUI("international-test-dog-2");
            dog.setAFid("international-test-foundation-1");
            dog.setFN("International Test Foundation 1");
            dummyDogs.add(dog);
        }
        newLatitude = userLatitude-0.04;
        newLongitude = userLongitude-0.01;
        newAddress = Utilities.getExactAddressFromGeoCoordinates(context, newLatitude, newLongitude);
        if (newAddress!=null) {
            Dog dog = new Dog("International Test Dog 3", "Female", "Mixed", newAddress[0], newAddress[1], newAddress[3], "3 years");
            dog.setGaLt(Double.toString(newLatitude));
            dog.setGaLg(Double.toString(newLongitude));
            dog.setUI("international-test-dog-3");
            dog.setAFid("international-test-foundation-2");
            dog.setFN("International Test Foundation 2");
            dummyDogs.add(dog);
        }
        updateObjectsOrCreateThemInFirebaseDb(dummyDogs);

    }
    public void removeDummyData() {
        deleteObjectFromFirebaseDb(new Dog("international-test-dog-1"));
        deleteObjectFromFirebaseDb(new Dog("international-test-dog-2"));
        deleteObjectFromFirebaseDb(new Dog("international-test-dog-3"));

        deleteObjectFromFirebaseDb(new Family("international-test-family-1"));
        deleteObjectFromFirebaseDb(new Family("international-test-family-2"));

        deleteObjectFromFirebaseDb(new Foundation("international-test-foundation-1"));
        deleteObjectFromFirebaseDb(new Foundation("international-test-foundation-2"));
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
                        //Toast.makeText(mContext, "Failed to upload image, check log.", Toast.LENGTH_SHORT).show();
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

        if (Utilities.imageNameIsInvalid(imageName)) return;

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

        final Uri localImageUri = Utilities.getLocalImageUriForObject(mContext, object, imageName);
        if (uploadTimes==null || uploadTimes.size()==0) {
            sendImageUriToInterface(localImageUri, imageName);
            return;
        }

        //If the image loaded into Firebase is newer than the image saved onto the local device (if it exists), then download it. Otherwise, use the local image.
        String internalStorageDirString = Utilities.getImagesDirectoryForObject(mContext, object);
        if (Utilities.directoryIsInvalid(internalStorageDirString)) {
            Log.i(DEBUG_TAG, "Serious error in getImageFromFirebaseStorage(): invalid images directory: " + internalStorageDirString);
            return;
        }
        File internalStorageDir = new File(internalStorageDirString);
        if (!internalStorageDir.exists()) internalStorageDir.mkdirs();

        File localFile = new File(internalStorageDirString, imageName + ".jpg");
        if (localFile.exists()) {
            if (localImageUri!=null) Log.i(DEBUG_TAG, "Local file does exists: " + localImageUri.toString());

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
            if (imageUploadTime!=0 && imageUploadTime > lastModifiedTime && Utilities.internetIsAvailable(mContext)) {
                if (localImageUri!=null) Log.i(DEBUG_TAG, "Local file " + localImageUri.toString() + "with mod time " + lastModifiedTime + " is older than Firebase image with u/l time " + imageUploadTime);
                downloadFromFirebase(internalStorageDir, imageName, childPath, localImageUri);
            }
            else {
                sendImageUriToInterface(localImageUri, imageName);
            }
        }
        else {
            if (localImageUri!=null) Log.i(DEBUG_TAG, "Local file does not exist: " + localImageUri.toString());
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

        // Issue the initial notification with zero progress
        mNotificationBuilder = new NotificationCompat.Builder(mContext, mContext.getString(R.string.tindog_notification_channel))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Tindog image download")
                .setContentText("Download in progress")
                .setProgress(PROGRESS_MAX, 0, false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationManager = NotificationManagerCompat.from(mContext);

        Log.i(DEBUG_TAG, "Attempting to download image with uri: " + localFirebaseTempImageUri.toString());
        imageRef.getFile(localFirebaseTempImage)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        mNotificationBuilder.setProgress(0, 0, false);
                        mNotificationManager.notify(FIREBASE_IMAGE_DOWNLOAD_NOTIFICATION_ID, mNotificationBuilder.build());
                        //Log.i(DEBUG_TAG, "Successfully downloaded image with uri: " + localFirebaseTempImageUri.toString());
                        sendImageUriToInterface(localFirebaseTempImageUri, imageName);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        //Log.i(DEBUG_TAG, "Download failed for image with uri: " + localFirebaseTempImageUri.toString());
                        sendImageUriToInterface(localImageUri, imageName);
                        //exception.printStackTrace();
                        //Toast.makeText(mContext, "Failed to retrieve image from Firebase storage, check log.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        int progress = (int) ( (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount() );
                        mNotificationBuilder.setProgress(PROGRESS_MAX, progress, false);
                        mNotificationManager.notify(FIREBASE_IMAGE_DOWNLOAD_NOTIFICATION_ID, mNotificationBuilder.build());
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
    private void deleteImageFromFirebaseStorage(Object object, final String imageName) {

        if (Utilities.imageNameIsInvalid(imageName)) return;

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

    //Communication with other activities/fragments
    final private FirebaseOperationsHandler mOnOperationPerformedHandler;
    public interface FirebaseOperationsHandler {
        void onDogsListFound(List<Dog> dogsList);
        void onFamiliesListFound(List<Family> familiesList);
        void onFoundationsListFound(List<Foundation> foundationsList);
        void onTinDogUserListFound(List<TinDogUser> usersList);
        void onMapMarkerListFound(List<MapMarker> markersList);
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
