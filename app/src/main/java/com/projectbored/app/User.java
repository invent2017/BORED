package com.projectbored.app;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by LikHern on 31/10/2017.
 */

@IgnoreExtraProperties
public class User {
    private String username;
    private String email;
    private String password;
    private boolean admin;
    private int views;
    private int viewed;
    private int upvotes;
    private int upvoted;

    public User(){

    }

    public User(String username, String email, String password){
        this.username = username;
        this.email = email;
        this.password = password;
        admin = false;
        views = 0;
        viewed = 0;
        upvotes = 0;
        upvoted = 0;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("Username", username);
        result.put("Email", email);
        result.put("Password", password);
        result.put("Admin", admin);
        result.put("Views", views);
        result.put("Viewed", viewed);
        result.put("Upvotes", upvotes);
        result.put("Upvoted", upvoted);

        return result;
    }
}
