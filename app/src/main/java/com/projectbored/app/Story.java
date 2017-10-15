package com.projectbored.app;

import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by LikHern on 15/10/2017.
 */

public class Story {
    public Uri uri;
    public LatLng location;
    public String caption;

    public Story (Uri u, Double latitude, Double longitude, String snippet) {
        uri = u;
        location = new LatLng(latitude, longitude);
        caption = new String(snippet);

    }

    public Story (Uri u,LatLng myLoc, String snippet) {
        uri = u;
        location = myLoc;
        caption = new String(snippet);
    }
}
