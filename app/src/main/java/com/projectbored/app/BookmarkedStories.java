package com.projectbored.app;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class BookmarkedStories extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";
    private String username;

    private ListView storyList;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarked_stories);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        storyList = (ListView) findViewById(R.id.bookmarked_story_list);
        username = getSharedPreferences(PREFS_NAME, 0).getString("Username","");

        getBookmarkedStories();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void getBookmarkedStories() {
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> stories = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.child("users").child(username).child("Bookmarked").getChildren()) {
                    stories.add(ds.getKey());
                }

                BookmarkedStoryListAdapter adapter = new BookmarkedStoryListAdapter(BookmarkedStories.this,
                        stories, dataSnapshot.child("stories"));
                storyList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
