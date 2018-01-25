package com.projectbored.app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HashtagChecker {

    String storyKey;
    StringBuilder hashtags = new StringBuilder();
    DatabaseReference mDataRef = FirebaseDatabase.getInstance().getReference().child("stories");

    public HashtagChecker(String storyKey) {
        this.storyKey = storyKey;
    }

    public String getHashtags() {

        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String hashtagPattern = "#\\w+";
                String storyCaption = dataSnapshot.child(storyKey).child("Caption").getValue(String.class);

                if(storyCaption != null) {
                    Matcher matcher = Pattern.compile(hashtagPattern).matcher(storyCaption);
                    while (matcher.find()) {
                        hashtags.append(matcher.group());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        String hashtagsList = hashtags.toString();
        if(hashtagsList.equals("")) {
            hashtagsList = null;
        }

        return hashtagsList;
    }
}
