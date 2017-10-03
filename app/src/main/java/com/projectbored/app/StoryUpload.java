package com.projectbored.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class StoryUpload extends AppCompatActivity {

    EditText caption;
    Button uploadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.projectbored.app.R.layout.activity_story_upload);

        caption = (EditText)findViewById(R.id.story_caption);
        uploadButton = (Button)findViewById(R.id.uploadstory);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateMap();
            }
        });

        dispatchTakePictureIntent();
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ((ImageView) findViewById(com.projectbored.app.R.id.story_image)).setImageBitmap(imageBitmap);
        }
    }

    //Quite sure the problem is with the following

    private void updateMap () {
        String snippet = caption.getText().toString();
        MapsActivityCurrentPlace m = new MapsActivityCurrentPlace();
        m.addMarker(snippet);
        Intent i = new Intent(this, MapsActivityCurrentPlace.class);
        startActivity(i);
    }
}
