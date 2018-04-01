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
    private String storyCaption;
    private StringBuilder hashtags = new StringBuilder();
    DataSnapshot dataSnapshot;

    public HashtagChecker(String storyKey, DataSnapshot dataSnapshot) {
        this.storyKey = storyKey;
        this.dataSnapshot = dataSnapshot;
    }

    public String getHashtags() {

        storyCaption = dataSnapshot.child(storyKey).child("Caption").getValue(String.class);

        String hashtagPattern = "(#\\w+)";
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

        String hashtagsList = hashtags.toString();
        if(hashtagsList.equals("")) {
            hashtagsList = null;
        }

        return hashtagsList;
    }
}
