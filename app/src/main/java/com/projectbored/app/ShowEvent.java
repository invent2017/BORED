package com.projectbored.app;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ShowEvent extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_event);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_event_menu, menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.option_back_to_map) {
            finish();
        }
        /*else if (item.getItemId() == R.id.option_delete_event) {
            deleteEvent();
        }*/
        return true;
    }


}
