package com.projectbored.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserStories extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";
    private String username;

    private ListView storyList;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_stories);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        username = getSharedPreferences(PREFS_NAME, 0).getString("Username", "");
        storyList = (ListView) findViewById(R.id.story_list);

        getUserStories();
    }

    @Override
    public void onBackPressed() {
        Intent backToMap = new Intent(this, MapsActivityCurrentPlace.class);
        startActivity(backToMap);
        finish();
    }

    private void getUserStories() {
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> stories = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.child("users").child(username).child("stories").getChildren()) {
                    stories.add(ds.getKey());
                }

                StoryListAdapter adapter = new StoryListAdapter(UserStories.this, stories, dataSnapshot.child("stories"));
                storyList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
