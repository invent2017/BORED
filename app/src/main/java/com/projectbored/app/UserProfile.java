package com.projectbored.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserProfile extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    private TextView usernameField, emailField, distanceNumber, viewsNumber,
            storyNumber, viewedNumber, upvotesNumber, upvotedNumber;

    private DatabaseReference mDataRef;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        setTitle("My Profile");

        usernameField = (TextView)findViewById(R.id.username);
        emailField = (TextView)findViewById(R.id.email);
        distanceNumber = (TextView)findViewById(R.id.kilometeres_covered);
        viewsNumber = (TextView)findViewById(R.id.stories_read);
        storyNumber = (TextView)findViewById(R.id.stories_posted);
        viewedNumber = (TextView)findViewById(R.id.views_received);
        upvotesNumber = (TextView)findViewById(R.id.upvotes_given);
        upvotedNumber = (TextView)findViewById(R.id.upvotes_received);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        username = getSharedPreferences(PREFS_NAME, 0 ).getString("Username", "");

        if(username != null) {
            loadFields();
        } else {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadFields() {
        usernameField.setText(username);

        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                emailField.setText(dataSnapshot.child("users").child(username).child("Email").getValue(String.class));
                distanceNumber.setText(dataSnapshot.child("users").child(username)
                                        .child("Distance").getValue(Integer.class).toString());

                int views = 0;
                int stories = 0;
                int viewed = 0;
                int upvoted = 0;
                int upvotes = 0;

                if(dataSnapshot.child("users").child(username).hasChild("stories")) {
                    for(DataSnapshot ds : dataSnapshot.child("users").child(username).child("stories").getChildren()){
                        stories++;
                    }

                    for(DataSnapshot ds : dataSnapshot.child("users").child(username).child("stories").getChildren()) {
                        String storyKey = ds.getKey();

                        viewed = viewed + dataSnapshot.child("stories").child(storyKey).child("Views").getValue(Integer.class);
                        upvoted = upvoted + dataSnapshot.child("stories").child(storyKey).child("Votes").getValue(Integer.class);
                    }
                }

                if(dataSnapshot.child("users").child(username).hasChild("ReadStories")) {
                    for(DataSnapshot ds : dataSnapshot.child("users").child(username).child("ReadStories").getChildren()) {
                        views++;
                    }
                }

                if(dataSnapshot.child("users").child(username).hasChild("UpvotedStories")) {
                    for(DataSnapshot ds : dataSnapshot.child("users").child(username).child("UpvotedStories").getChildren()) {
                        upvotes++;
                    }
                }


                viewsNumber.setText(Integer.toString(views));
                storyNumber.setText(Integer.toString(stories));
                viewedNumber.setText(Integer.toString(viewed));
                upvotedNumber.setText(Integer.toString(upvoted));
                upvotesNumber.setText(Integer.toString(upvotes));



            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}