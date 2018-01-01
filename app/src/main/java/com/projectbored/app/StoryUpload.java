package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoryUpload extends AppCompatActivity {
    //private static final String TAG = ShowStory.class.getSimpleName();  //for debugging purposes
    private static final String PREFS_NAME = "UserDetails";

    EditText caption;
    EditText hashtagBox;

    private StorageReference mStorageRef;
    private DatabaseReference mDataRef;

    String mCurrentPhotoPath;
    String storyKey;

    Bundle storySettings;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int GALLERY_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.projectbored.app.R.layout.activity_story_upload);

        setTitle(R.string.add_story);

        storySettings = getIntent().getExtras();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDataRef = FirebaseDatabase.getInstance().getReference();

        caption = (EditText)findViewById(R.id.story_caption);
        if(storySettings.getString("Caption") != null) {
            caption.setText(storySettings.getString("Caption"));
        }

        hashtagBox = findViewById(R.id.hashtags);

        storyKey = mDataRef.child("stories").push().getKey();

        if(storySettings.getBoolean("FromCamera")){
            dispatchTakePictureIntent();
        } else {
            galleryPickerIntent();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_story_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_upload_story) {
            uploadStoryData();
        } else if(item.getItemId() == R.id.option_change_image) {
            changeImage();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(storyKey != null) {
            deleteImage();
        }

        finish();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try{
                photoFile = createImageFile();
            }catch (IOException ex) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Camera failed. Please try again later.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent backToMap = new Intent(StoryUpload.this, MapsActivityCurrentPlace.class);
                        backToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(backToMap);
                    }
                });
                builder.create().show();
            }
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this, "com.projectbored.app.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void galleryPickerIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);

        File photoFile = null;
        try{
            photoFile = createImageFile();
        } catch (IOException ex) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Couldn't load image. Please try again later.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent backToMap = new Intent(StoryUpload.this, MapsActivityCurrentPlace.class);
                            backToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(backToMap);
                        }
                    });
            builder.create().show();
        }
        if(photoFile != null) {
            Uri photoUri = FileProvider.getUriForFile(this, "com.projectbored.app.fileprovider", photoFile);
            photoPickerIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
        //    Bundle extras= data.getExtras();
        //    Bitmap imageBitmap = (Bitmap)extras.get("data");
            ((ImageView) findViewById(com.projectbored.app.R.id.story_image)).setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
            Uri file = Uri.fromFile(new File(mCurrentPhotoPath));
            uploadImage(file);
        } else if(requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                ((ImageView) findViewById(R.id.story_image)).setImageBitmap(selectedImage);
                uploadImage(imageUri);

            } catch (FileNotFoundException e) {
                Toast.makeText(this, "Something went wrong. Story not uploaded.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timestamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg", storageDir);

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void uploadImage (Uri file) {

        //String fileUri = file.toString();
        //File imageFile = new File(fileUri);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpg")
                .build();

        String imageFileName = file.getLastPathSegment();

        //TODO: check if image with same name already exists

        UploadTask uploadTask = mStorageRef.child(imageFileName).putFile(file, metadata);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                builder.setMessage("Upload failed. Please try again later.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(StoryUpload.this, MapsActivityCurrentPlace.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });
                builder.create().show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                uploadImageData(taskSnapshot);
            }
        });

    }

    private void uploadImageData(UploadTask.TaskSnapshot taskSnapshot) {
        final Uri PHOTO_URI = taskSnapshot.getMetadata().getDownloadUrl();

        if(PHOTO_URI == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            builder.setMessage("Upload failed. Please try again later.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(StoryUpload.this, MapsActivityCurrentPlace.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                    });
            builder.create().show();
        } else {
            mDataRef.child("uploads").child(storyKey).setValue(PHOTO_URI.toString());
        }
    }

    private void uploadStoryData () {
        final Location storyLocation = new Location(LocationManager.GPS_PROVIDER);
        storyLocation.setLatitude(storySettings.getDouble("Latitude"));
        storyLocation.setLongitude(storySettings.getDouble("Longitude"));

        if (storyLocation != null) {
            final String locationString = Double.toString(storyLocation.getLatitude())
                                    + ","
                                    + Double.toString(storyLocation.getLongitude());

            mDataRef.child("uploads").child(storyKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        String storyURI = dataSnapshot.getValue(String.class);
                        String storyCaption = caption.getText().toString();

                        String hashtagBoxText = hashtagBox.getText().toString();

                        if(!hashtagBoxText.equals("")) {
                            hashtagBoxText = hashtagBoxText.substring(1);
                            String [] hashtags = hashtagBoxText.split("#");

                            StringBuilder captionBuilder = new StringBuilder().append(storyCaption).append(" ");
                            for(int i = 0; i < hashtags.length; i++) {
                                hashtags[i] = hashtags[i].trim();
                                captionBuilder.append("#").append(hashtags[i]);
                            }

                            storyCaption = captionBuilder.toString();
                        }

                        Map<String, Object> childUpdates = new HashMap<>();

                        Story story = new Story(storyURI, storyLocation, storyCaption, new Date());
                        Map<String, Object> storyDetails = story.toMap();

                        childUpdates.put("/stories/" + storyKey, storyDetails);

                        String locationKey = (Double.toString(storyLocation.getLatitude()) + ","
                                + Double.toString(storyLocation.getLongitude())).replace('.', 'd');
                        childUpdates.put("/locations/" + locationKey + "/" + storyKey, false);

                        if (storySettings.getBoolean("Logged in")) {
                            String username = getSharedPreferences(PREFS_NAME, 0).getString("Username", "");
                            childUpdates.put("/users/" + username + "/stories/" + storyKey, locationString);
                        }

                        if (storyCaption.contains("#")) {
                            String hashTagPattern = ("#(\\w+)");

                            Pattern p = Pattern.compile(hashTagPattern);
                            Matcher m = p.matcher(storyCaption);

                            while (m.find()) {
                                String hashtag = m.group(1);
                                childUpdates.put("/hashtags/" + hashtag + "/" + storyKey, locationString);
                            }
                        }



                        mDataRef.updateChildren(childUpdates);

                        mDataRef.child("uploads").child(storyKey).removeValue();

                        Toast.makeText(StoryUpload.this, "Story added!", Toast.LENGTH_SHORT).show();

                        finish();
                    } else {
                        Toast.makeText(StoryUpload.this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                    builder.setMessage("Upload failed. Please try again later.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(StoryUpload.this, MapsActivityCurrentPlace.class);
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                }
                            });
                    builder.create().show();
                }
            });


        } else {
            Toast.makeText(this, "An error occurred.", Toast.LENGTH_SHORT).show();
        }

    }

    private void changeImage() {
        if(storyKey != null){
            deleteImage();
        }

        AlertDialog.Builder changeImagePrompt = new AlertDialog.Builder(this);
        changeImagePrompt.setTitle(R.string.change_image)
                .setItems(R.array.add_story_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0) {
                            Intent newStory = new Intent(StoryUpload.this, StoryUpload.class);
                            storySettings.putBoolean("FromCamera", true);
                            storySettings.putString("Caption", caption.getText().toString());
                            newStory.putExtras(storySettings);
                            newStory.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                            finish();

                            startActivity(newStory);
                        } else if(which == 1) {
                            Intent newStory = new Intent(StoryUpload.this, StoryUpload.class);
                            storySettings.putBoolean("FromCamera", false);
                            storySettings.putString("Caption", caption.getText().toString());
                            newStory.putExtras(storySettings);
                            newStory.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                            finish();

                            startActivity(newStory);
                        }
                    }
                });
        changeImagePrompt.create().show();
    }

    private void deleteImage() {
        mDataRef.child("uploads").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(storyKey)) {
                    String storyUri = dataSnapshot.child(storyKey).getValue(String.class);
                    FirebaseStorage.getInstance().getReferenceFromUrl(storyUri).delete();
                    dataSnapshot.child(storyKey).getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                finish();
            }
        });
    }
}