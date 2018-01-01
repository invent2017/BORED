package com.projectbored.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.gc.materialdesign.views.ButtonFlat;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

// tutorial from https://code.tutsplus.com/tutorials/creating-onboarding-screens-for-android-apps--cms-24465

public class Onboarding extends FragmentActivity {
    private static final String PREFS_NAME = "UserDetails";

    ViewPager pager;
    SmartTabLayout indicator;
    ButtonFlat skip;
    ButtonFlat next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0 : return new OnboardingFragment1();
                    case 1 : return new OnboardingFragment2();
                    case 2 : return new OnboardingFragment3();
                    default: return null;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };

        pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);
        indicator = findViewById(R.id.indicator);

        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if(position == 2){
                    skip.setVisibility(View.GONE);
                    next.setText("Done");
                } else {
                    skip.setVisibility(View.VISIBLE);
                    next.setText("Next");
                }
            }
        });

        skip = findViewById(R.id.skip);
        next = findViewById(R.id.next);

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pager.getCurrentItem() == 2) { // The last screen
                    finishOnboarding();
                } else {
                    pager.setCurrentItem(
                            pager.getCurrentItem() + 1,
                            true
                    );
                }
            }
        });

    }

    private void finishOnboarding() {

        // Get the shared preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);

        // Set onboarding_complete to true
        preferences.edit().putBoolean("onboarding_complete",true).apply();


        Intent map = new Intent(this, MapsActivityCurrentPlace.class);
        map.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(map);


        // Close the OnboardingActivity
        finish();
    }

}
