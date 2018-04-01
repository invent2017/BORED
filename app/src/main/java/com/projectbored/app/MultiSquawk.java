package com.projectbored.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class MultiSquawk extends AppCompatActivity {

    //private static final String TAG = ShowStory.class.getSimpleName();  //for debugging purposes
    private static final String PREFS_NAME = "UserDetails";

    EditText caption;
    EditText hashtagBox;

    private StorageReference mStorageRef;
    private DatabaseReference mDataRef;

    String mCurrentPhotoPath;
    String storyKey;

    Bundle storySettings;

    static final int GALLERY_REQUEST = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multisquawk);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setTitle(R.string.add_story);

        storySettings = getIntent().getExtras();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDataRef = FirebaseDatabase.getInstance().getReference();

        caption = findViewById(R.id.story_caption);
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
            //nothing: changeImage();
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
        //nothing
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
                            Intent backToMap = new Intent(MultiSquawk.this, MapsActivityCurrentPlace.class);
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
        if(requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
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
                                Intent i = new Intent(MultiSquawk.this, MapsActivityCurrentPlace.class);
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
                            Intent i = new Intent(MultiSquawk.this, MapsActivityCurrentPlace.class);
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
            //TODO: add location info things from exif (can ref https://developer.android.com/reference/android/media/ExifInterface.html)
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
                        childUpdates.put("/locations/" + locationKey + "/" + storyKey, 0);

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

                        Toast.makeText(MultiSquawk.this, "Story added!", Toast.LENGTH_SHORT).show();

                        finish();
                    } else {
                        Toast.makeText(MultiSquawk.this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                    builder.setMessage("Upload failed. Please try again later.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(MultiSquawk.this, MapsActivityCurrentPlace.class);
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


    public class geoDegree {
        private boolean valid = false;
        Float Latitude, Longitude;
        geoDegree(ExifInterface exif){
            String attrLATITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String attrLATITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String attrLONGITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String attrLONGITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            if((attrLATITUDE !=null)
                    && (attrLATITUDE_REF !=null)
                    && (attrLONGITUDE != null)
                    && (attrLONGITUDE_REF !=null))
            {
                valid = true;

                if(attrLATITUDE_REF.equals("N")){
                    Latitude = convertToDegree(attrLATITUDE);
                }
                else{
                    Latitude = 0 - convertToDegree(attrLATITUDE);
                }

                if(attrLONGITUDE_REF.equals("E")){
                    Longitude = convertToDegree(attrLONGITUDE);
                }
                else{
                    Longitude = 0 - convertToDegree(attrLONGITUDE);
                }

            }
        };

        final public String gpsString = String.valueOf(Latitude) + ", " + String.valueOf(Longitude);

        private Float convertToDegree(String stringDMS){
            Float result = null;
            String[] DMS = stringDMS.split(",", 3);

            String[] stringD = DMS[0].split("/", 2);
            Double D0 = new Double(stringD[0]);
            Double D1 = new Double(stringD[1]);
            Double FloatD = D0/D1;

            String[] stringM = DMS[1].split("/", 2);
            Double M0 = new Double(stringM[0]);
            Double M1 = new Double(stringM[1]);
            Double FloatM = M0/M1;

            String[] stringS = DMS[2].split("/", 2);
            Double S0 = new Double(stringS[0]);
            Double S1 = new Double(stringS[1]);
            Double FloatS = S0/S1;

            result = new Float(FloatD + (FloatM/60) + (FloatS/3600));

            return result;

        };

        public int getLatitudeE6(){
            return (int)(Latitude*1000000);
        }

        public int getLongitudeE6(){
            return (int)(Longitude*1000000);
        }

    }



}
