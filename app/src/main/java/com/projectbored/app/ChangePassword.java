package com.projectbored.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChangePassword extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    private DatabaseReference mDataRef;
    private EditText oldPasswordText, newPasswordText, confirmNewPasswordText;
    private Button changePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        setTitle("Change Password");

        mDataRef = FirebaseDatabase.getInstance().getReference();

        oldPasswordText = (EditText)findViewById(R.id.old_password);
        newPasswordText = (EditText)findViewById(R.id.new_password);
        confirmNewPasswordText = (EditText)findViewById(R.id.confirm_new_password);

        changePasswordButton = (Button)findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePassword();
            }
        });
    }

    private void changePassword() {
        final String username = getSharedPreferences(PREFS_NAME, 0).getString("username", "");
        final String oldPassword = oldPasswordText.getText().toString();
        final String newPassword1 = newPasswordText.getText().toString();
        final String newPassword2 = confirmNewPasswordText.getText().toString();

        if(oldPassword.equals("") || newPassword1.equals("") || newPassword2.equals("")) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();

        } else if(newPassword1.equals(newPassword2)) {
            mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild(username)) {
                        String existingPassword = dataSnapshot.child(username).child("Password").getValue(String.class);
                        if(oldPassword.equals(existingPassword)) {
                            dataSnapshot.child(username).child("Password").getRef().setValue(newPassword1);
                            Toast.makeText(ChangePassword.this, "Your password was successfully changed.",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ChangePassword.this, "Incorrect password.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Intent loginAgain = new Intent(ChangePassword.this, Login.class);
                        loginAgain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        Toast.makeText(ChangePassword.this, "An error occurred. Please log in again.", Toast.LENGTH_SHORT).show();
                        startActivity(loginAgain);

                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Toast.makeText(this, "Passwords entered do not match.", Toast.LENGTH_SHORT).show();
        }
    }
}
