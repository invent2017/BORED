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

        refreshDatabase();

        checkUpdates();
    }

    private void refreshDatabase() {

        mDataRef.child("stories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    final String user = ds.getKey();

                    for(DataSnapshot dataSnapshot1 : ds.child("stories").getChildren()) {
                        final String storyKey = dataSnapshot1.getKey();

                        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                int views = dataSnapshot.child("users").child(user).child("Viewed").getValue(Integer.class);
                                if(dataSnapshot.child("stories").child(storyKey).child("Views").getValue() != null) {
                                    views += dataSnapshot.child("stories").child(storyKey).child("Views").getValue(Integer.class);
                                }

                                mDataRef.child("users").child(user).child("Viewed").setValue(views);

                                int upvotes = 0;
                                if(dataSnapshot.child("users").child(user).child("Upvoted").getValue(Integer.class) != null) {
                                    upvotes = dataSnapshot.child("users").child(user).child("Upvoted").getValue(Integer.class);
                                }
                                if(dataSnapshot.child("stories").child(storyKey).child("Votes").getValue() != null) {
                                    upvotes += dataSnapshot.child("stories").child(storyKey).child("Votes").getValue(Integer.class);
                                }

                                mDataRef.child("users").child(user).child("Upvoted").setValue(upvotes);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkUpdates() {
        final String versionName = BuildConfig.VERSION_NAME;
        mDataRef.child("version").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!(dataSnapshot.getValue(String.class).equals(versionName))) {
                    AlertDialog.Builder updatePrompt = new AlertDialog.Builder(Startup.this);
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
                                    getUserData();
                                }
                            });
                    updatePrompt.create().show();
                } else {
                    getUserData();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
