package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

        checkAppStatus();
    }

    private void checkAppStatus() {
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean underMaintenance = false;
                if(dataSnapshot.child("maintenance").exists()) {
                    underMaintenance = dataSnapshot.child("maintenance").getValue(boolean.class);
                }
                if(underMaintenance) {
                    Toast.makeText(Startup.this, "BORED is currently under maintenance.", Toast.LENGTH_SHORT).show();
                    Startup.this.finish();
                } else {
                    checkUpdates(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkUpdates(final DataSnapshot dataSnapshot) {
        final String versionName = BuildConfig.VERSION_NAME;
        String[] versionDetailsArray = dataSnapshot.child("version").getValue(String.class).split(" ");
        if(!(versionDetailsArray[0].equals(versionName))) {
            AlertDialog.Builder updatePrompt = new AlertDialog.Builder(Startup.this);
            boolean critical = false;
            if(versionDetailsArray[1].equals("1")) {
                critical = true;
            }
            if(critical) {
                updatePrompt.setMessage("A new version of BORED! is available. Update?")
                        .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://projectboredinc.wordpress.com/download/"));
                                startActivity(browserIntent);
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                Startup.this.finish();
                            }
                        });
            } else {
                updatePrompt.setMessage("A new version of BORED! is available. Update?")
                        .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://projectboredinc.wordpress.com/download/"));
                                startActivity(browserIntent);
                            }
                        })
                        .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getUserData(dataSnapshot);
                            }
                        });
            }
            updatePrompt.create().show();
        } else {
            getUserData(dataSnapshot);
        }
    }

    private void getUserData(DataSnapshot dataSnapshot) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        loggedIn = settings.getBoolean("Logged in", false);
        if(!loggedIn){
            promptLogIn().create().show();
        } else {
            String username = settings.getString("Username", "");
            String password = settings.getString("Password", "");
            //Toast.makeText(Startup.this, "Logged in as " + username + "." , Toast.LENGTH_LONG).show();
            verifyAccount(settings, username, password, dataSnapshot.child("users"));

            /*Intent start = new Intent(Startup.this, MapsActivityCurrentPlace.class);
            start.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(start);*/
        }
    }

    private void verifyAccount(SharedPreferences settings, String username, String password, DataSnapshot dataSnapshot){
        if(dataSnapshot.child(username).exists()){
            if(dataSnapshot.child(username).child("Password").getValue(String.class).equals(password)) {
                Toast.makeText(Startup.this, "Logged in as " + username + "." , Toast.LENGTH_SHORT).show();

                Intent start = new Intent(Startup.this, MapsActivityCurrentPlace.class);
                start.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(start);
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

    private AlertDialog.Builder promptLogIn() {
        AlertDialog.Builder logInPrompt = new AlertDialog.Builder(this);
        logInPrompt.setMessage("Want to access bonus features? :)")
                .setPositiveButton("Log In", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent logInIntent = new Intent(Startup.this, Login.class);
                        startActivity(logInIntent);
                    }
                })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent start = new Intent(Startup.this, MapsActivityCurrentPlace.class);
                        start.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(start);
                    }
                });

        return logInPrompt;
    }
}
