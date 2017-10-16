package com.projectbored.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

public class ShowStory extends AppCompatActivity {

    Button upVoteButton;
    Button downVoteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_story);

        Button upVoteButton = (Button)findViewById(R.id.upVoteButton);
        
        //tbc

        }
    }

    // creating the menu that launches backToMap method

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

    // backToMap method (that just goes back to map i guess lol)
    // idk if this works

    private void backToMap() {
        // to fill in with code
        // i think this is suppose to open the map activity
        Intent intent = new Intent(this, MapsActivityCurrentPlace.class);
        startActivity(intent);
    }

}
