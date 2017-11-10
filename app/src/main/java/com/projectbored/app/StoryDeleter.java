package com.projectbored.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class StoryDeleter extends AppCompatActivity {
    DatabaseReference mStoryRef;
    StorageReference mPhotoRef;

    Bundle storyDetails;

    String storyKey;
    String username;
    String keyLocationString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_deleter);

        storyDetails = getIntent().getExtras();

        mStoryRef = FirebaseDatabase.getInstance().getReference();

        Bundle storyDetails = getIntent().getExtras();
        getValues(storyDetails);

        deleteStory();
    }

    private void getValues(Bundle storyDetails) {
        storyKey = storyDetails.getString("key");
        username = storyDetails.getString("Username");
        String locationString = Double.toString(storyDetails.getDouble("Latitude"))
                + ","
                + Double.toString(storyDetails.getDouble("Longitude"));
        keyLocationString = locationString.replace(".", "d");


    }

    private void deleteStory() {
        mStoryRef.child("stories").child(storyKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String url = dataSnapshot.child("URI").getValue(String.class);
                if (url != null) {
                    mPhotoRef =
                            FirebaseStorage.getInstance().getReferenceFromUrl(
                                dataSnapshot.child("URI").getValue(String.class));
                    mPhotoRef.delete();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(StoryDeleter.this, "Error deleting story.", Toast.LENGTH_SHORT).show();

                Intent backToMap = new Intent(StoryDeleter.this, MapsActivityCurrentPlace.class);
                backToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(backToMap);
            }
        });

        mStoryRef.child("hashtags").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    if (ds.hasChild(storyKey)) {
                        ds.child(storyKey).getRef().removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mStoryRef.child("users").child(username).child("stories").child(storyKey).removeValue();
        mStoryRef.child("stories").child(storyKey).removeValue();
        mStoryRef.child("locations").child(keyLocationString).removeValue();

        Toast.makeText(this, "Story deleted.", Toast.LENGTH_SHORT).show();

        Intent backToMap = new Intent(this, MapsActivityCurrentPlace.class);
        backToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(backToMap);
    }
}
