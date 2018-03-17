package com.projectbored.app;

import android.content.Context;
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

    public static class SingleToast {

        private static Toast mToast;

        public static void show(Context context, String text, int duration) {
            if (mToast != null) mToast.cancel();
            mToast = Toast.makeText(context, text, duration);
            mToast.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        setTitle("Change Password");

        mDataRef = FirebaseDatabase.getInstance().getReference();

        oldPasswordText = findViewById(R.id.old_password);
        newPasswordText = findViewById(R.id.new_password);
        confirmNewPasswordText = findViewById(R.id.confirm_new_password);

        changePasswordButton = findViewById(R.id.change_password_button);
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
            SingleToast.show(this, "Please fill in all fields.", Toast.LENGTH_SHORT);

        } else if(newPassword1.equals(newPassword2)) {
            mDataRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild(username)) {
                        String existingPassword = dataSnapshot.child(username).child("Password").getValue(String.class);
                        if(oldPassword.equals(existingPassword)) {
                            dataSnapshot.child(username).child("Password").getRef().setValue(newPassword1);
                            SingleToast.show(ChangePassword.this, "Your password was successfully changed.",
                                    Toast.LENGTH_SHORT);
                            finish();
                        } else {
                            SingleToast.show(ChangePassword.this, "Incorrect password.", Toast.LENGTH_SHORT);
                        }

                    } else {
                        Intent loginAgain = new Intent(ChangePassword.this, Login.class);
                        loginAgain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        SingleToast.show(ChangePassword.this, "An error occurred. Please log in again.", Toast.LENGTH_SHORT);
                        startActivity(loginAgain);

                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            SingleToast.show(this, "Passwords entered do not match.", Toast.LENGTH_SHORT);
        }
    }


}
