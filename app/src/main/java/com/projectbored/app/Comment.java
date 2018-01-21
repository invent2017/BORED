package com.projectbored.app;

import com.google.firebase.database.Exclude;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by LikHern on 6/1/2018.
 */

public class Comment {
    private String user;
    private String story;
    private String string;
    private Calendar date;

    public Comment(String user, String story, String string) {
        this.user = user;
        this.story = story;
        this.string = string;
        this.date = Calendar.getInstance();
    }

    public Comment(String user, String story, String string, Calendar date) {
        this.user = user;
        this.story = story;
        this.string = string;
        this.date = date;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("User", user);
        result.put("Story", story);
        result.put("String", string);
        result.put("Date", date.getTimeInMillis());

        return result;
    }
}
