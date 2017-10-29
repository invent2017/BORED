package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;

// still need to make ImageView retrieve image and textView retrieve caption
// still need to let upvote/downvote change the thingies in the database possibly so they work

public class ShowStory extends AppCompatActivity implements View.OnClickListener {

    ImageView imageView;
    ImageButton upVoteButton;
    ImageButton downVoteButton;
    ImageButton shareButton;
    TextView voteNumber;
    TextView storyCaption;

    boolean upvoteClicked = false;
    boolean downvoteClicked = false;

    int storyVotes;

    DatabaseReference mStoryRef;
    DatabaseReference mVotesRef;

    Bundle storyDetails;

    // onCreate method here -hy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_story);

        mStoryRef = FirebaseDatabase.getInstance().getReference();

        imageView = (ImageView)findViewById(R.id.imageView);

        upVoteButton = (ImageButton)findViewById(R.id.upVoteButton);
        upVoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upVote();
            }
        });

        downVoteButton = (ImageButton)findViewById(R.id.downVoteButton);
        downVoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downVote();
            }
        });

        shareButton = (ImageButton)findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareFunction();
            }
        });

        voteNumber = (TextView) findViewById(R.id.voteNumber);

        storyCaption = (TextView) findViewById(R.id.storyCaption);

        storyDetails = getIntent().getExtras();

        loadStoryDetails(storyDetails);
        }


    // stuff the buttons do when clicked -hy

    public void upVote(){
        if(upvoteClicked) {
            storyVotes--;
            upvoteClicked = false;
        } else if(downvoteClicked) {
            storyVotes = storyVotes + 2;
            upvoteClicked = true;
            downvoteClicked = false;
        } else {
            storyVotes++;
            upvoteClicked = true;
        }

        updateVotes();
    }

    public void downVote(){
        if (downvoteClicked) {
            storyVotes++;
            downvoteClicked = false;
        } else if(upvoteClicked) {
            storyVotes = storyVotes - 2;
            downvoteClicked = true;
            upvoteClicked = false;
        } else {
            storyVotes--;
            downvoteClicked = true;
        }

        updateVotes();
    }

    public void shareFunction(){
        // this is the sharing code, it might not work -hy
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        // i don't actually know what the subject or whatnot is so heh
        String shareBody = "Check out this cool story on bored!";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Cool BORED! story");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upVoteButton: {
                upVote();
                break;
            }

            case R.id.downVoteButton: {
                downVote();
                break;
            }

            case R.id.shareButton: {
                shareFunction();
                break;
            }
        }
    }

    // creating the menu that launches backToMap method -hy

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_story_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.projectbored.app.R.id.option_back_to_map) {
            backToMap();
        }
        return true;
    }

    private void loadStoryDetails(Bundle storyDetails){
        final String storyKey = storyDetails.getString("key");

        if(storyKey != null) {
            mVotesRef= FirebaseDatabase.getInstance().getReference().child("stories").child(storyKey).child("Votes");
            mStoryRef.child("stories").child(storyKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String uri = dataSnapshot.child("URI").getValue(String.class);
                    String caption = dataSnapshot.child("Caption").getValue(String.class);


                    if(uri != null && caption != null){
                        StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(uri);

                        //Load story image into image view.
                        Glide.with(ShowStory.this).using(new FirebaseImageLoader()).load(mStorageRef).into(imageView);

                        storyCaption.setText(caption);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(ShowStory.this, "Failed to load story data.", Toast.LENGTH_SHORT).show();
                }
            });

            mStoryRef.child("stories").child(storyKey).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int votes = dataSnapshot.child("Votes").getValue(Integer.class);

                    storyVotes = votes;
                    voteNumber.setText(String.format(new Locale("en", "US"),"%d",votes));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            builder.setMessage("Story does not exist.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i  = new Intent(ShowStory.this, MapsActivityCurrentPlace.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                    });
            builder.create();
        }


    }

    private void updateVotes() {
        mVotesRef.setValue(storyVotes);
    }

    // backToMap method (that just goes back to map i guess lol) -hy
    // idk if this works -hy

    private void backToMap() {
        // to fill in with code -hy
        // i think this is suppose to open the map activity -hy
        Intent intent = new Intent(this, MapsActivityCurrentPlace.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //I think you need to add this to go back to the main activity otherwise it will lag -LH
        startActivity(intent);
    }

}
