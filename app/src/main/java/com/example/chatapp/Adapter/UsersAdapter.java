package com.example.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Element.BottomSheetUser;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.Object.Chat;
import com.example.chatapp.Object.User;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    final private Context mContext;
    final private List<User> userList;
    final private boolean isChat;
    private FragmentManager fm;

    String lastMessage, userWithLastMessageID, timeLastMessage, type;
    private boolean isSeen;

    public UsersAdapter(Context mContext, List<User> userList, boolean isChat) {
        this.mContext = mContext;
        this.userList = userList;
        this.isChat = isChat;
    }

    public UsersAdapter(Context mContext, List<User> userList, boolean isChat, FragmentManager fm) {
        this.mContext = mContext;
        this.userList = userList;
        this.isChat = isChat;
        this.fm = fm;
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

        if (isChat) {
            lastMessage(user.getId(), holder.last_message, holder.time_last_message, holder.username, holder.ic_not_seen);

            if (user.getStatus().equals("online")) {
                holder.img_onl.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            } else {
                holder.img_onl.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        } else {
            holder.last_message.setText(user.getName());
            holder.img_onl.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);
            holder.time_last_message.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, MessageActivity.class);
            intent.putExtra("chatID", user.getId());
            mContext.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            BottomSheetUser bottomSheetUser = new BottomSheetUser(user.getId());
            bottomSheetUser.show(fm, "bottom");
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView username, last_message, time_last_message;
        public ImageView profile_image, img_onl, img_off, ic_not_seen;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username_users);
            profile_image = itemView.findViewById(R.id.profile_users_image);
            img_onl = itemView.findViewById(R.id.img_onl);
            img_off = itemView.findViewById(R.id.img_off);
            last_message = itemView.findViewById(R.id.last_message);
            time_last_message = itemView.findViewById(R.id.time_last_message);
            ic_not_seen = itemView.findViewById(R.id.ic_not_seen);
        }
    }

    //check last message
    private void lastMessage(String userID, TextView last_message, TextView time_last_message, TextView username, ImageView ic_not_seen) {
        lastMessage = "default";
        userWithLastMessageID = "";
        timeLastMessage = "00:00";
        type = "";

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (fUser != null) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Chat chat = dataSnapshot.getValue(Chat.class);
                        assert chat != null;
                        if (chat.getReceiver().equals(fUser.getUid()) && chat.getSender().equals(userID) ||
                                chat.getReceiver().equals(userID) && chat.getSender().equals(fUser.getUid())) {
                            lastMessage = chat.getMessage();
                            userWithLastMessageID = chat.getSender();
                            timeLastMessage = chat.getTimeSend().substring(0,5);
                            isSeen = chat.isSeen();
                            type = chat.getType();
                        }
                    }
                    if ("default".equals(lastMessage)) {
                        last_message.setText("");
                        time_last_message.setText("");
                    } else {
                        if (userWithLastMessageID.equals(fUser.getUid()) && type != null) {
                            if (type.equals("image")) {
                                lastMessage = "You: Sent an image";
                            } else if (type.equals("audio")) {
                                lastMessage = "You: Sent a voice record";
                            } else {
                                lastMessage = "You: " + lastMessage;
                            }
                        } else if (type != null){
                            if (type.equals("image")) {
                                lastMessage = "Sent an image";
                            } else if (type.equals("audio")) {
                                lastMessage = "Sent a voice record";
                            }
                            if (!isSeen) {
                                username.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark));
                                last_message.setTypeface(Typeface.DEFAULT_BOLD);
                                ic_not_seen.setVisibility(View.VISIBLE);
                            }
                        }
                        last_message.setText(lastMessage);
                        time_last_message.setText(timeLastMessage);
                    }
                    lastMessage = "default";
                    userWithLastMessageID = "";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
