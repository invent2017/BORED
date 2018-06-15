package com.projectbored.app;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class SearchAdapter extends BaseAdapter
        implements Filterable{
    private Activity context;
    private ArrayList<String> initialResults;
    private ArrayList<String> hashtags;
    private ArrayList<String> searchResults;

    public SearchAdapter(Activity context, ArrayList<String> hashtags) {
        this.context = context;
        this.hashtags = hashtags;

        initialResults = new ArrayList<>();
        initialResults.add(0, "SUGGESTIONS");
        initialResults.add(1, "Nearby");
        initialResults.add(2, "Today's squawks");
        initialResults.add(3, "My squawks");
        initialResults.add(4, "Read squawks");
        initialResults.add(5, "POPULAR HASHTAGS");

        searchResults = initialResults;

        for(int i = 0 ; i < hashtags.size(); i++) {
            initialResults.add(hashtags.get(i));
        }

    }

    /*private ArrayList<String> initialiseDefaultResults() {
        this.hashtags = hashtags;

        ArrayList<String> defaultResults = new ArrayList<>();
        defaultResults.add(0, "SUGGESTIONS");
        defaultResults.add(1, "Nearby");
        defaultResults.add(2, "Today's squawks");
        defaultResults.add(3, "My squawks");
        defaultResults.add(4, "Read squawks");
        defaultResults.add(5, "POPULAR HASHTAGS");

        for(int i = 0 ; i < hashtags.size(); i++) {
            initialResults.add(hashtags.get(i));
        }

        return defaultResults;
    }*/

    @Override
    public Object getItem(int i) {
        return searchResults.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getCount() {
        return searchResults.size();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.suggestion_row, viewGroup, false);

        }

        //idk why it seems like setClickable(true) makes it unclickable
        if(i == 0 || i == 5) {
            view.setClickable(true);
        } else {
            view.setClickable(false);
        }

        TextView textView = view.findViewById(R.id.suggestionText);
        textView.setText(searchResults.get(i));

        return view;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                FilterResults filterResults = new FilterResults();

                if(charSequence == null || charSequence.length() == 0) {
                    filterResults.values = hashtags;
                    filterResults.count = hashtags.size();
                } else {
                    ArrayList<String> filteredSearches = new ArrayList<>();
                    for(String hashtag : hashtags) {
                        if(hashtag.toUpperCase().contains(charSequence.toString().toUpperCase())) {
                            filteredSearches.add(hashtag);
                        }
                    }
                    filterResults.values = filteredSearches;
                    filterResults.count = filteredSearches.size();
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                searchResults = initialResults;
                searchResults.addAll((ArrayList<String>)filterResults.values);
                notifyDataSetChanged();
            }

        };


    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }
}
