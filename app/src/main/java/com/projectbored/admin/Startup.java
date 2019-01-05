package com.projectbored.admin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.fabric.sdk.android.Fabric;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Startup Code (works in following flow):
 * 1. Checks for app maintenance and updates
 * 2. Removes expired events
 * 3. Checks if onboarding has been completed (if false, user --> onboarding --> Login)
 * 4. Checks if logged in
 * 5. Opens MapsActivityCurrentPlace if logged in
 */

public class Startup extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    private boolean loggedIn;

    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_startup);

        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        getUserData();

    }

    // Checks log-in and onboarding using SharedPreferences
    private void getUserData() {
        // If onboarding has been completed, check for logged in
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        loggedIn = settings.getBoolean("Logged in", false);
        if(!loggedIn){
            Intent login = new Intent(this, Login.class);
            startActivity(login);

            finish();

        } else {
            String email = settings.getString("Email", "");
            String password = settings.getString("Password", "");
            //Toast.makeText(Startup.this, "Logged in as " + username + "." , Toast.LENGTH_LONG).show();
            verifyAccount(email, password);
        }
    }

    // verify log-in
    private void verifyAccount(final String email, String password) {

        if (email.equals("") || password.equals("")) {
            Intent logout = new Intent(Startup.this, Logout.class);
            logout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(logout);

            finish();

        } else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String uid = mAuth.getUid();
                                if(dataSnapshot.child("users").child(uid).child("Admin").getValue(Boolean.class)) {
                                    /*int storiesWithoutURI = 0;
                                    ArrayList<String> keyArray = new ArrayList<>();
                                    for(DataSnapshot ds : dataSnapshot.child("stories").getChildren()) {
                                        if(!ds.hasChild("URI")) {
                                            storiesWithoutURI++;
                                            keyArray.add(ds.getKey());
                                        }
                                    }

                                    StringBuilder messageBuilder = new StringBuilder();
                                    messageBuilder.append("Number of stories without URI: ").append(storiesWithoutURI).append("\n").append("\n");
                                    for(String key : keyArray) {
                                        messageBuilder.append(key).append("\n");
                                    }

                                    AlertDialog.Builder builder = new AlertDialog.Builder(Startup.this);
                                    builder.setMessage(messageBuilder.toString());
                                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            startApp();
                                        }
                                    });
                                    builder.show();*/

                                    Toast.makeText(Startup.this, "Logged in as " + email + ".", Toast.LENGTH_SHORT).show();
                                    startApp();
                                    mDataRef.removeEventListener(this);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });


                    }
                }
            }).addOnFailureListener(Startup.this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(Startup.this, "Account settings have changed. Please log in again.", Toast.LENGTH_SHORT).show();

                    Intent logout = new Intent(Startup.this, Logout.class);
                    logout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(logout);

                    finish();

                }
            });


        /* Old code
        /
        if(dataSnapshot.child(username).exists()){
            if(dataSnapshot.child(username).child("Password").getValue(String.class).equals(password)) {
                Toast.makeText(Startup.this, "Logged in as " + username + "." , Toast.LENGTH_SHORT).show();

                Intent start = new Intent(Startup.this, MapsActivityCurrentPlace.class);
                start.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(start);
            } else {
                Toast.makeText(Startup.this, "Account settings have changed. Please log in again.", Toast.LENGTH_SHORT).show();

                Intent logout = new Intent(Startup.this, Logout.class);
                logout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(logout);
            }
        } else {
            Toast.makeText(Startup.this, "Account settings have changed. Please log in again.", Toast.LENGTH_SHORT).show();

            Intent logout = new Intent(Startup.this, Logout.class);
            logout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(logout);

            finish();
        }*/
        }
    }

    private void startApp(){
        Intent i = new Intent(Startup.this, MapsActivityCurrentPlace.class);
        startActivity(i);
        finish();
    }
}
