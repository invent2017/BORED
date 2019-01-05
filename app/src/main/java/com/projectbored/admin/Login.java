package com.projectbored.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Login extends AppCompatActivity {
    public static final String PREFS_NAME = "UserDetails";

    private EditText emailField, passwordField;
    private Button signInButton;

    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.signInEmail);
        passwordField = findViewById(R.id.signInPassword);

        signInButton = findViewById(R.id.signin_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent returnToMap = new Intent(this, MapsActivityCurrentPlace.class);
        returnToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToMap);
    }

    // checks that you key in a valid email
    private boolean validateEmail(String email) {
        boolean valid = false;

        String emailPattern = ("\\S+@\\w+\\.\\w+");
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        if(matcher.matches()) {
            valid = true;

        }

        return valid;
    }

    private void signIn() {
        if(emailField.getText().toString().trim().isEmpty() || passwordField.getText().toString().trim().isEmpty())
        {
            Toast.makeText(this, R.string.error_field_required, Toast.LENGTH_SHORT).show();
        } else {
            final String email = emailField.getText().toString();
            final String password = passwordField.getText().toString();

            if(validateEmail(email)) {

                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getUid();
                            mDataRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.child("Admin").getValue(boolean.class)) {
                                        storeLocalUserData(email, password);

                                        Toast.makeText(Login.this, "Logged in as " + email + ".", Toast.LENGTH_SHORT).show();
                                        Intent i = new Intent(Login.this, MapsActivityCurrentPlace.class);
                                        startActivity(i);
                                        finish();
                                    } else {
                                        Toast.makeText(Login.this, "This user is not an admin.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(Login.this, R.string.error_incorrect_password, Toast.LENGTH_SHORT).show();
                        } else if(e instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(Login.this, R.string.error_incorrect_email, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void storeLocalUserData(String email, String password) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("Logged in", true);
        editor.putString("Email", email);
        editor.putString("Password", password);

        editor.apply();
    }
}
