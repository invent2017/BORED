package com.projectbored.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChangePassword extends AppCompatActivity {
    private static final String PREFS_NAME = "UserDetails";

    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;
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
        mAuth = FirebaseAuth.getInstance();

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
        String email = getSharedPreferences(PREFS_NAME, 0).getString("Email", "");
        final String username = mAuth.getUid();
        final String oldPassword = oldPasswordText.getText().toString();
        final String newPassword1 = newPasswordText.getText().toString();
        final String newPassword2 = confirmNewPasswordText.getText().toString();

        if(oldPassword.equals("") || newPassword1.equals("") || newPassword2.equals("")) {
            SingleToast.show(this, "Please fill in all fields.", Toast.LENGTH_SHORT);

        } else if(newPassword1.equals(newPassword2)) {
            if(email.equals("")) {
                Intent logout = new Intent(this, Logout.class);
                Toast.makeText(this, "An error occurred. Please sign in again.", Toast.LENGTH_SHORT).show();
                startActivity(logout);
                finish();
            } else {
                mAuth.signInWithEmailAndPassword(email, oldPassword).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        mAuth.getCurrentUser().updatePassword(newPassword1);
                        SingleToast.show(ChangePassword.this, "Your password was successfully changed.",
                                Toast.LENGTH_SHORT);


                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("Password", newPassword1);

                        editor.apply();

                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        SingleToast.show(ChangePassword.this, "Incorrect password.", Toast.LENGTH_SHORT);
                    }
                });

                finish();
            }
        } else {
            SingleToast.show(this, "Passwords entered do not match.", Toast.LENGTH_SHORT);
        }
    }


}
