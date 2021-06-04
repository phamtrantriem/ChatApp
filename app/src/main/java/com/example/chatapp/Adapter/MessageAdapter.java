package com.example.chatapp.Adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.GlideApp;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.Object.Chat;
import com.example.chatapp.Object.User;
import com.example.chatapp.R;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    final private Context mContext;
    final private List<Chat> chatList;
    final private String imageURL;
    final private String type;

    FirebaseUser firebaseUser;

    public MessageAdapter(Context mContext, List<Chat> chatList, String imageURL, String type) {
        this.mContext = mContext;
        this.chatList = chatList;
        this.imageURL = imageURL;
        this.type = type;
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
        Log.d("MESSAGE_ADAPTER", chat.toString());
        if (chat.getType().equals("text")) {
            holder.show_message.setText(chat.getMessage());
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message_image.setVisibility(View.GONE);
            holder.show_message_audio.setVisibility(View.GONE);
        } else if (chat.getType().equals("image")) {
            Glide.with(mContext).load(chat.getMessage()).into(holder.show_message_image);
            holder.show_message_image.setVisibility(View.VISIBLE);
            holder.show_message.setVisibility(View.GONE);
            holder.show_message_audio.setVisibility(View.GONE);
            holder.show_message_image.setOnClickListener(v -> {
                final Dialog dialog = new Dialog(v.getContext(), android.R.style.Theme_Black_NoTitleBar);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCanceledOnTouchOutside(true);
                dialog.setContentView(R.layout.dialog_image);

                PhotoView photo_view = dialog.findViewById(R.id.photo_view);
                ImageButton dialog_back = dialog.findViewById(R.id.btn_dialog_back);

                GlideApp.with(v.getContext()).load(chat.getMessage()).into(photo_view);

                dialog_back.setOnClickListener(v1 -> dialog.dismiss());
                dialog.show();
            });
        } else if (chat.getType().equals("audio")) {
            holder.show_message_audio.setVisibility(View.VISIBLE);
            holder.show_message.setVisibility(View.GONE);
            holder.show_message_image.setVisibility(View.GONE);
            holder.btn_play_message.setOnClickListener(v -> {
                if (chat.getSender().equals(firebaseUser.getUid())) {
                    holder.btn_play_message.setImageResource(R.drawable.ic_baseline_pause_white);
                } else {
                    holder.btn_play_message.setImageResource(R.drawable.ic_baseline_pause_black);
                }
                String source = chat.getMessage();
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(source);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    holder.btn_play_message.setClickable(false);
                    mediaPlayer.setOnCompletionListener(mp -> {
                        if (chat.getSender().equals(firebaseUser.getUid())) {
                            holder.btn_play_message.setImageResource(R.drawable.ic_baseline_play_white);
                        } else {
                            holder.btn_play_message.setImageResource(R.drawable.ic_baseline_play_black);
                        }
                        mediaPlayer.stop();
                        holder.btn_play_message.setClickable(true);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        if (imageURL.equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(imageURL).into(holder.profile_image);
        }
        //check for last message and show the time delivering or seen
        if (position == chatList.size()-1) {
            if (chat.isSeen()) {
                String string = "Seen at";
                holder.txt_seen.setText(string);
                holder.txt_seen.setVisibility(View.VISIBLE);
                if (!chat.getTimeSeen().equals("")) {
                    holder.txt_time_seen.setText(chat.getTimeSeen());
                    holder.txt_time_seen.setVisibility(View.VISIBLE);
                }
            } else {
                String string = "Delivered at";
                holder.txt_seen.setText(string);
                holder.txt_seen.setVisibility(View.VISIBLE);
                if (!chat.getTimeSend().equals("")) {
                    holder.txt_time_seen.setText(chat.getTimeSend());
                    holder.txt_time_seen.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message, txt_seen, txt_time_seen;
        public ImageView profile_image, show_message_image;
        public ImageButton btn_play_message;
        public LinearLayout show_message_audio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            txt_time_seen = itemView.findViewById(R.id.txt_time_seen);
            show_message_image = itemView.findViewById(R.id.show_message_image);
            btn_play_message = itemView.findViewById(R.id.btn_play_message);
            show_message_audio = itemView.findViewById(R.id.show_message_audio);
        }
    }

    @Override
    public int getItemViewType(int position) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        Chat chat = chatList.get(position);
        if (chat.getSender().equals(firebaseUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}
