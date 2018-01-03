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
 * Created by LikHern on 3/1/2018.
 */

public class BookmarkedStoryListAdapter extends BaseAdapter {
    private Activity context;
    private ArrayList<String> storyKeys;

    private DataSnapshot dataSnapshot;

    public BookmarkedStoryListAdapter(Activity context, ArrayList<String> storyKeys, DataSnapshot dataSnapshot) {
        this.context = context;
        this.storyKeys = storyKeys;
        this.dataSnapshot = dataSnapshot;
    }

    private class ViewHolder {

        public LinearLayout linearLayout;
        public ImageView imageView;
        public TextView captionText;
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
            view = LayoutInflater.from(context).inflate(R.layout.bookmarked_story_row, viewGroup, false);
        }

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if(viewHolder == null) {
            viewHolder = new ViewHolder();
            viewHolder.linearLayout = (LinearLayout)view.findViewById(R.id.bookmarked_story_row);
            viewHolder.imageView = (ImageView) view.findViewById(R.id.bookmarked_story_image);
            viewHolder.captionText = (TextView)view.findViewById(R.id.bookmarked_story_caption);
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
        Glide.with(context).using(new FirebaseImageLoader()).load(mStorageRef).into(viewHolder.imageView);

        String storyCaption = dataSnapshot.child(storyKey).child("Caption").getValue(String.class);
        if(storyCaption == null) {
            viewHolder.captionText.setText("");
        } else {
            if (storyCaption.length() > 30) {
                storyCaption = storyCaption.substring(0, 26) + "...";
            }
            viewHolder.captionText.setText(storyCaption);
        }

        return view;
    }


    private void showStoryDetails(String storyKey) {
        Intent showDetails = new Intent(context, ShowStory.class);
        Bundle details = new Bundle();
        details.putString("key", storyKey);
        details.putBoolean("FromProfile", true);
        showDetails.putExtras(details);
        context.startActivity(showDetails);
    }
}
