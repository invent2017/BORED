package com.projectbored.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_ACTION_SEND;

/**
 * This activity shows story details (image, caption, hashtags)
 * >> This occurs when stories are accessed via map
 * >> OR when stories are accssed via bookmarked/my stories list
 * Interactive on-activity functions: Comments, upvote, downvote, report, share
 * Interactive multi-activities functions: Bookmark story, show story on map
 * Passive functions: View count
 * Navigating functions: Back-to-map
 */

public class ShowStory extends AppCompatActivity implements View.OnClickListener {
    Bundle storyDetails;

    private static final String PREFS_NAME = "UserDetails";
    private String STORY_KEY;
    private String username;

    ArrayList<String> comments;

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
    ListView commentsList;
    EditText commentInput;

    int storyVotes;
    int storyViews;

    DatabaseReference mDataRef;
    DatabaseReference mVotesRef;
    DatabaseReference mViewsRef;

    StorageReference mStorageRef;

    // onCreate method here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_story);

        // Layout things
        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // Prevents keyboard from automoatically appearing
        // also did focusableInTouchMode in layout to prevent activity from automatically scrolling
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        storyDetails = getIntent().getExtras();
        STORY_KEY = storyDetails.getString("key");
        username = getSharedPreferences(PREFS_NAME, 0).getString("Username", "");
        if(username.equals("")){
            logout();
        }

        comments = new ArrayList<>();

        mDataRef = FirebaseDatabase.getInstance().getReference();

        mStorageRef = FirebaseStorage.getInstance().getReference();

        imageView = findViewById(R.id.imageView);

        upVoteButton = findViewById(R.id.upVoteButton);
        upVoteButton.setOnClickListener(this);

        downVoteButton = findViewById(R.id.downVoteButton);
        downVoteButton.setOnClickListener(this);
        // RAWR Test: No downvote
        downVoteButton.setVisibility(View.INVISIBLE);

        shareButton = findViewById(R.id.shareButton);
        shareButton.setOnClickListener(this);

        reportStoryButton =  findViewById(R.id.reportstory);
        reportStoryButton.setOnClickListener(this);

        voteNumber = findViewById(R.id.voteNumber);
        viewNumber = findViewById(R.id.viewNumber);

        storyCaption = findViewById(R.id.storyCaption);
        featuredText = findViewById(R.id.featuredText);
        dateText = findViewById(R.id.dateText);

        commentsList = findViewById(R.id.comments_list);
        setCommentsListHeight(commentsList);
        commentsList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        loadComments();

        commentInput = findViewById(R.id.comment_text);
        commentInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(i == IME_ACTION_SEND || i == KEYCODE_ENTER) {
                    String commentString = commentInput.getText().toString().trim();
                    if(!commentString.equals("")) {
                        postComment(commentString);
                    }

                    return true;
                } else {
                    return false;
                }
            }
        });

        loadStoryDetails(storyDetails);

        addView();
    }

    // Code to load the story image, caption, hashtag etc.
    private void loadStoryDetails(Bundle storyDetails){
        final String storyKey = storyDetails.getString("key");

        if(storyKey != null) {
            mViewsRef= FirebaseDatabase.getInstance().getReference().child("stories").child(storyKey).child("Views");
            mVotesRef= FirebaseDatabase.getInstance().getReference().child("stories").child(storyKey).child("Votes");
            mDataRef.child("stories").child(storyKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String uri = dataSnapshot.child("URI").getValue(String.class);
                        String caption = dataSnapshot.child("Caption").getValue(String.class);
                        boolean isFeatured = dataSnapshot.child("Featured").getValue(boolean.class);

                        if (isFeatured) {
                            featuredText.setText(R.string.featured_story);
                        }

                        if (uri != null && caption != null) {
                            StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(uri);

                            //Load story image into image view.
                            Glide.with(ShowStory.this).using(new FirebaseImageLoader()).load(mStorageRef).into(imageView);

                            storyCaption.setText(caption);
                        }

                        int storyDay = dataSnapshot.child("DateTime").child("date").getValue(Integer.class);
                        int storyMonth = 1 + dataSnapshot.child("DateTime").child("month").getValue(Integer.class);
                        int storyYear = 1900 + dataSnapshot.child("DateTime").child("year").getValue(Integer.class);

                        DateCreator storyDateCreator = new DateCreator(storyDay, storyMonth, storyYear);
                        dateText.setText(storyDateCreator.getDateString());

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

    // Adds comments
    public void postComment(String commentString) {
        Comment comment = new Comment(username, STORY_KEY, commentString);
        Map<String, Object> commentDetails = comment.toMap();
        String commentKey = mDataRef.push().getKey();
        mDataRef.child("comments").child(STORY_KEY).child(commentKey).setValue(commentDetails);
        commentInput.setText("");
    }


    // Shows comments in a list
    public void loadComments() {

        mDataRef.child("comments").child(STORY_KEY).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    comments.add(ds.child("String").getValue(String.class));
                }
                ArrayAdapter adapter = new ArrayAdapter<>(ShowStory.this, R.layout.comment_row, comments);
                commentsList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public static void setCommentsListHeight(ListView commentsList) {
        ListAdapter listAdapter = commentsList.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(commentsList.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, commentsList);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, AbsListView.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = commentsList.getLayoutParams();
        params.height = totalHeight + (commentsList.getDividerHeight() * (listAdapter.getCount() - 1));
        commentsList.setLayoutParams(params);
    }

    // Increase the number of times the story has been viewed
    public void addView(){
        DatabaseReference mStoryRef = FirebaseDatabase.getInstance().getReference().child("stories").child(STORY_KEY).child("Views");
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

        // Makes story read for user
        mDataRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!(dataSnapshot.child("ReadStories").hasChild(STORY_KEY))){
                    dataSnapshot.child("ReadStories").child(STORY_KEY).getRef().setValue(STORY_KEY);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    // stuff the buttons do when clicked:
    // upvote, downvote
    // share
    // report

    // Macro-structure on what happens when buttons are clicked
    public void onClick(View v) {
        switch (v.getId()) {
            // Button disappears and appears accordingly to indicate if you have voted or not (REMOVED FOR NOW)
            case R.id.upVoteButton:
                upVote();
                //upVoteButton.setVisibility(View.INVISIBLE);
                //downVoteButton.setVisibility(View.VISIBLE);
                break;

            case R.id.downVoteButton:
                downVote();
                //upVoteButton.setVisibility(View.VISIBLE);
                //downVoteButton.setVisibility(View.INVISIBLE);
                break;

            case R.id.shareButton:
                shareFunction();
                break;

            case R.id.reportstory:
                reportStory();
                break;

            default:
                throw new RuntimeException("Don't click this.");
        }
    }

    // Updates database where story is now indicated as flagged
    public void reportStory(){
        mDataRef.child("stories").child(STORY_KEY).child("Flagged").setValue(true);
        SingleToast.show(this, "Story flagged.", Toast.LENGTH_SHORT);
   }

    // Upvote code
    public void upVote(){
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("stories").child(STORY_KEY).getValue() != null) {
                    int votes = dataSnapshot.child("stories").child(STORY_KEY)
                            .child("Votes").getValue(Integer.class);

                    // If you have already upvoted before
                    if(dataSnapshot.child("stories").child(STORY_KEY)
                            .child("Upvoters").hasChild(username)) {

                        // removes your upvote if upvoted before
                        votes--;
                        dataSnapshot.child("stories").child(STORY_KEY)
                                .child("Upvoters").child(username).getRef().setValue(null);

                        dataSnapshot.child("users").child(username).child("UpvotedStories")
                                .child(STORY_KEY).getRef().setValue(null);

                    } else if(dataSnapshot.child("stories").child(STORY_KEY)
                            .child("Downvoters").hasChild(username)) {

                        // changes from downvote to upvote
                        votes = votes + 2;
                        dataSnapshot.child("stories").child(STORY_KEY)
                                .child("Downvoters").child(username).getRef().setValue(null);
                        dataSnapshot.child("stories").child(STORY_KEY)
                                .child("Upvoters").child(username).getRef().setValue(username);

                        dataSnapshot.child("users").child(username).child("DownvotedStories")
                                .child(STORY_KEY).getRef().setValue(null);
                        dataSnapshot.child("users").child(username).child("UpvotedStories")
                                .child(STORY_KEY).getRef().setValue(STORY_KEY);
                    } else {

                        // upvotes
                        votes++;
                        dataSnapshot.child("stories").child(STORY_KEY)
                                .child("Upvoters").child(username).getRef().setValue(username);

                        dataSnapshot.child("users").child(username).child("UpvotedStories")
                                .child(STORY_KEY).getRef().setValue(STORY_KEY);
                    }
                    dataSnapshot.child("stories").child(STORY_KEY).child("Votes").getRef().setValue(votes);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    // Downvote code
    public void downVote(){
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("stories").child(STORY_KEY).getValue() != null) {
                    int votes = dataSnapshot.child("stories").child(STORY_KEY)
                            .child("Votes").getValue(Integer.class);
                    if(dataSnapshot.child("stories").child(STORY_KEY)
                            .child("Downvoters").hasChild(username)) {

                        // removes downvote if downvoted before
                        votes++;
                        dataSnapshot.child("stories").child(STORY_KEY).child("Downvoters")
                                .child(username).getRef().setValue(null);

                        dataSnapshot.child("users").child(username).child("DownvotedStories")
                                .child(STORY_KEY).getRef().setValue(null);
                    } else if(dataSnapshot.child("stories").child(STORY_KEY)
                            .child("Upvoters").hasChild(username)) {

                        // changes upvote to downvote
                        votes = votes - 2;
                        dataSnapshot.child("stories").child(STORY_KEY).child("Upvoters")
                                .child(username).getRef().setValue(null);
                        dataSnapshot.child("stories").child(STORY_KEY).child("Downvoters")
                                .child(username).getRef().setValue(username);

                        dataSnapshot.child("users").child(username).child("UpvotedStories")
                                .child(STORY_KEY).getRef().setValue(null);
                        dataSnapshot.child("users").child(username).child("DownvotedStories")
                                .child(STORY_KEY).getRef().setValue(STORY_KEY);
                    } else {
                        votes--;
                        dataSnapshot.child("stories").child(STORY_KEY)
                                .child("Downvoters").child(username).getRef().setValue(username);

                        dataSnapshot.child("users").child(username).child("DownvotedStories")
                                .child(STORY_KEY).getRef().setValue(STORY_KEY);
                    }
                    dataSnapshot.child("stories").child(STORY_KEY).child("Votes").getRef().setValue(votes);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        mDataRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {


                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

    }

    // Share function enables user to share the story to friends.
    // Clicking on the link actually opens up the story in-app
    public void shareFunction(){
        // this is the sharing code
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        String storyKey = storyDetails.getString("key");
        String shareBody = "Check out this cool story on See GO!\n" + "http://projectboredinc.wordpress.com/story/" + storyKey;
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Cool See GO squawk");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    // Pressing back moves story back to map
    @Override
    public void onBackPressed() {
        finish();
    }

    // creating the menu that includes the following methods
    // view story location on map (for bookmarked stories)
    // delete
    // bookmark
    // back-to-map

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_story_menu, menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        return true;
    }

    // Determines visibility of menu depending on circumstances
    // (e.g. bookmarked? user's own story)
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(getIntent().getExtras().getBoolean("FromProfile", false)) {
            menu.findItem(R.id.option_view_on_map).setVisible(true);
            menu.findItem(R.id.option_back_to_map).setVisible(false);
        }

        final MenuItem deleteStoryOption = menu.findItem(R.id.option_delete_story);
        final MenuItem bookmarkStoryOption = menu.findItem(R.id.option_bookmark_story);
        mDataRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("stories").hasChild(STORY_KEY)) {
                    deleteStoryOption.setVisible(true);
                    bookmarkStoryOption.setVisible(false);
                } else {
                    deleteStoryOption.setVisible(false);
                    if(dataSnapshot.child("Bookmarked").hasChild(STORY_KEY)) {
                        bookmarkStoryOption.setVisible(false);
                    } else {
                        bookmarkStoryOption.setVisible(true);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.option_view_on_map) {
            showStoryLocation(STORY_KEY);
        } else if(item.getItemId() == R.id.option_delete_story) {
            deleteStory();
        } else if(item.getItemId() == R.id.option_bookmark_story) {
            bookmarkStory();
        } else if(item.getItemId() == R.id.option_back_to_map) {
            finish();
        }
        return true;
    }

    // Code for showing story location on map
    private void showStoryLocation(String storyKey) {
        Intent showLocation = new Intent(this, MapsActivityCurrentPlace.class);
        Bundle details = new Bundle();
        details.putString("UserStory", storyKey);
        showLocation.putExtras(details);
        startActivity(showLocation);

        finish();
    }

    // Code for deleting story
    private void deleteStory(){
        Intent delete = new Intent(getApplicationContext(), StoryDeleter.class);
        storyDetails.putString("Username", username);
        delete.putExtras(storyDetails);
        delete.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(delete);
        // RAWR Test Finish
        finish();
    }

    // Code for bookmarking story
    private void bookmarkStory() {
        String locationString = new StringBuilder().append(storyDetails.getDouble("Latitude"))
                .append(",").append(storyDetails.getDouble("Longitude")).toString();
        mDataRef.child("users").child(username).child("Bookmarked").child(STORY_KEY).setValue(locationString);

        SingleToast.show(this, "Story bookmarked.", Toast.LENGTH_SHORT);
        invalidateOptionsMenu();
    }


    // MISC Code

    private void updateVotes() {
        mVotesRef.setValue(storyVotes);
    }

    private void backToMap() {
        Intent backtoMap = new Intent(this, MapsActivityCurrentPlace.class);
        backtoMap.putExtras(storyDetails);
        startActivity(backtoMap);

        finish();
    }

    private void logout() {
        Intent logout = new Intent(this, Logout.class);
        startActivity(logout);
        Toast.makeText(this, "You have been logged out. Please log in again.", Toast.LENGTH_SHORT).show();

        finish();
    }

    // Code to prevent toast accumulation
    public static class SingleToast {

        private static Toast mToast;

        public static void show(Context context, String text, int duration) {
            if (mToast != null) mToast.cancel();
            mToast = Toast.makeText(context, text, duration);
            mToast.show();
        }
    }

}
