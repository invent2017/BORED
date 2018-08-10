package com.projectbored.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

/**
 * Created by LikHern on 2/1/2018.
 */

public class UserStoryListAdapter extends BaseAdapter {
    private Activity context;
    private ArrayList<String> storyKeys;

    private DataSnapshot dataSnapshot;

    public UserStoryListAdapter(Activity context, ArrayList<String> storyKeys, DataSnapshot dataSnapshot) {
        this.context = context;
        this.storyKeys = storyKeys;
        this.dataSnapshot = dataSnapshot;
    }

    private class ViewHolder {

        public LinearLayout linearLayout;
        public ImageView imageView;
        public TextView captionText, viewsNumber, votesNumber;
    }

    @Override
    public int getCount() {
        return storyKeys.size();
    }

    @Override
    public Object getItem(int i) {
        return storyKeys.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.user_story_row, viewGroup, false);
        }

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if(viewHolder == null) {
            viewHolder = new ViewHolder();
            viewHolder.linearLayout = (LinearLayout)view.findViewById(R.id.story_row);
            viewHolder.imageView = (ImageView) view.findViewById(R.id.my_story_image);
            viewHolder.captionText = (TextView)view.findViewById(R.id.my_story_caption);
            viewHolder.viewsNumber = (TextView)view.findViewById(R.id.views_number);
            viewHolder.votesNumber = (TextView)view.findViewById(R.id.votes_number);
        }

        viewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStoryDetails(storyKeys.get(i));
            }
        });

        String storyKey = storyKeys.get(i);
        String storyUri = dataSnapshot.child(storyKey).child("URI").getValue(String.class);

        StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(storyUri);
        Glide.with(context).load(mStorageRef).into(viewHolder.imageView);

        String storyCaption = dataSnapshot.child(storyKey).child("Caption").getValue(String.class);
        if(storyCaption == null) {
            viewHolder.captionText.setText("");
        } else {
            if (storyCaption.length() > 12) {
                storyCaption = storyCaption.substring(0, 8) + "...";
            }
            viewHolder.captionText.setText(storyCaption);
        }

        int views = dataSnapshot.child(storyKey).child("Views").getValue(Integer.class);
        viewHolder.viewsNumber.setText(Integer.toString(views));

        int votes = dataSnapshot.child(storyKey).child("Votes").getValue(Integer.class);
        viewHolder.votesNumber.setText(Integer.toString(votes));

        return view;
    }



    private void showStoryDetails(String storyKey) {
        Intent showDetails = new Intent(context, ShowStory.class);
        Bundle details = new Bundle();
        String[] locationArray = getStoryLocation(storyKey);
        details.putString("key", storyKey);
        details.putBoolean("FromProfile", true);
        details.putDouble("Latitude", Double.parseDouble(locationArray[0]));
        details.putDouble("Longitude", Double.parseDouble(locationArray[1]));
        showDetails.putExtras(details);
        context.startActivity(showDetails);
    }

    private String[] getStoryLocation(String storyKey) {
        String location = dataSnapshot.child(storyKey).child("Location").getValue(String.class);
        return location.split(",");
    }
}
