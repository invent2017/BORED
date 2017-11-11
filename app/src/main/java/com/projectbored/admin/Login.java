package com.projectbored.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {
    public static final String PREFS_NAME = "UserDetails";

    private EditText usernameField;
    private EditText passwordField;
    private Button signInButton;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        usernameField = (EditText)findViewById(R.id.signInUsername);
        passwordField = (EditText)findViewById(R.id.signInPassword);

        signInButton = (Button)findViewById(R.id.signin_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent returnToMap = new Intent(this, MapsActivityCurrentPlace.class);
        returnToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToMap);
    }

    private void signIn() {
        if(usernameField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            Toast.makeText(this, R.string.error_field_required, Toast.LENGTH_SHORT).show();
        } else {
            final String username = usernameField.getText().toString();
            final String password = passwordField.getText().toString();

            mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(username).exists()){
                        if(dataSnapshot.child(username).child("Admin").getValue(boolean.class)) {
                            if (dataSnapshot.child(username).child("Password").getValue(String.class).equals(password)) {
                                storeLocalUserData(username, password);

                                Toast.makeText(Login.this, "Logged in as " + username + ".", Toast.LENGTH_SHORT).show();

                                Intent i = new Intent(Login.this, MapsActivityCurrentPlace.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            } else {
                                Toast.makeText(Login.this, R.string.error_incorrect_password, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(Login.this, "This user is not an admin.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Login.this, R.string.error_incorrect_username, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
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
