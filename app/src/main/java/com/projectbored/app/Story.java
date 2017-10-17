package com.projectbored.app;

import android.location.Location;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class Story {
    public Uri uri;
    public Location location;
    public String caption;
    public Date dateTime;

    public Story() {

    }

    public Story (Uri u, Location myLocation, String snippet, Date dateTime) {
        uri = u;
        location = myLocation;
        caption = new String(snippet);
        this.dateTime = dateTime;

    }

    public void setUri(Uri u) {
        uri = u;
    }

    public void setLocation(Location myLocation) {
        location = myLocation;
    }

    public void setCaption(String storyCaption) {
        caption = new String(storyCaption);
    }

    public void setDateTime(Date time) {
        dateTime = time;
    }

    public Uri getUri(){
        return uri;
    }

    public Location getLocation() {
        return location;
    }

    public String getCaption() {
        return caption;
    }

    public Date getDateTime() {
        return dateTime;
    }


}
