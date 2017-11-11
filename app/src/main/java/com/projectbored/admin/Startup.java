package com.projectbored.admin;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Startup extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    private boolean loggedIn;

    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        mDataRef = FirebaseDatabase.getInstance().getReference();

        getUserData();
    }

    private void getUserData() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        loggedIn = settings.getBoolean("Logged in", false);
        if(!loggedIn){
            promptLogIn().create().show();
        } else {
            String username = settings.getString("Username", "");
            String password = settings.getString("Password", "");
            //Toast.makeText(Startup.this, "Logged in as " + username + "." , Toast.LENGTH_LONG).show();
            verifyAccount(settings, username, password);

            /*Intent start = new Intent(Startup.this, MapsActivityCurrentPlace.class);
            start.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(start);*/
        }
    }

    private void verifyAccount(final SharedPreferences settings, final String username, final String password){
        mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(username).exists()){
                    if(dataSnapshot.child(username).child("Admin").getValue(boolean.class)) {
                        if (dataSnapshot.child(username).child("Password").getValue(String.class).equals(password)) {
                            Toast.makeText(Startup.this, "Logged in as " + username + ".", Toast.LENGTH_SHORT).show();

                            Intent start = new Intent(Startup.this, MapsActivityCurrentPlace.class);
                            start.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(start);
                        } else {
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean("Logged in", false);
                            editor.remove("Username");
                            editor.remove("Password");
                            editor.apply();

                            promptLogIn().create().show();
                        }
                    } else {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("Logged in", false);
                        editor.remove("Username");
                        editor.remove("Password");
                        editor.apply();

                        Toast.makeText(Startup.this, "You need to be an admin to use the admin version of the app.", Toast.LENGTH_SHORT).show();

                        promptLogIn().create().show();
                    }
                } else {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("Logged in", false);
                    editor.remove("Username");
                    editor.remove("Password");
                    editor.apply();

                    Toast.makeText(Startup.this, "Account settings have changed. Please log in again.", Toast.LENGTH_SHORT).show();

                    Intent returnToMap = new Intent(Startup.this, MapsActivityCurrentPlace.class);
                    returnToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(returnToMap);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private AlertDialog.Builder promptLogIn() {
        AlertDialog.Builder logInPrompt = new AlertDialog.Builder(this);
        logInPrompt.setMessage("Please log in to continue.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent logInIntent = new Intent(Startup.this, Login.class);
                        startActivity(logInIntent);
                    }
                });

        return logInPrompt;
    }
}
