package com.example.chatapp.Fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chatapp.Adapter.StoryAdapter;
import com.example.chatapp.Model.Story;
import com.example.chatapp.R;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;

public class StoryFragment extends Fragment {

    ViewPager2 viewPager2;
    StoryAdapter adapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_story, container, false);
        viewPager2 = (ViewPager2) view.findViewById(R.id.view_pager2);
        FirebaseRecyclerOptions<Story> options = new FirebaseRecyclerOptions.Builder<Story>()
                .setQuery(FirebaseDatabase.getInstance().getReference().child("Stories"), Story.class)
                .build();

        adapter = new StoryAdapter(options);
        viewPager2.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}