package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ShowMultipleStories extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";
    Bundle stories;
    String username;

    DatabaseReference mDataRef;

    ViewPager storiesPager;
    String[] storyKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_multiple_stories);

        mDataRef = FirebaseDatabase.getInstance().getReference();
        username = getSharedPreferences(PREFS_NAME, 0).getString("Username", "");

        stories = getIntent().getExtras();
        storyKeys = stories.getString("key").split(",");

        storiesPager = (ViewPager)findViewById(R.id.stories_pager);
        storiesPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        showStories();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_story_menu, menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final int storyPosition = storiesPager.getCurrentItem();
        final MenuItem deleteStoryOption = menu.findItem(R.id.option_delete_story);
        mDataRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("stories").hasChild(storyKeys[storyPosition])) {
                    deleteStoryOption.setVisible(true);
                } else {
                    deleteStoryOption.setVisible(false);
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
        if(item.getItemId() == R.id.option_back_to_map) {
            finish();
        } else if(item.getItemId() == R.id.option_delete_story) {
            deleteStory();
        }

        return true;
    }

    private void deleteStory() {
        int storyPosition = storiesPager.getCurrentItem();
        String storyKey = storyKeys[storyPosition];

        Intent delete = new Intent(getApplicationContext(), StoryDeleter.class);
        Bundle storyDetails = new Bundle();
        storyDetails.putString("key", storyKey);
        storyDetails.putString("Username", username);
        delete.putExtras(storyDetails);
        startActivity(delete);
        finish();
    }

    private void showStories() {
        FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                StoryFragment fragment = new StoryFragment();

                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                String myUsername = settings.getString("Username", "");

                Bundle storyDetails = new Bundle();
                storyDetails.putString("key", storyKeys[position]);
                storyDetails.putString("username", myUsername);

                fragment.setArguments(storyDetails);
                return fragment;
            }

            @Override
            public int getCount() {
                int numStories = 0;
                for(String key : storyKeys) {
                    numStories++;
                }
                return numStories;
            }

        };

        int numStories = 0;
        for(String key : storyKeys) {
            adapter.instantiateItem(storiesPager, numStories);
            numStories++;
        }




        storiesPager.setAdapter(adapter);

    }
}
