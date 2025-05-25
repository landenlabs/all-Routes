/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.db;


import androidx.lifecycle.MutableLiveData;

import com.landenlabs.routes.data.GpsPoint;

import java.util.ArrayList;

/**
 * Realtime Fire cloud database
 */
public class FirebaseCloud {
    // DatabaseReference database;

    public FirebaseCloud() {
         /*
        database = FirebaseDatabase.getInstance()
                .getReference("test/Routes/")
                .child("v1");

        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Post post = dataSnapshot.getValue(Post.class);
                System.out.println(post);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
         */
    }

    public void saveRoute(String name, ArrayList<GpsPoint> route) {
        // database.child(name).setValue(route);
    }

    public MutableLiveData<ArrayList<GpsPoint>> getRoute(String name) {
        MutableLiveData<ArrayList<GpsPoint>> liveData = new MutableLiveData<>();

        /*
        database.orderByChild(name).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    DataSnapshot child = dataSnapshot.child(name);
                    GenericTypeIndicator<ArrayList<GpsPoint>> t = new GenericTypeIndicator<ArrayList<GpsPoint>>() {
                    };
                    // values.addAll(child.getValue(t));
                    ArrayList<GpsPoint> list = child.getValue(t);
                    / *
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Object obj = snapshot.getValue();
                    }
                     * /
                    ALog.i.tagMsg(this, "First data : " + list.get(0));
                    liveData.postValue(list);
                } catch (Exception ex) {
                    ALog.e.tagMsg(this, "load error=", ex);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ALog.e.tagMsg(this, "load error=", error);
            }
        });
        */

        return liveData;
    }
}
