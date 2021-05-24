package com.example.chatapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Object.Chat;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    final private Context mContext;
    final private List<Chat> chatList;
    final private String imageURL;

    FirebaseUser firebaseUser;

    public MessageAdapter(Context mContext, List<Chat> chatList, String imageURL) {
        this.mContext = mContext;
        this.chatList = chatList;
        this.imageURL = imageURL;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_LEFT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.chats_item_left, parent, false);
        } else {
            view = LayoutInflater.from(mContext).inflate(R.layout.chats_item_right, parent, false);
        }
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        holder.show_message.setText(chat.getMessage());
        if (imageURL.equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(imageURL).into(holder.profile_image);
        }

    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message;
        public ImageView profile_image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
        }
    }

    @Override
    public int getItemViewType(int position) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        if (!chatList.get(position).equals(firebaseUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}
