package com.projectbored.app;

import android.location.Location;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Story {
    public Uri uri;
    public Location location;
    public String caption;
    public Date dateTime;
    public int votes;

    public Story() {

    }

    public Story (Uri u, Location myLocation, String snippet, Date dateTime) {
        uri = u;
        location = myLocation;
        caption = new String(snippet);
        this.dateTime = dateTime;
        votes = 0;

    }

    @Exclude
    public void setUri(Uri u) {
        uri = u;
    }

    @Exclude
    public void setLocation(Location myLocation) {
        location = myLocation;
    }

    @Exclude
    public void setCaption(String storyCaption) {
        caption = new String(storyCaption);
    }

    @Exclude
    public void setDateTime(Date time) {
        dateTime = time;
    }

    @Exclude
    public void upVote() {
        votes++;
    }

    @Exclude
    public void downVote() {
        votes--;
    }

    @Exclude
    public Uri getUri(){
        return uri;
    }

    @Exclude
    public Location getLocation() {
        return location;
    }

    @Exclude
    public String getCaption() {
        return caption;
    }

    @Exclude
    public Date getDateTime() {
        return dateTime;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("URI", uri);
        result.put("Location", location);
        result.put("Caption", caption);
        result.put("Date/Time", dateTime);

        return result;
    }


}
