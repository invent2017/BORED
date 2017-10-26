package com.projectbored.app;

import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Story implements Parcelable {
    private String uri;
    private Location location;
    private String caption;
    private Date dateTime;
    private int votes;

    @Exclude
    private int mData;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags){
        out.writeInt(mData);
    }

    public Story() {

    }

    public Story (Uri u, Location myLocation, String snippet, Date dateTime) {
        uri = u.toString();
        location = myLocation;
        caption = snippet;
        this.dateTime = dateTime;
        votes = 0;
    }

    public Story(String u, Location myLocation, String snippet, Date dateTime){
        uri = u;
        location = myLocation;
        caption = snippet;
        this.dateTime = dateTime;
        votes = 0;

    }

    private Story(Parcel in) {
        mData = in.readInt();
    }

    public static final Parcelable.Creator<Story> CREATOR = new Parcelable.Creator<Story>() {
        public Story createFromParcel(Parcel in) {
            return new Story(in);
        }

        public Story[] newArray(int size) {
            return new Story[size];
        }
    };

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

    public void upVote() {
        votes++;
    }

    public void downVote() {
        votes--;
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

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("URI", uri);
        result.put("Location", location);
        result.put("Caption", caption);
        result.put("DateTime", dateTime);

        return result;
    }


}
