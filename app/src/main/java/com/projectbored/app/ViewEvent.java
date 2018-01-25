package com.projectbored.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

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
    Bundle eventDetails;

    private DatabaseReference mDataRef;
    private TextView titleText, descriptionText, dateText;
    private TimePicker timeView;
    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        eventDetails = getIntent().getExtras();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        titleText = findViewById(R.id.event_title);
        descriptionText = findViewById(R.id.event_description);
        dateText = findViewById(R.id.date_text);
        timeView = findViewById(R.id.event_time);
        timeView.setEnabled(false);
        imageView = findViewById(R.id.event_image);

        loadEventDetails();
    }

    private void loadEventDetails() {
        String eventKey = eventDetails.getString("key");
        mDataRef.child("events").child(eventKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String eventTitle = dataSnapshot.child("Title").getValue(String.class);
                String eventDescription = dataSnapshot.child("Description").getValue(String.class);
                String imageUri = dataSnapshot.child("URI").getValue(String.class);
                long eventTimeMillis = dataSnapshot.child("Expires").getValue(Long.class);

                Calendar timeNow = Calendar.getInstance();
                Calendar eventTime = Calendar.getInstance();
                eventTime.setTimeInMillis(eventTimeMillis);

                titleText.setText(eventTitle);
                descriptionText.setText(eventDescription);
                if(timeNow.get(Calendar.DAY_OF_MONTH) < eventTime.get(Calendar.DAY_OF_MONTH)) {
                    dateText.setText(R.string.tomorrow);
                } else {
                    dateText.setText(R.string.today);
                }

                StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUri);
                Glide.with(ViewEvent.this).using(new FirebaseImageLoader()).load(mStorageRef).into(imageView);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
