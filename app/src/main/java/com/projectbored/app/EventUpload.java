package com.projectbored.app;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Map;

public class EventUpload extends AppCompatActivity {
    Bundle eventSettings;

    String eventKey;

    private EditText titleField, descriptionField;
    private TextView eventDateText;
    private Spinner spinner;
    private TimePicker timePicker;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_upload);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        eventSettings = getIntent().getExtras();
        eventKey = mDataRef.push().getKey();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        titleField = findViewById(R.id.event_title);
        descriptionField = findViewById(R.id.event_description);

        eventDateText = findViewById(R.id.event_date_text);

        timePicker = findViewById(R.id.timePicker);

        spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.event_date_options,
                android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 0) {
                    DateCreator eventDateCreator = new DateCreator(Calendar.getInstance());

                    StringBuilder eventDateString = new StringBuilder();
                    eventDateString.append("Today, ").append(eventDateCreator.getDateString());
                    eventDateText.setText(eventDateString.toString());

                } else if(i == 1) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.DAY_OF_MONTH, 1 + calendar.get(Calendar.DAY_OF_MONTH));

                    DateCreator eventDateCreator = new DateCreator(calendar);

                    StringBuilder eventDateString = new StringBuilder();
                    eventDateString.append("Tomorrow, ").append(eventDateCreator.getDateString());
                    eventDateText.setText(eventDateString.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getMenuInflater().inflate(R.menu.add_event_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_upload_event) {
            uploadEvent();
        }
        return true;
    }

    private void uploadEvent() {
        Calendar timeNow = Calendar.getInstance();
        Calendar eventCalendar = Calendar.getInstance();
        int pos = spinner.getSelectedItemPosition();
        if(pos == 1) {
            eventCalendar.set(Calendar.DAY_OF_MONTH, 1 + eventCalendar.get(Calendar.DAY_OF_MONTH));
        }
        if (Build.VERSION.SDK_INT >= 23) {
            eventCalendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            eventCalendar.set(Calendar.MINUTE, timePicker.getMinute());
        } else {
            eventCalendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
            eventCalendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());
        }
        eventCalendar.set(Calendar.SECOND, 0);

        if(timeNow.getTimeInMillis() >= eventCalendar.getTimeInMillis()) {
            Toast.makeText(this, "Event has already expired.", Toast.LENGTH_SHORT).show();

        } else {

            Location eventLocation = new Location(LocationManager.GPS_PROVIDER);
            eventLocation.setLatitude(eventSettings.getDouble("Latitude"));
            eventLocation.setLongitude(eventSettings.getDouble("Longitude"));

            String eventTitle = titleField.getText().toString();
            String eventDescription = descriptionField.getText().toString();

            Event event = new Event(eventTitle, eventDescription, eventCalendar.getTimeInMillis(), eventLocation);
            String locationKey = event.getLocationString().replace('.', 'd');
            mDataRef.child("events").child(eventKey).updateChildren(event.toMap());
            mDataRef.child("locations").child(locationKey).child(eventKey).setValue(2);

            Toast.makeText(this, "Event added!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
