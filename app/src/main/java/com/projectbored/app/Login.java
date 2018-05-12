package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class Login extends AppCompatActivity {
    public static final String PREFS_NAME = "UserDetails";

    private EditText usernameField;
    private EditText passwordField;
    private Button signInButton;
    private Button promptSignUpButton;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("Login");

        mDataRef = FirebaseDatabase.getInstance().getReference();

        usernameField = findViewById(R.id.signInUsername);
        passwordField = findViewById(R.id.signInPassword);

        signInButton = findViewById(R.id.signin_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        promptSignUpButton = findViewById(R.id.signup_prompt_button);
        promptSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUp = new Intent(Login.this, CreateAccount.class);
                startActivity(signUp);
            }
        });
    }

    // If you press back the app closes
    @Override
    public void onBackPressed() {
        Intent closeApp = new Intent(Intent.ACTION_MAIN);
        closeApp.addCategory(Intent.CATEGORY_HOME);
        startActivity(closeApp);
    }

    // Sign in process: Username, Password
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
                        if(dataSnapshot.child(username).child("Password").getValue(String.class).equals(password)) {
                            storeLocalUserData(username, password);

                            Toast.makeText(Login.this, "Logged in as " + username + ".", Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(Login.this, MapsActivityCurrentPlace.class);
                            startActivity(i);

                            finish();
                        } else {
                            Toast.makeText(Login.this, R.string.error_incorrect_password, Toast.LENGTH_SHORT).show();
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

    // Local database things so you remain signed in unless you sign out
    private void storeLocalUserData(String username, String password) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("Logged in", true);
        editor.putString("Username", username);
        editor.putString("Password", password);

        editor.apply();
    }
}
