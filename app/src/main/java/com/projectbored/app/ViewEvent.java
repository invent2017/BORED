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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth;
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
    private FirebaseAuth mAuth;
    private TextView titleText, descriptionText, dateText, timeView, interestedNumber;
    //private ImageView imageView;
    private Button interestedButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        username = mAuth.getUid();

        eventDetails = getIntent().getExtras();
        getEventKey();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        titleText = findViewById(R.id.event_title);
        descriptionText = findViewById(R.id.event_description);
        dateText = findViewById(R.id.date_text);
        timeView = findViewById(R.id.event_time);
        interestedButton = findViewById(R.id.interested_button);
        interestedNumber = findViewById(R.id.interested_number);
        //imageView = findViewById(R.id.event_image);

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

    private void indicateInterest(DataSnapshot dataSnapshot) {
        if(dataSnapshot.child("EventsInterested").hasChild(eventKey)) {
            Toast.makeText(ViewEvent.this, "You have already indicated your interest for this event.",
                    Toast.LENGTH_SHORT).show();
        } else {
            mDataRef.child("users").child(username).child("EventsNotInterested").child(eventKey).removeValue();
            mDataRef.child("users").child(username).child("EventsInterested").child(eventKey).setValue(true);
            int numInterested = dataSnapshot.child("events").child(eventKey).child("Interested").getValue(Integer.class);
            mDataRef.child("events").child(eventKey).child("Interested").setValue(numInterested + 1);
            //TODO: Should we allow event creators to customise this message?
            Toast.makeText(ViewEvent.this, "Thank you for your participation in this event!",
                    Toast.LENGTH_SHORT).show();
            interestedButton.setText(R.string.event_already_interested);
        }
    }

    private void loadEventDetails() {
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("users").child("username").child("EventsInterested").hasChild(eventKey)) {
                    interestedButton.setText(R.string.event_already_interested);
                } else {
                    interestedButton.setText(R.string.event_interested);
                }

                String eventTitle = dataSnapshot.child("events").child(eventKey).child("Title").getValue(String.class);
                String eventDescription = dataSnapshot.child("events").child(eventKey).child("Description").getValue(String.class);
                int numInterested = dataSnapshot.child("events").child(eventKey).child("Interested").getValue(Integer.class);
                //String imageUri = dataSnapshot.child("events").child(eventKey).child("URI").getValue(String.class);
                long eventTimeMillis = dataSnapshot.child("events").child(eventKey).child("ExpiryTime").getValue(Long.class);

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
                interestedNumber.setText(numInterested);

                //StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUri);
                //Glide.with(ViewEvent.this).using(new FirebaseImageLoader()).load(mStorageRef).into(imageView);

                interestedButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        indicateInterest(dataSnapshot);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
