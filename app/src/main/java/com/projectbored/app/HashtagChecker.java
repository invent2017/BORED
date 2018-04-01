package com.projectbored.app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HashtagChecker {

    private String storyKey;
    private StringBuilder hashtags = new StringBuilder();
    DatabaseReference mDataRef = FirebaseDatabase.getInstance().getReference().child("stories");

    public HashtagChecker(String storyKey) {
        this.storyKey = storyKey;
    }

    public String getHashtags() {

        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String hashtagPattern = "(#\\w+)";
                String storyCaption = dataSnapshot.child(storyKey).child("Caption").getValue(String.class);

                if(storyCaption != null) {
                    Pattern pattern = Pattern.compile(hashtagPattern);
                    Matcher matcher = pattern.matcher(storyCaption);

                    while (matcher.find()) {
                        if(hashtags.length() == 0) {
                            hashtags.append(matcher.group(1));
                        } else {
                            hashtags.append(", ").append(matcher.group(1));
                        }
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
