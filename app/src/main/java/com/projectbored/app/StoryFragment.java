package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.Date;
import java.util.Locale;


public class StoryFragment extends Fragment {

    String storyKey;
    String username;

    ImageView imageView;
    ImageButton upVoteButton;
    ImageButton downVoteButton;
    ImageButton shareButton;
    TextView voteNumber;
    TextView viewNumber;
    TextView storyCaption;
    TextView featuredText;
    TextView dateText;
    Button reportStoryButton;

    int storyVotes;
    int storyViews;

    DatabaseReference mDataRef;
    DatabaseReference mVotesRef;
    DatabaseReference mViewsRef;

    StorageReference mStorageRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle storyDetails = getArguments();
        storyKey = storyDetails.getString("key");
        username = getUsername();

        mDataRef = FirebaseDatabase.getInstance().getReference();

        mStorageRef = FirebaseStorage.getInstance().getReference();



        loadStoryDetails(storyDetails);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_show_story, container, false);
        imageView = view.findViewById(R.id.imageView);

        upVoteButton = view.findViewById(R.id.upVoteButton);
        upVoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upVote();
            }
        });

        downVoteButton = view.findViewById(R.id.downVoteButton);
        downVoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downVote();
            }
        });

        shareButton = view.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareFunction();
            }
        });

        voteNumber = view.findViewById(R.id.voteNumber);
        viewNumber =  view.findViewById(R.id.viewNumber);

        storyCaption =  view.findViewById(R.id.storyCaption);
        featuredText = view.findViewById(R.id.featuredText);
        dateText = view.findViewById(R.id.dateText);

        reportStoryButton =  view.findViewById(R.id.reportstory);
        reportStoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportStory();
            }
        });

        addView();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

    }

    public void addView(){
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

        mDataRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!(dataSnapshot.child("ReadStories").hasChild(storyKey))){
                    dataSnapshot.child("ReadStories").child(storyKey).getRef().setValue(storyKey);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    // stuff the buttons do when clicked -hy

    public void reportStory(){
        mDataRef.child("stories").child(storyKey).child("Flagged").setValue(true);
        Toast.makeText(getActivity(), "Story flagged.", Toast.LENGTH_SHORT).show();
    }

    public void upVote(){
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("stories").child(storyKey).getValue() != null) {
                    int votes = dataSnapshot.child("stories").child(storyKey)
                            .child("Votes").getValue(Integer.class);
                    if(dataSnapshot.child("stories").child(storyKey)
                            .child("Upvoters").hasChild(getUsername())) {
                        votes--;
                        dataSnapshot.child("stories").child(storyKey)
                                .child("Upvoters").child(getUsername()).getRef().setValue(null);

                        dataSnapshot.child("users").child(getUsername()).child("UpvotedStories")
                                .child(storyKey).getRef().setValue(null);

                    } else if(dataSnapshot.child("stories").child(storyKey)
                            .child("Downvoters").hasChild(getUsername())) {
                        votes = votes + 2;
                        dataSnapshot.child("stories").child(storyKey)
                                .child("Downvoters").child(getUsername()).getRef().setValue(null);
                        dataSnapshot.child("stories").child(storyKey)
                                .child("Upvoters").child(getUsername()).getRef().setValue(getUsername());

                        dataSnapshot.child("users").child(getUsername()).child("DownvotedStories")
                                .child(storyKey).getRef().setValue(null);
                        dataSnapshot.child("users").child(getUsername()).child("UpvotedStories")
                                .child(storyKey).getRef().setValue(storyKey);
                    } else {
                        votes++;
                        dataSnapshot.child("stories").child(storyKey)
                                .child("Upvoters").child(getUsername()).getRef().setValue(getUsername());

                        dataSnapshot.child("users").child(getUsername()).child("UpvotedStories")
                                .child(storyKey).getRef().setValue(storyKey);
                    }
                    dataSnapshot.child("stories").child(storyKey).child("Votes").getRef().setValue(votes);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void downVote(){
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("stories").child(storyKey).getValue() != null) {
                    int votes = dataSnapshot.child("stories").child(storyKey)
                            .child("Votes").getValue(Integer.class);
                    if(dataSnapshot.child("stories").child(storyKey)
                            .child("Downvoters").hasChild(getUsername())) {
                        votes++;
                        dataSnapshot.child("stories").child(storyKey).child("Downvoters")
                                .child(getUsername()).getRef().setValue(null);

                        dataSnapshot.child("users").child(getUsername()).child("DownvotedStories")
                                .child(storyKey).getRef().setValue(null);
                    } else if(dataSnapshot.child("stories").child(storyKey)
                            .child("Upvoters").hasChild(getUsername())) {
                        votes = votes - 2;
                        dataSnapshot.child("stories").child(storyKey).child("Upvoters")
                                .child(getUsername()).getRef().setValue(null);
                        dataSnapshot.child("stories").child(storyKey).child("Downvoters")
                                .child(getUsername()).getRef().setValue(getUsername());

                        dataSnapshot.child("users").child(getUsername()).child("UpvotedStories")
                                .child(storyKey).getRef().setValue(null);
                        dataSnapshot.child("users").child(getUsername()).child("DownvotedStories")
                                .child(storyKey).getRef().setValue(storyKey);
                    } else {
                        votes--;
                        dataSnapshot.child("stories").child(storyKey)
                                .child("Downvoters").child(getUsername()).getRef().setValue(getUsername());

                        dataSnapshot.child("users").child(getUsername()).child("DownvotedStories")
                                .child(storyKey).getRef().setValue(storyKey);
                    }
                    dataSnapshot.child("stories").child(storyKey).child("Votes").getRef().setValue(votes);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void shareFunction(){
        // this is the sharing code, it might not work -hy
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

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
                    boolean isFeatured = dataSnapshot.child("Featured").getValue(boolean.class);

                    if(isFeatured) {
                        featuredText.setText(R.string.featured_story);
                    }

                    if(uri != null && caption != null){
                        StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(uri);

                        //Load story image into image view.
                        Glide.with(getActivity()).load(mStorageRef).into(imageView);

                        storyCaption.setText(caption);
                    }

                    int storyDay = dataSnapshot.child("DateTime").child("date").getValue(Integer.class);
                    int storyMonth = 1+ dataSnapshot.child("DateTime").child("month").getValue(Integer.class);
                    int storyYear = 1900 + dataSnapshot.child("DateTime").child("year").getValue(Integer.class);

                    DateCreator storyDateCreator = new DateCreator(storyDay, storyMonth, storyYear);
                    dateText.setText(storyDateCreator.getDateString());

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Failed to load story data.", Toast.LENGTH_SHORT).show();
                }
            });

            mDataRef.child("stories").child(storyKey).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot != null){
                        int votes = 0;
                        if(dataSnapshot.child("Votes").getValue() != null) {
                            votes = dataSnapshot.child("Votes").getValue(Integer.class);
                        }
                        storyVotes = votes;
                        voteNumber.setText(String.format(new Locale("en", "US"),"%d",votes));

                        // added this for views lel
                        int views = 0;
                        if(dataSnapshot.child("Views").getValue() != null) {
                            views = dataSnapshot.child("Views").getValue(Integer.class);
                        }
                        storyViews = views;
                        viewNumber.setText(String.format(new Locale("en","US"), "%d", views));
                    } else {
                        voteNumber.setText(String.format(new Locale("en", "US"), "%d", 0));
                        viewNumber.setText(String.format(new Locale("en", "US"), "%d", 0));
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Story does not exist.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i  = new Intent(getActivity(), MapsActivityCurrentPlace.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                    });
            builder.create();
        }
    }

    private String getUsername() {
        return getArguments().getString("username");
    }




}
