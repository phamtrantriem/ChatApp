package com.example.chatapp.Adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.Story;
import com.example.chatapp.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

public class StoryAdapter extends FirebaseRecyclerAdapter<Story, StoryAdapter.ViewHolder> {


    public StoryAdapter(@NonNull FirebaseRecyclerOptions<Story> options) {
        super(options);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Story model) {
        holder.setDataStory(model);
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        VideoView videoView;
        ImageView imageView;
        ProgressBar progressBar;
        TextView username, content, time;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = (VideoView) itemView.findViewById(R.id.videoViewStory);
            imageView = (ImageView) itemView.findViewById(R.id.imageViewStory);
            progressBar = (ProgressBar) itemView.findViewById(R.id.videoProgressBar);
            username = (TextView) itemView.findViewById(R.id.txt_username);
            content = (TextView) itemView.findViewById(R.id.txt_content);
            time = (TextView) itemView.findViewById(R.id.txt_time);
        }
        public void setDataStory(Story story) {
            switch (story.getType()) {
                case "image":
                    videoView.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    Glide.with(itemView.getContext()).load(story.getUrl()).into(imageView);
                    break;
                case "video":
                    videoView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                    videoView.setVideoPath(story.getUrl());
                    videoView.setOnPreparedListener(mp -> {
                        progressBar.setVisibility(View.GONE);
                        mp.start();
                    });
                    videoView.setOnCompletionListener(MediaPlayer::start);
                    break;
            }
            if (story.getUsername() != null) {
                username.setText(story.getUsername());
            } else {
                username.setText("thuhuong");
            }
            content.setText(story.getContent());
            time.setText(story.getTime());
            Log.d("STORY_ADAPTER", story.toString());
        }
    }
}
