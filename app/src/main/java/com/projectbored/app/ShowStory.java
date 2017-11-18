package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;

public class ShowStory extends AppCompatActivity implements View.OnClickListener {
    private static final String PREFS_NAME = "UserDetails";

    ImageView imageView;
    ImageButton upVoteButton;
    ImageButton downVoteButton;
    ImageButton shareButton;
    TextView voteNumber;
    TextView viewNumber;
    TextView storyCaption;
    Button reportStoryButton;

    int storyVotes;
    int storyViews;

    DatabaseReference mDataRef;
    DatabaseReference mVotesRef;
    DatabaseReference mViewsRef;

    StorageReference mStorageRef;

    Bundle storyDetails;
    boolean loggedIn;

    // onCreate method here -hy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_story);

        setTitle("Story");

        mDataRef = FirebaseDatabase.getInstance().getReference();

        mStorageRef = FirebaseStorage.getInstance().getReference();

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
        viewNumber = (TextView) findViewById(R.id.viewNumber);

        storyCaption = (TextView) findViewById(R.id.storyCaption);

        storyDetails = getIntent().getExtras();

        reportStoryButton = (Button) findViewById(R.id.reportstory);
        reportStoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportStory();
            }
        });

        loadStoryDetails(storyDetails);
        loggedIn = storyDetails.getBoolean("Logged in");


        addView();

        //trying to do emoji things

    }

    public void addView(){
        final String storyKey = storyDetails.getString("key");
        DatabaseReference mStoryRef = FirebaseDatabase.getInstance().getReference().child("stories").child(storyKey).child("Views");
        mStoryRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() != null) {
                    int storyViews = mutableData.getValue(Integer.class);
                    ++storyViews;

                    mutableData.setValue(storyViews);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

        if(loggedIn){
            String username = getUsername();
            mDataRef.child("stories").child(storyKey).child("Viewers").child(username).setValue(username);
            mDataRef.child("users").child(username).child("Views").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int views = dataSnapshot.getValue(Integer.class);
                    dataSnapshot.getRef().setValue(++views);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    // stuff the buttons do when clicked -hy

    public void reportStory(){
        String storyKey = storyDetails.getString("key");
        mDataRef.child("stories").child(storyKey).child("Flagged").setValue(true);
        Toast.makeText(this, "Story flagged.", Toast.LENGTH_SHORT).show();
   }

    public void upVote(){
        if(loggedIn){
            mDataRef.child("stories").child(storyDetails.getString("key")).runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() != null) {
                        int votes = mutableData.child("Votes").getValue(Integer.class);
                        if(mutableData.child("Upvoters").hasChild(getUsername())) {
                            votes--;
                            mutableData.child("Upvoters").child(getUsername()).setValue(null);
                        } else if(mutableData.child("Downvoters").hasChild(getUsername())) {
                            votes = votes + 2;
                            mutableData.child("Downvoters").child(getUsername()).setValue(null);
                            mutableData.child("Upvoters").child(getUsername()).setValue(getUsername());
                        } else {
                            votes++;
                            mutableData.child("Upvoters").child(getUsername()).setValue(getUsername());
                        }
                        mutableData.child("Votes").setValue(votes);
                    }

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                }
            });

            mDataRef.child("users").child(getUsername()).child("Upvotes").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int userVotes = dataSnapshot.getValue(Integer.class);
                    dataSnapshot.getRef().setValue(++userVotes);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Toast.makeText(this, "You must log in to upvote stories.", Toast.LENGTH_SHORT).show();
        }

    }

    public void downVote(){
        if(loggedIn){
            mDataRef.child("stories").child(storyDetails.getString("key")).runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() != null) {
                        int votes = mutableData.child("Votes").getValue(Integer.class);
                        if(mutableData.child("Downvoters").hasChild(getUsername())) {
                            votes++;
                            mutableData.child("Downvoters").child(getUsername()).setValue(null);
                        } else if(mutableData.child("Upvoters").hasChild(getUsername())) {
                            votes = votes - 2;
                            mutableData.child("Upvoters").child(getUsername()).setValue(null);
                            mutableData.child("Downvoters").child(getUsername()).setValue(getUsername());
                        } else {
                            votes--;
                            mutableData.child("Downvoters").child(getUsername()).setValue(getUsername());
                        }
                        mutableData.child("Votes").setValue(votes);
                    }

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                }
            });

        } else {
            Toast.makeText(this, "You must log in to downvote stories.", Toast.LENGTH_SHORT).show();
        }
    }

    public void shareFunction(){
        // this is the sharing code, it might not work -hy
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        String storyKey = storyDetails.getString("key");
        // i don't actually know what the subject or whatnot is so heh
        // need to add things to shareBody that links to the story or sth like that
        String shareBody = "Check out this cool story on bored!\n" + "http://projectboredinc.wordpress.com/story/" + storyKey;
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(loggedIn) {
            final MenuItem deleteStoryOption = menu.findItem(R.id.option_delete_story);
            mDataRef.child("users").child(getUsername()).child("stories").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild(storyDetails.getString("key"))) {
                        deleteStoryOption.setVisible(true);
                    } else {
                        deleteStoryOption.setVisible(false);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.projectbored.app.R.id.option_back_to_map) {
            backToMap();
        }

        if(item.getItemId() == R.id.option_delete_story) {
            deleteStory();
        }
        return true;
    }

    private void deleteStory(){
        Intent delete = new Intent(getApplicationContext(), StoryDeleter.class);
        storyDetails.putString("Username", getUsername());
        delete.putExtras(storyDetails);
        delete.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(delete);
    }


    private void loadStoryDetails(Bundle storyDetails){
        final String storyKey = storyDetails.getString("key");

        if(storyKey != null) {
            mViewsRef= FirebaseDatabase.getInstance().getReference().child("stories").child(storyKey).child("Views");
            mVotesRef= FirebaseDatabase.getInstance().getReference().child("stories").child(storyKey).child("Votes");
            mDataRef.child("stories").child(storyKey).addListenerForSingleValueEvent(new ValueEventListener() {
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

            mDataRef.child("stories").child(storyKey).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        int votes = dataSnapshot.child("Votes").getValue(Integer.class);
                        storyVotes = votes;
                        voteNumber.setText(String.format(new Locale("en", "US"),"%d",votes));

                        // added this for views lel
                        int views = dataSnapshot.child("Views").getValue(Integer.class);
                        storyViews = views;
                        viewNumber.setText(String.format(new Locale("en","US"), "%d", views));
                    } catch(NullPointerException e){
                        voteNumber.setText(String.format(new Locale("en", "US"), "%d", 0));
                        viewNumber.setText(String.format(new Locale("en", "US"), "%d", 0));
                    }

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

    private String getUsername() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getString("Username", "");
    }

    private void backToMap() {
        Intent intent = new Intent(this, MapsActivityCurrentPlace.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}
