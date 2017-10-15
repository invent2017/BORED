package com.projectbored.app;

import android.location.Location;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by LikHern on 15/10/2017.
 */

public class Story {
    public Uri uri;
    public Location location;
    public String caption;
    public Date dateTime;

    public Story (Uri u, Location myLocation, String snippet, Date dateTime) {
        uri = u;
        location = myLocation;
        caption = new String(snippet);
        this.dateTime = dateTime;

    }

    public Location getLocation() {
        return location;
    }

    public String getCaption() {
        return caption;
    }


}
