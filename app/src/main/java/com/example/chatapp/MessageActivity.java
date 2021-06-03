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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.chatapp.Adapter.MessageAdapter;
import com.example.chatapp.Notification.APIService;
import com.example.chatapp.Notification.Client;
import com.example.chatapp.Notification.Data;
import com.example.chatapp.Notification.Sender;
import com.example.chatapp.Notification.Token;
import com.example.chatapp.Object.Chat;
import com.example.chatapp.Object.User;
import com.example.chatapp.Service.Constaints;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;

    FirebaseUser firebaseUser;
    DatabaseReference reference, ref;

    Intent intent;
    ImageButton btn_send, btn_info;
    CircleImageView btn_send_file;
    LottieAnimationView typingAnimation;
    EditText txt_send;

    ValueEventListener seenListener;

    MessageAdapter messageAdapter;
    List<Chat> chatList;

    String userID;
    String chatID;
    String token;

    RecyclerView recyclerView;

    APIService apiService;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
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
        txt_send = findViewById(R.id.txt_message);
        btn_info = findViewById(R.id.btn_info);
        btn_send = findViewById(R.id.btn_send);
        btn_send_file = findViewById(R.id.btn_send_file);

        intent = getIntent();
        if (intent.hasExtra("chatID")) {
            chatID = intent.getStringExtra("chatID");
            userID = chatID;
        } else {
            userID = intent.getStringExtra("userID");
            chatID = userID;
        }



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
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        //info of user
        btn_info.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(MessageActivity.this, android.R.style.Theme_Black_NoTitleBar);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setContentView(R.layout.dialog_infomation);

            ImageView dialog_image = dialog.findViewById(R.id.profile_image);
            TextView dialog_username = dialog.findViewById(R.id.username);
            TextView dialog_name = dialog.findViewById(R.id.txtDesNameValue);
            TextView dialog_email = dialog.findViewById(R.id.txtDesEmailValue);
            TextView dialog_phone = dialog.findViewById(R.id.txtDesPhoneValue);
            ImageButton dialog_back = dialog.findViewById(R.id.btn_dialog_back);

            ref = FirebaseDatabase.getInstance().getReference("Users").child(userID);
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    assert user != null;
                    dialog_username.setText(user.getUsername());
                    dialog_name.setText(user.getName());
                    dialog_phone.setText(user.getPhone());
                    dialog_email.setText(user.getEmail());
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

        //check typing
        txt_send.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    typing("not");
                } else {
                    typing(userID);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        //send massage
        btn_send.setOnClickListener(v -> {
            String msg = txt_send.getText().toString();
            if (!msg.equals("")) {
                sendMessage(firebaseUser.getUid(), userID, msg);
                getToken(userID, chatID, msg);
            } else {
                Toast.makeText(MessageActivity.this, "Empty message!!", Toast.LENGTH_SHORT).show();
            }
            txt_send.setText("");
        });

        //send file
        btn_send_file.setOnClickListener(v -> {

        });
        checkTyping(userID);
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
        DatabaseReference chatReference1 = FirebaseDatabase.getInstance().getReference("ChatsList").child(firebaseUser.getUid()).child(userID);
        DatabaseReference chatReference2 = FirebaseDatabase.getInstance().getReference("ChatsList").child(userID).child(firebaseUser.getUid());
        chatReference1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatReference1.child("id").setValue(userID);
                    chatReference1.child("lastMessageDate").setValue(formatted);
                    chatReference2.child("id").setValue(firebaseUser.getUid());
                    chatReference2.child("lastMessageDate").setValue(formatted);
                } else {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("lastMessageDate", formatted);
                    chatReference1.updateChildren(hashMap);
                    chatReference2.updateChildren(hashMap);
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
                //sendNotification(receiver, user.getUsername(), msg);
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
        HashMap<String, Object> mapStatus = new HashMap<>();
        mapStatus.put("status", status);
        reference.updateChildren(mapStatus);
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

    private void typing(String typing) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        HashMap<String, Object> mapTyping = new HashMap<>();
        mapTyping.put("typing", typing);
        reference.updateChildren(mapTyping);
    }

    private void checkTyping(String userID) {
        DatabaseReference dRef = FirebaseDatabase.getInstance().getReference("Users").child(userID).child("typing");
        dRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String typing = Objects.requireNonNull(snapshot.getValue()).toString();
                    if (typing.equals(firebaseUser.getUid())) {
                        typingAnimation= findViewById(R.id.typing);
                        typingAnimation.setVisibility(View.VISIBLE);
                        typingAnimation.playAnimation();
                    } else {
                        if (typingAnimation != null) {
                            typingAnimation.pauseAnimation();
                            typingAnimation.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getToken(String userID, String chatID, String msg) {
        DatabaseReference dRef = FirebaseDatabase.getInstance().getReference("Users").child(userID);
        dRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //lost token
                Log.d("GET_TOKEN2", snapshot.toString());
                String userURL = Objects.requireNonNull(snapshot.child("imageURL").getValue()).toString();
                String name = Objects.requireNonNull(snapshot.child("name").getValue()).toString();

                if (snapshot.child("token").getValue().toString() != null) {
                    String token = Objects.requireNonNull(snapshot.child("token").getValue()).toString();
                    JSONObject to = new JSONObject();
                    JSONObject data = new JSONObject();
                    try {
                        data.put("title", name);
                        data.put("message", msg);
                        data.put("userID", userID);
                        data.put("userURL", userURL);
                        data.put("chatID", chatID);
                        to.put("to", token);
                        to.put("data", data);

                        sendNotification(to);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendNotification(JSONObject to) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Constaints.NOTIFICATION_URL, to, response -> {
            Log.d("NOTIFICATION", "send notification response: " + response);

        }, error -> {
            Log.d("NOTIFICATION", "send notification error: " +error);
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                map.put("Authorization", "key=" + Constaints.AUTHORIZATION);
                map.put("Content-Type", "application/json");
                return map;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private void sendNotification1(String userID, String chatID, String msg, String userURL) {

        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(userID);
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
}