package com.projectbored.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

public class UserProfile extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    private TextView usernameField, emailField, distanceNumber, viewsNumber,
            storyNumber, viewedNumber, upvotesNumber, upvotedNumber;

    private DatabaseReference mDataRef;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        usernameField = (TextView)findViewById(R.id.username);
        emailField = (TextView)findViewById(R.id.email);
        distanceNumber = (TextView)findViewById(R.id.kilometeres_covered);
        viewsNumber = (TextView)findViewById(R.id.stories_read);
        storyNumber = (TextView)findViewById(R.id.stories_posted);
        viewedNumber = (TextView)findViewById(R.id.views_received);
        upvotesNumber = (TextView)findViewById(R.id.upvotes_given);
        upvotedNumber = (TextView)findViewById(R.id.upvotes_received);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        username = getSharedPreferences(PREFS_NAME, 0 ).getString("Username", "");

        if(username != null) {
            loadFields();
        } else {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadFields() {
        usernameField.setText(username);

        mDataRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                emailField.setText(dataSnapshot.child("Email").getValue(String.class));
                distanceNumber.setText(dataSnapshot.child("Distance").getValue(Integer.class).toString());
                viewsNumber.setText(dataSnapshot.child("Views").getValue(Integer.class).toString());

                int stories = 0;
                for(DataSnapshot ds : dataSnapshot.child("stories").getChildren()){
                    stories++;
                }
                storyNumber.setText(Integer.toString(stories));
                viewedNumber.setText(dataSnapshot.child("Viewed").getValue(Integer.class).toString());
                upvotesNumber.setText(dataSnapshot.child("Upvotes").getValue(Integer.class).toString());
                upvotedNumber.setText(dataSnapshot.child("Upvoted").getValue(Integer.class).toString());

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


}
