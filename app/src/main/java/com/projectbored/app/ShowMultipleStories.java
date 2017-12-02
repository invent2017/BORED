package com.projectbored.app;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;

public class ShowMultipleStories extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";
    Bundle stories;

    ViewPager storiesPager;
    String[] storyKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_multiple_stories);

        stories = getIntent().getExtras();
        storyKeys = stories.getString("key").split(",");

        storiesPager = (ViewPager)findViewById(R.id.stories_pager);
        showStories();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_story_menu, menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        return true;
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
