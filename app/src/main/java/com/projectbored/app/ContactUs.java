package com.projectbored.app;

import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ContactUs extends AppCompatActivity {

    EditText textSubject;
    EditText textMessage;
    Button surveyButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.whitebored);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        textSubject = findViewById(R.id.subject_input);
        textMessage = findViewById(R.id.message_input);

        surveyButton = findViewById(R.id.button2);
        surveyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://goo.gl/forms/S9FgEdln0jWBfqCr2"));
                    startActivity(browserIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_us_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_send_message) {

            String to = "projectbored.inc@gmail.com";
            String subject = textSubject.getText().toString();
            String message = textMessage.getText().toString();

            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{ to});
            //email.putExtra(Intent.EXTRA_CC, new String[]{ to});
            //email.putExtra(Intent.EXTRA_BCC, new String[]{to});
            email.putExtra(Intent.EXTRA_SUBJECT, subject);
            email.putExtra(Intent.EXTRA_TEXT, message);

            //need this to prompts email client only
            email.setType("message/rfc822");

            startActivity(Intent.createChooser(email, "Send Email with..."));

        }
        return true;
    }
}
