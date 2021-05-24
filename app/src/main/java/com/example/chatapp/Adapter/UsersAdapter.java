package com.example.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.Object.User;
import com.example.chatapp.R;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    final private Context mContext;
    final private List<User> userList;

    public UsersAdapter(Context mContext, List<User> userList) {
        this.mContext = mContext;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.users_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersAdapter.ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.username.setText(user.getUsername());
        if (user.getImageURL().equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, MessageActivity.class);
            intent.putExtra("userID", user.getId());
            Toast.makeText(mContext, "userID" + user.getId(), Toast.LENGTH_SHORT).show();
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView username;
        public ImageView profile_image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username_users);
            profile_image = itemView.findViewById(R.id.profile_users_image);
        }
    }
}
