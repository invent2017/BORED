package com.projectbored.app;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

// still need to make ImageView retrieve image and textView retrieve caption
// still need to let upvote/downvote change the thingies in the database possibly so they work

public class ShowStory extends ActionBarActivity implements View.OnClickListener {

    ImageButton upVoteButton;
    ImageButton downVoteButton;
    ImageButton shareButton;

    // onCreate method here -hy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_story);

        ImageButton mClickUpVoteButton = (ImageButton)findViewById(R.id.upVoteButton);
        mClickUpVoteButton.setOnClickListener(this);
        ImageButton mClickDownVoteButton = (ImageButton)findViewById(R.id.downVoteButton);
        mClickDownVoteButton.setOnClickListener(this);
        ImageButton mClickShare = (ImageButton)findViewById(R.id.shareButton);
        mClickShare.setOnClickListener(this);

        }


    // stuff the buttons do when clicked -hy

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upVoteButton: {
                // do something for button 1 click
                break;
            }

            case R.id.downVoteButton: {
                // do something for button 2 click
                break;
            }

            case R.id.shareButton: {
                // this is the sharing code, it might not work -hy
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                // i don't actually know what the subject or whatnot is so heh
                String shareBody = "Check out this cool story on bored!";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Cool BORED! story");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
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
        startActivity(intent);
    }

}
