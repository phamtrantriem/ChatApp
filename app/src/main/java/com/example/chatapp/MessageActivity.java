package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Adapter.MessageAdapter;
import com.example.chatapp.Fragment.APIService;
import com.example.chatapp.Notification.Client;
import com.example.chatapp.Notification.Data;
import com.example.chatapp.Notification.MyResponse;
import com.example.chatapp.Notification.Sender;
import com.example.chatapp.Notification.Token;
import com.example.chatapp.Object.Chat;
import com.example.chatapp.Object.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;

    FirebaseUser firebaseUser;
    DatabaseReference reference, ref;

    Intent intent;
    ImageButton btn_send, btn_info;
    EditText txt_send;

    ValueEventListener seenListener;

    MessageAdapter messageAdapter;
    List<Chat> chatList;

    String userID;

    RecyclerView recyclerView;

    APIService apiService;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        recyclerView = findViewById(R.id.recycler_messages);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        txt_send = findViewById(R.id.txt_message);
        btn_info = findViewById(R.id.btn_info);

        intent = getIntent();

        userID = intent.getStringExtra("userID");

        //get receiver
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                assert user != null;
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")) {
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }
                readMessage(firebaseUser.getUid(), userID, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        //info of user
        btn_info.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(MessageActivity.this, android.R.style.Theme_Black_NoTitleBar);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setContentView(R.layout.dialog_infomation);

            TextView dialog_username = dialog.findViewById(R.id.username);
            TextView dialog_email = dialog.findViewById(R.id.txtDesEmailValue);
            ImageView dialog_image = dialog.findViewById(R.id.profile_image);
            ImageButton dialog_back = dialog.findViewById(R.id.btn_dialog_back);

            ref = FirebaseDatabase.getInstance().getReference("Users").child(userID);
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    assert user != null;

                    dialog_username.setText(user.getUsername());
                    String email = user.getUsername()+"@gmail.com";
                    dialog_email.setText(email);
                    if (user.getImageURL() != null && user.getImageURL().equals("default")) {
                        dialog_image.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(getApplicationContext()).load(user.getImageURL()).into(dialog_image);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            dialog_back.setOnClickListener(v1 -> dialog.dismiss());

            dialog.show();
        });

        //send massage
        btn_send.setOnClickListener(v -> {
            String msg = txt_send.getText().toString();
            if (!msg.equals("")) {
                sendMessage(firebaseUser.getUid(), userID, msg);
            } else {
                Toast.makeText(MessageActivity.this, "Empty message!!", Toast.LENGTH_SHORT).show();
            }
            txt_send.setText("");
        });

        seenMessage(userID);
    }

    private void seenMessage(final String userID) {
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    assert chat != null;
                    if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userID)) {
                        if (!chat.isSeen()) {
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("seen", true);

                            LocalDateTime current = LocalDateTime.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                            String formatted = current.format(formatter);

                            hashMap.put("timeSeen", formatted);
                            dataSnapshot.getRef().updateChildren(hashMap);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendMessage(String sender, String receiver, String message) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("seen", false);

        LocalDateTime current = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");
        String formatted = current.format(formatter);

        hashMap.put("timeSend", formatted);
        hashMap.put("timeSeen", "");

        reference.child("Chats").push().setValue(hashMap);

        //add user to chat fragment
        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference("ChatsList").child(firebaseUser.getUid()).child(userID);
        chatReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatReference.child("id").setValue(userID);
                    chatReference.child("lastMessageDate").setValue(formatted);
                } else {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("lastMessageDate", formatted);
                    chatReference.updateChildren(hashMap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final String msg = message;

        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                assert user != null;
                sendNotification(receiver, user.getUsername(), msg);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendNotification(String receiver, String username, String msg) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Token token = dataSnapshot.getValue(Token.class);
                    Data data = new Data(firebaseUser.getUid(), R.mipmap.ic_launcher, username + ": " + msg, "New message", userID);

                    assert token != null;
                    Sender sender = new Sender(data, token.getToken());

//                    apiService.sendNotification(sender)
//                            .enqueue(new Callback<MyResponse>() {
//                                @Override
//                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
//                                    if (response.code() == 200) {
//                                        if (response.body().success == 1) {
//                                            Toast.makeText(MessageActivity.this, "Failed", Toast.LENGTH_SHORT).show();
//                                        }
//                                    };
//                                }
//
//                                @Override
//                                public void onFailure(Call<MyResponse> call, Throwable t) {
//
//                                }
//                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void readMessage(String myID, String userID, String imageURL) {
        chatList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    assert chat != null;
                    Log.d("MESSAGE_ACTIVITY", chat.toString());
                    if ((chat.getReceiver().equals(myID) && chat.getSender().equals(userID)) ||
                            (chat.getReceiver().equals(userID) && chat.getSender().equals(myID))) {
                        chatList.add(chat);
                    }
                    messageAdapter = new MessageAdapter(MessageActivity.this, chatList, imageURL);
                    messageAdapter.notifyDataSetChanged();
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (seenListener!=null) {
            reference.removeEventListener(seenListener);
        }

        status("offline");
    }
}