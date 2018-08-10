package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
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

    //public boolean loggedIn;
    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;

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
        actionBar.setTitle("");

        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        usernameField = findViewById(R.id.signUpUsername);
        emailField = findViewById(R.id.signUpEmail);
        passwordField = findViewById(R.id.signUpPassword);

        signUpButton = findViewById(R.id.signup_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
    }

    // If you press back, you go back to Log In
    @Override
    public void onBackPressed() {
        Intent goLogin = new Intent(this, Login.class);
        startActivity(goLogin);
        finish();
    }

    // Code to sign you up: needs email, password
    private void signUp() {
        if(emailField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            Toast.makeText(this, R.string.error_field_required, Toast.LENGTH_SHORT).show();
        } else {
            String username = usernameField.getText().toString();
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();

            if(validateEmail(email)) {
                addUser(username, email, password);
            }
        }
    }

    // checks that you key in a valid email
    private boolean validateEmail(String email) {
        boolean valid = false;

        String emailPattern = ("\\S+@\\w+\\.\\w+");
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        if(matcher.matches()) {
            valid = true;

        } else {
            Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
        }

        return valid;
    }

    // Adds user to Firebase using Authentication
    private void addUser(final String username, final String email, final String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                final String uid = task.getResult().getUser().getUid();
                storeLocalUserData(uid, email, password);

                //Add user details to database
                mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(!(dataSnapshot.child("users").child(username).exists())) {
                            User user = new User(username, email, password);
                            Map<String, Object> userDetails = user.toMap();
                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("/users/" + uid, userDetails);
                            mDataRef.updateChildren(childUpdates);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                Toast.makeText(CreateAccount.this, "Logged in as " + email + ".", Toast.LENGTH_SHORT).show();

                Intent i = new Intent(CreateAccount.this, MapsActivityCurrentPlace.class);
                startActivity(i);
                CreateAccount.this.finish();
            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(e instanceof FirebaseAuthUserCollisionException){
                    Toast.makeText(CreateAccount.this, R.string.error_existing_email, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*  Old code
    /
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
    */

    // Local database things so you remain signed in unless you sign out
    private void storeLocalUserData(String uid, String email, String password) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("Logged in", true);
        editor.putString("Email", email);
        editor.putString("Password", password);

        editor.apply();
    }
}
