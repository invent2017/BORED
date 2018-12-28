package com.projectbored.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserProfile extends AppCompatActivity {
    //private static final String PREFS_NAME = "UserDetails";

    private TextView usernameField, emailField, /*distanceNumber,*/ viewsNumber,
            storyNumber, viewedNumber, upvotesNumber, upvotedNumber;
    private Button inviteFriendButton;

    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        setTitle("My Profile");

        usernameField = findViewById(R.id.username);
        emailField = findViewById(R.id.email);
        //distanceNumber = findViewById(R.id.kilometeres_covered);
        viewsNumber = findViewById(R.id.stories_read);
        storyNumber = findViewById(R.id.stories_posted);
        viewedNumber = findViewById(R.id.views_received);
        upvotesNumber = findViewById(R.id.upvotes_given);
        upvotedNumber = findViewById(R.id.upvotes_received);
        inviteFriendButton = findViewById(R.id.invite_friends_button);
        inviteFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inviteFriend();
            }
        });

        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        username = mAuth.getUid();

        if(username != null) {
            loadFields();
        } else {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            Intent login = new Intent(this, Login.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(login);

            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.option_change_password) {
            Intent changePassword = new Intent(this, ChangePassword.class);
            startActivity(changePassword);
        } else if(item.getItemId() == R.id.option_view_user_stories) {
            Intent viewMyStories = new Intent(this, UserStories.class);
            startActivity(viewMyStories);
        } else if(item.getItemId() == R.id.option_view_bookmarked_stories) {
            Intent viewBookmarkedStories = new Intent(this, BookmarkedStories.class);
            startActivity(viewBookmarkedStories);
        } else if(item.getItemId() == R.id.option_import_account) {
            Intent importOldAccount = new Intent(this, ImportOldAccount.class);
            startActivity(importOldAccount);
        }

        return true;
    }

    private void loadFields() {

        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                usernameField.setText(dataSnapshot.child("users").child(username).child("Username").getValue(String.class));
                emailField.setText(dataSnapshot.child("users").child(username).child("Email").getValue(String.class));
                //distanceNumber.setText(dataSnapshot.child("users").child(username)
                                        //.child("Distance").getValue(Integer.class).toString());

                int views = 0;
                int stories = 0;
                int viewed = 0;
                int upvoted = 0;
                int upvotes = 0;

                if(dataSnapshot.child("users").child(username).hasChild("stories")) {
                    for(DataSnapshot ds : dataSnapshot.child("users").child(username).child("stories").getChildren()){
                        stories++;

                        String storyKey = ds.getKey();

                        if(dataSnapshot.child("stories").child(storyKey).exists()) {
                            viewed = viewed + dataSnapshot.child("stories").child(storyKey).child("Views").getValue(Integer.class);
                            upvoted = upvoted + dataSnapshot.child("stories").child(storyKey).child("Votes").getValue(Integer.class);
                        }
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

    private void inviteFriend(){
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = "Check this out! I've found cool stuff with this app. I think you'll like it " + "https://projectboredinc.wordpress.com/download/";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Cool unBORED!");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share the fun with..."));
    }


}
