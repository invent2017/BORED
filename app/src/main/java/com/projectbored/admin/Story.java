package com.projectbored.admin;

import android.location.Location;
import android.net.Uri;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Story {
    private String uri;
    private Location location;
    private String caption;
    private Date dateTime;
    private int votes;
    private int views;

    private boolean featured;
    private boolean flagged;


    public Story() {

    }

    public Story (Uri u, Location myLocation, String snippet, Date dateTime) {
        uri = u.toString();
        location = myLocation;
        caption = snippet;
        this.dateTime = dateTime;
        votes = 0;
        views = 0;
        featured = false;
        flagged = false;
    }

    public Story (String uri, Location location, String caption, Date dateTime) {
        this.uri = uri;
        this.location = location;
        this.caption = caption;
        this.dateTime = dateTime;
        votes = 0;
        views = 0;
        featured = false;
        flagged = false;
    }

    public Story(String u, Location myLocation, String snippet, Date dateTime, int numVotes, int numViews){
        uri = u;
        location = myLocation;
        caption = snippet;
        this.dateTime = dateTime;
        votes = numVotes;
        views = numViews;
        featured = false;
        flagged = false;
    }

    public void setUri(Uri u) {
        uri = u.toString();
    }

    public void setLocation(Location myLocation) {
        location = myLocation;
    }

    public void setCaption(String storyCaption) {
        caption = storyCaption;
    }

    public void setDateTime(Date time) {
        dateTime = time;
    }

    public void setViews (int storyViews){
        views = storyViews;
    }

    public void setVotes(int storyVotes) {
        votes = storyVotes;
    }

    public void addView() { views++; }

    public void upVote() {
        votes++;
    }

    public void downVote() {
        votes--;
    }

    public int getViews() {
        return views;
    }

    public int getVotes() { return votes;}

    public String getUri(){
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

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("URI", uri);
        result.put("Location", location);
        result.put("Caption", caption);
        result.put("DateTime", dateTime);
        result.put("Votes", votes);
        result.put("Views",views);
        result.put("Featured", featured);
        result.put("Flagged", flagged);

        return result;
    }

}
