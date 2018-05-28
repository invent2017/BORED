package com.projectbored.app;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;

public class ViewEvent extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    Bundle eventDetails;
    private String eventKey;
    private String username;

    private DatabaseReference mDataRef;
    private TextView titleText, descriptionText, dateText;
    private TextView timeView;
    //private ImageView imageView;
    private Button interestedButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        username = getSharedPreferences(PREFS_NAME, 0).getString("Username", "");

        eventDetails = getIntent().getExtras();
        getEventKey();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        titleText = findViewById(R.id.event_title);
        descriptionText = findViewById(R.id.event_description);
        dateText = findViewById(R.id.date_text);
        timeView = findViewById(R.id.event_time);
        //imageView = findViewById(R.id.event_image);
        interestedButton = findViewById(R.id.interested_button);
        interestedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDataRef.child("users").child(username).child("EventsNotInterested").child(eventKey).removeValue();
                mDataRef.child("users").child(username).child("EventsInterested").child(eventKey).setValue(true);

                //TODO: Should we allow event creators to customise this message?
                Toast.makeText(ViewEvent.this, "Thank you for your participation in this event!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        loadEventDetails();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_event_menu, menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        return true;
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

    private void getEventKey() {
        eventKey = eventDetails.getString("key");
    }

    private void loadEventDetails() {
        mDataRef.child("events").child(eventKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String eventTitle = dataSnapshot.child("Title").getValue(String.class);
                String eventDescription = dataSnapshot.child("Description").getValue(String.class);
                //String imageUri = dataSnapshot.child("URI").getValue(String.class);
                long eventTimeMillis = dataSnapshot.child("ExpiryTime").getValue(Long.class);

                Calendar timeNow = Calendar.getInstance();
                Calendar eventTime = Calendar.getInstance();
                eventTime.setTimeInMillis(eventTimeMillis);

                timeView.setText(new TimeStringGenerator(eventTimeMillis).getTimeString());

                titleText.setText(eventTitle);
                descriptionText.setText(eventDescription);
                if(timeNow.get(Calendar.DAY_OF_MONTH) < eventTime.get(Calendar.DAY_OF_MONTH)) {
                    dateText.setText(R.string.tomorrow);
                } else {
                    dateText.setText(R.string.today);
                }

                //StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUri);
                //Glide.with(ViewEvent.this).using(new FirebaseImageLoader()).load(mStorageRef).into(imageView);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
