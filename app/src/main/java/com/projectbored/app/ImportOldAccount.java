package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ImportOldAccount extends AppCompatActivity {
    private EditText oldUsernameField, oldPasswordField;
    private TextView errorMessageText,transferringDataText;
    private Button signInButton;
    private ProgressBar transferringDataProgress;

    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;

    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_old_account);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        mDataRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        uid = mAuth.getUid();

        oldUsernameField = findViewById(R.id.old_account_username);
        oldPasswordField = findViewById(R.id.old_account_password);
        errorMessageText = findViewById(R.id.login_error_message);
        transferringDataText = findViewById(R.id.transferring_text);
        transferringDataProgress = findViewById(R.id.transferring_progress);
        signInButton = findViewById(R.id.old_account_sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOldAccount();
            }
        });

    }

    private void getOldAccount() {
        if(oldUsernameField.getText().toString().trim().isEmpty() || oldPasswordField.getText().toString().trim().isEmpty())
        {
            errorMessageText.setText(R.string.error_field_required);
        } else {
            final String username = oldUsernameField.getText().toString();
            final String password = oldPasswordField.getText().toString();

            if(username.contains(".") || username.contains("#") || username.contains("$") || username.contains("[")
                    || username.contains("]")) {
                errorMessageText.setText(R.string.error_invalid_username);
            } else {

                mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                        if (dataSnapshot.child("users").hasChild(username)) {
                            if (password.equals(dataSnapshot.child("users").child(username)
                                    .child("Password").getValue(String.class))) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ImportOldAccount.this);
                                builder.setTitle("Import data from " + username);
                                builder.setMessage("Would you like to transfer data from " + username + " to this account?");
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        importData(dataSnapshot, username);
                                        dialogInterface.dismiss();

                                        finishActivity();
                                    }
                                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });

                                builder.show();


                            } else {
                                errorMessageText.setText(R.string.error_incorrect_password);
                            }

                        } else {
                            errorMessageText.setText(R.string.account_not_found);
                            mDataRef.child("users").removeEventListener(this);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }


        }
    }

    private void importData(final DataSnapshot dataSnapshot, final String oldUsername) {
        transferringDataText.setVisibility(View.VISIBLE);
        transferringDataProgress.setVisibility(View.VISIBLE);

        Map<String, Object> oldAccountMap = (Map<String, Object>)dataSnapshot.child("users").child(oldUsername).getValue();
        oldAccountMap.remove("Username");
        oldAccountMap.remove("Email");
        oldAccountMap.remove("Password");

        dataSnapshot.child("users").child(uid).getRef().updateChildren(oldAccountMap,
                new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                transferringDataProgress.incrementProgressBy(20);
                updateStoryData(oldUsername);
            }
        });

    }

    private void updateStoryData(final String oldUsername) {
        mDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot story : dataSnapshot.child("users").child(oldUsername).child("stories").getChildren()) {
                    String storyKey = story.getKey();
                    dataSnapshot.child("stories").child(storyKey).child("User").getRef().setValue(uid);
                }
                transferringDataProgress.incrementProgressBy(20);

                for(DataSnapshot story : dataSnapshot.child("users").child(oldUsername).child("ReadStories").getChildren()) {
                    String storyKey = story.getKey();
                    dataSnapshot.child("stories").child(storyKey).child("Viewers").child(uid).getRef().setValue(uid);
                    dataSnapshot.child("stories").child(storyKey).child("Viewers").child(oldUsername).getRef().removeValue();
                }
                transferringDataProgress.incrementProgressBy(20);

                for(DataSnapshot story : dataSnapshot.child("users").child(oldUsername).child("UpvotedStories").getChildren()) {
                    String storyKey = story.getKey();
                    dataSnapshot.child("stories").child(storyKey).child("Upvoters").child(uid).getRef().setValue(uid);
                    dataSnapshot.child("stories").child(storyKey).child("Upvoters").child(oldUsername).getRef().removeValue();
                }
                transferringDataProgress.incrementProgressBy(20);

                mDataRef.child("users").child(oldUsername).removeValue();

                recountStats(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void recountStats(DataSnapshot dataSnapshot) {
        int upvoted = 0;
        int upvotes = 0;
        int viewed = 0;
        int views = 0;

        for(DataSnapshot story : dataSnapshot.child("users").child(uid).child("UpvotedStories").getChildren()) {
            upvotes++;
        }

        for(DataSnapshot story : dataSnapshot.child("users").child(uid).child("ReadStories").getChildren()) {
            views++;
        }

        for(DataSnapshot story : dataSnapshot.child("users").child(uid).child("stories").getChildren()){

            String storyKey = story.getKey();

            if(dataSnapshot.child("stories").child(storyKey).exists()) {
                viewed += dataSnapshot.child("stories").child(storyKey).child("Views").getValue(Integer.class);
                upvoted += dataSnapshot.child("stories").child(storyKey).child("Votes").getValue(Integer.class);
            }
        }

        mDataRef.child("users").child(uid).child("Upvoted").setValue(upvoted);
        mDataRef.child("users").child(uid).child("Upvotes").setValue(upvotes);
        mDataRef.child("users").child(uid).child("Viewed").setValue(viewed);
        mDataRef.child("users").child(uid).child("Views").setValue(views);

        transferringDataProgress.incrementProgressBy(20);


    }

    private void finishActivity() {
        Intent backToMap = new Intent(ImportOldAccount.this, MapsActivityCurrentPlace.class);
        backToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Toast.makeText(ImportOldAccount.this, "Old account data was successfully transferred.",
                Toast.LENGTH_SHORT).show();
        startActivity(backToMap);
        finish();
    }
}
