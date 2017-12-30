package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateAccount extends AppCompatActivity {
    public static final String PREFS_NAME = "UserDetails";

    public boolean loggedIn;
    private DatabaseReference mDataRef;

    private EditText usernameField;
    private EditText emailField;
    private EditText passwordField;

    private Button signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        usernameField = (EditText)findViewById(R.id.signUpUsername);
        emailField = (EditText)findViewById(R.id.signUpEmail);
        passwordField = (EditText)findViewById(R.id.signUpPassword);

        signUpButton = (Button)findViewById(R.id.signup_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent returnToMap = new Intent(this, MapsActivityCurrentPlace.class);
        returnToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToMap);
    }

    private void signUp() {
        if(usernameField.getText().toString().trim().isEmpty() || emailField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            Toast.makeText(this, R.string.error_field_required, Toast.LENGTH_SHORT).show();
        } else {
            final String username = usernameField.getText().toString();
            final String email = emailField.getText().toString();
            final String password = passwordField.getText().toString();

            String emailPattern = ("\\S+@\\w+\\.\\w+");
            Pattern pattern = Pattern.compile(emailPattern);
            Matcher matcher = pattern.matcher(email);
            if(matcher.matches()) {

                mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child(username).exists()) {
                            Toast.makeText(CreateAccount.this, R.string.error_existing_username, Toast.LENGTH_SHORT).show();
                        } else {
                            addUser(username, email, password);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                loggedIn = true;

            } else {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void addUser(String username, String email, String password) {
        User user = new User(username, email, password);
        Map<String, Object> userDetails = user.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/" + username, userDetails);
        mDataRef.updateChildren(childUpdates);

        storeLocalUserData(username, password);

        Toast.makeText(this, "Logged in as " + username + ".", Toast.LENGTH_SHORT).show();

        Intent i = new Intent(this, MapsActivityCurrentPlace.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void storeLocalUserData(String username, String password) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("Logged in", true);
        editor.putString("Username", username);
        editor.putString("Password", password);

        editor.apply();
    }
}
