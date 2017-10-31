package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {
    public static final String PREFS_NAME = "UserDetails";

    private EditText usernameField;
    private EditText passwordField;
    private Button signInButton;
    private Button signUpButton;

    private TextView emptyFieldText;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        emptyFieldText = (TextView)findViewById(R.id.emptyFieldAlert);

        usernameField = (EditText)findViewById(R.id.username);
        passwordField = (EditText)findViewById(R.id.password);

        signInButton = (Button)findViewById(R.id.signin_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        signUpButton = (Button)findViewById(R.id.signup_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
    }

    private void signIn() {
        if(usernameField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            emptyFieldText.setText(R.string.error_field_required);
        } else {
            final String username = usernameField.getText().toString();
            final String password = passwordField.getText().toString();

            mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(username).exists()){
                        if(dataSnapshot.child(username).child("Password").getValue(String.class).equals(password)) {
                            storeUserData(username, password);

                            Intent i = new Intent(Login.this, MapsActivityCurrentPlace.class);
                            startActivity(i);
                        } else {
                            emptyFieldText.setText(R.string.error_incorrect_password);
                        }
                    } else {
                        emptyFieldText.setText(R.string.error_incorrect_username);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void signUp() {
        if(usernameField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            emptyFieldText.setText(R.string.error_field_required);
        } else {
            final String username = usernameField.getText().toString();
            final String password = passwordField.getText().toString();

            mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child(username).exists()) {
                        emptyFieldText.setText(R.string.error_existing_username);
                    } else {
                        addUser(username, password);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void addUser(String username, String password) {
        User user = new User(username, password);
        Map<String, Object> userDetails = user.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/" + username, userDetails);
        mDataRef.updateChildren(childUpdates);

        storeUserData(username, password);

        Intent i = new Intent(this, MapsActivityCurrentPlace.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void storeUserData(String username, String password) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("Logged in", true);
        editor.putString("Username", username);
        editor.putString("Password", password);

        editor.commit();
    }
}
