package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth;

public class LogoutNoToast extends AppCompatActivity {
    final public static String PREFS_NAME = "UserDetails";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);

        mAuth = FirebaseAuth.getInstance();

        logout();
    }

    private void logout() {
        mAuth.signOut();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("Username");
        editor.remove("Email");
        editor.remove("Password");
        editor.putBoolean("Logged in", false);

        editor.apply();

        Intent login = new Intent(this, Login.class);
        startActivity(login);

        finish();
    }
}
