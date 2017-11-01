package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class Startup extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    private boolean loggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        getUserData();
    }

    private void getUserData() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        loggedIn = settings.getBoolean("Logged in", false);
        if(!loggedIn){
            promptLogIn().create().show();
        } else {
            String username = settings.getString("Username", "");
            Toast.makeText(this, "Logged in as " + username + "." , Toast.LENGTH_LONG).show();
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
