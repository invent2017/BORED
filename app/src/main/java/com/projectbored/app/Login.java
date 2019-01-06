package com.projectbored.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Login extends AppCompatActivity {
    public static final String PREFS_NAME = "UserDetails";

    private EditText emailField;
    private EditText passwordField;
    private TextView errorMessage;
    private Button signInButton;
    private Button promptSignUpButton;

    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("Login");

        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.signInEmail);
        passwordField = findViewById(R.id.signInPassword);
        errorMessage = findViewById(R.id.login_error_message);

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

    // checks that you key in a valid email
    private boolean validateEmail(String email) {
        boolean valid = false;

        String emailPattern = ("\\S+@\\w+\\.\\w+");
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        if(matcher.matches()) {
            valid = true;

        } else {
            errorMessage.setText(R.string.error_invalid_email);
        }

        return valid;
    }

    // Sign in process: Email, Password
    private void signIn() {
        if(emailField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            errorMessage.setText(R.string.error_field_required);
        } else {
            final String email = emailField.getText().toString();
            final String password = passwordField.getText().toString();

            if(validateEmail(email)) {

                mAuth.signInWithEmailAndPassword(email,password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            storeLocalUserData(email, password);

                            Toast.makeText(Login.this, "Logged in as " + email + ".", Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(Login.this, MapsActivityCurrentPlace.class);
                            startActivity(i);
                            finish();
                        }
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage.setText(R.string.error_incorrect_password);
                        } else if(e instanceof FirebaseAuthInvalidUserException) {
                            errorMessage.setText(R.string.error_incorrect_email);
                        }
                    }
                });
            }

        }
    }

    /*  Old code
    /
    private void signIn() {
        if(emailField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            Toast.makeText(this, R.string.error_field_required, Toast.LENGTH_SHORT).show();
        } else {
            final String username = emailField.getText().toString();
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
    */

    // Local database things so you remain signed in unless you sign out
    private void storeLocalUserData(String email, String password) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("Logged in", true);
        editor.putString("Email", email);
        editor.putString("Password", password);

        editor.apply();
    }
}
