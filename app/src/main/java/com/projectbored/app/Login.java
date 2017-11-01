package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

    private TextView emptyFieldText;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        emptyFieldText = (TextView)findViewById(R.id.signInEmptyFieldAlert);

        usernameField = (EditText)findViewById(R.id.signInUsername);
        passwordField = (EditText)findViewById(R.id.signInPassword);

        signInButton = (Button)findViewById(R.id.signin_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        promptSignUpButton = (Button)findViewById(R.id.signup_prompt_button);
        promptSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUp = new Intent(Login.this, CreateAccount.class);
                startActivity(signUp);
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
            emptyFieldText.setText(R.string.error_field_required);
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
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

    private void storeLocalUserData(String username, String password) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("Logged in", true);
        editor.putString("Username", username);
        editor.putString("Password", password);

        editor.apply();
    }
}
