package com.projectbored.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.os.Bundle;
import android.widget.TextView;

// still need to make ImageView retrieve image and textView retrieve caption
// still need to let upvote/downvote change the thingies in the database possibly so they work

public class ShowStory extends AppCompatActivity implements View.OnClickListener {

    ImageView imageView;
    ImageButton upVoteButton;
    ImageButton downVoteButton;
    ImageButton shareButton;
    TextView voteNumber;
    TextView storyCaption;

    Story story;

    // onCreate method here -hy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_story);

        story = getStoryDetails();

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

        }


    // stuff the buttons do when clicked -hy

    public Story getStoryDetails() {
        Intent i = getIntent();
        return i.getParcelableExtra("Story");
    }

    public void upVote(){
        story.upVote();
    }

    public void downVote(){
        story.downVote();
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
