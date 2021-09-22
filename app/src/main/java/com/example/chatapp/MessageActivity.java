package com.example.chatapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.example.chatapp.Model.Chat;
import com.example.chatapp.Model.User;
import com.example.chatapp.Service.Constants;
import com.fxn.pix.Options;
import com.fxn.pix.Pix;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MessageActivity extends AppCompatActivity {

    //header
    CircleImageView profile_image;
    TextView username;

    FirebaseUser firebaseUser;
    DatabaseReference dReference, iReference;
    StorageReference sReference;

    Intent intent;

    //body
    ImageButton btn_send, btn_info, btn_send_file;
    LottieAnimationView typingAnimation;
    EditText txt_send;
    RelativeLayout input_layout;
    ImageView ic_camera, ic_gallery, ic_micro;
    LinearLayout input_text_layout;
    MediaRecorder mediaRecorder;

    ValueEventListener seenListener;
    MessageAdapter messageAdapter;
    List<Chat> chatList;

    private Uri imageUri;
    private Uri audioUri;
    private String extension;
    StorageTask<UploadTask.TaskSnapshot> uploadImageTask, uploadAudioTask;

    String userID;
    String chatID;
    public static String filename = "recorder.3gp";

    String audioFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DCIM + File.separator + filename;

    RecyclerView recyclerView;

    private ArrayList<String> selectedImages;

    private ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int resultCode = result.getResultCode();
                Intent data = result.getData();
                if (resultCode == Constants.RESULT_OK && data != null && data.getData() != null) {
                    imageUri = data.getData();
                    Log.d("MESSAGE_ACT_URI", imageUri.toString());
                    if (uploadImageTask != null && uploadImageTask.isInProgress()) {
                        Toast.makeText(getApplicationContext(), "Upload in progress", Toast.LENGTH_SHORT).show();
                    } else {
                        uploadImage();
                    }
                }
            });

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

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
        input_layout = findViewById(R.id.input_layout);
        ic_camera = findViewById(R.id.ic_camera);
        ic_gallery = findViewById(R.id.ic_gallery);
        ic_micro = findViewById(R.id.ic_micro);
        input_text_layout = findViewById(R.id.input_text_layout);

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
        dReference = FirebaseDatabase.getInstance().getReference("Users").child(userID);
        dReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                assert user != null;
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")) {
                    profile_image.setImageResource(R.drawable.ic_baseline_person_24);
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

            ImageView dialog_image = dialog.findViewById(R.id.profile_image);
            TextView dialog_username = dialog.findViewById(R.id.username);
            TextView dialog_name = dialog.findViewById(R.id.txtDesNameValue);
            TextView dialog_email = dialog.findViewById(R.id.txtDesEmailValue);
            TextView dialog_phone = dialog.findViewById(R.id.txtDesPhoneValue);
            ImageButton dialog_back = dialog.findViewById(R.id.btn_dialog_back);

            //information of userID
            iReference = FirebaseDatabase.getInstance().getReference("Users").child(userID);
            iReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    assert user != null;
                    dialog_username.setText(user.getUsername());
                    dialog_name.setText(user.getName());
                    dialog_phone.setText(user.getPhone());
                    dialog_email.setText(user.getEmail());
                    if (user.getImageURL() != null && user.getImageURL().equals("default")) {
                        dialog_image.setImageResource(R.drawable.ic_baseline_person_24);
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    typing("not");
                } else {
                    typing(userID);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //send massage
        btn_send.setOnClickListener(v -> {
            String msg = txt_send.getText().toString();
            if (!msg.equals("")) {
                sendMessage(firebaseUser.getUid(), userID, msg, "text");
                getToken(userID, chatID, msg);
            } else {
                Toast.makeText(MessageActivity.this, "Empty message!!", Toast.LENGTH_SHORT).show();
            }
            txt_send.setText("");
        });

        //send file
        btn_send_file.setOnClickListener(v -> {
            if (input_layout.getVisibility() == View.INVISIBLE) {
                showInputLayout();
            } else {
                hideInputLayout();
            }
        });

        //input camera
        ic_camera.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                getCameraImage();
            } else {
                String[] micPermissions = {Manifest.permission.CAMERA};
                requestPermissions(micPermissions, Constants.CAMERA_REQUEST_CODE);
            }

        });

        //input gallery
        ic_gallery.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("*/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);

            someActivityResultLauncher.launch(intent);
        });

        //input recorder

        ic_micro.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if (checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    getRecordingMessage();
                } else {
                    String[] micPermissions = {Manifest.permission.MANAGE_EXTERNAL_STORAGE};
                    requestPermissions(micPermissions, Constants.STORAGE_REQUEST_CODE);
                }
            } else {
                String[] micPermissions = {Manifest.permission.RECORD_AUDIO};
                requestPermissions(micPermissions, Constants.RECORDING_REQUEST_CODE);
            }

            if (input_layout.getVisibility() == View.INVISIBLE) {
                showInputLayout();
            } else {
                hideInputLayout();
            }
        });

        checkTyping(userID);
        seenMessage(userID);
    }

    private void getRecordingMessage() {

        LayoutInflater inflater = LayoutInflater.from(MessageActivity.this);
        View view = inflater.inflate(R.layout.voice_option_layout, null);
        TextView status = view.findViewById(R.id.txt_record_status);
        Button start = view.findViewById(R.id.btn_record_start);
        Button stop = view.findViewById(R.id.btn_record_stop);
        Button send = view.findViewById(R.id.btn_record_send);

        AlertDialog alertDialog = new AlertDialog.Builder(MessageActivity.this)
                .setView(view)
                .create();

        alertDialog.show();
        stop.setClickable(false);
        send.setClickable(false);
        start.setOnClickListener(v -> {
            try {
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

                mediaRecorder.setOutputFile(audioFile);
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String status1 = "Audio recording...";
            status.setText(status1);
            start.setClickable(false);
            send.setClickable(false);
        });

        stop.setOnClickListener(v -> {
            if (mediaRecorder != null) {
                mediaRecorder.reset();
                mediaRecorder.release();
                status.setText("Recording stopped");
                start.setClickable(true);
                send.setClickable(true);
            }
        });

        send.setOnClickListener(v -> {
            audioUri = Uri.fromFile(new File(audioFile));
            sReference = FirebaseStorage.getInstance().getReference("audio/message");
            if (audioUri != null) {
                String s = String.valueOf(System.currentTimeMillis());
                final StorageReference audioReference = sReference.child(s + ".3gp");
                uploadAudioTask = audioReference.putFile(audioUri);
                uploadAudioTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    return audioReference.getDownloadUrl();
                }).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        assert downloadUri != null;
                        String mUri = downloadUri.toString();

                        sendMessage(firebaseUser.getUid(), userID, mUri, "audio");

                        alertDialog.dismiss();
                    } else {
                        Toast.makeText(getApplicationContext(), "Fails", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                });
            } else {
                Toast.makeText(getApplicationContext(), "No recording selected!!!", Toast.LENGTH_SHORT).show();
            }
            view.setVisibility(View.INVISIBLE);
        });

    }

    private void showInputLayout() {
        RelativeLayout relativeLayout = input_layout;
        float radius = Math.max(relativeLayout.getWidth(), relativeLayout.getHeight());

        Animator animator = ViewAnimationUtils.createCircularReveal(relativeLayout, relativeLayout.getLeft(), relativeLayout.getTop(), 0, radius * 2);
        animator.setDuration(100);
        relativeLayout.setVisibility(View.VISIBLE);
        animator.start();
    }

    private void hideInputLayout() {
        RelativeLayout relativeLayout = input_layout;
        float radius = Math.max(relativeLayout.getWidth(), relativeLayout.getHeight());

        Animator animator = ViewAnimationUtils.createCircularReveal(relativeLayout, relativeLayout.getLeft(), relativeLayout.getTop(), radius * 2, 0);
        animator.setDuration(100);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                relativeLayout.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
    }

    private void seenMessage(final String userID) {
        dReference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = dReference.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    assert chat != null;
                    if (chat.getReceiver() != null && chat.getSender() != null) {
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendMessage(String sender, String receiver, String message, String type) {
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
        hashMap.put("type", type);

        reference.child("Chats").push().setValue(hashMap);
        //add user to chat fragment
        DatabaseReference chatReference1 = FirebaseDatabase.getInstance().getReference("ChatsList").child(firebaseUser.getUid()).child(userID);
        DatabaseReference chatReference2 = FirebaseDatabase.getInstance().getReference("ChatsList").child(userID).child(firebaseUser.getUid());
        chatReference1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.child("id").exists()) {
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
    }

    private void readMessage(String currentID, String userID, String imageURL) {
        chatList = new ArrayList<>();

        dReference = FirebaseDatabase.getInstance().getReference("Chats");
        dReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    assert chat != null;
                    if (chat.getSender() != null && chat.getReceiver() != null) {
                        if ((chat.getReceiver().equals(currentID) && chat.getSender().equals(userID)) ||
                                (chat.getReceiver().equals(userID) && chat.getSender().equals(currentID))) {
                            chatList.add(chat);
                        }
                    }
                }
                messageAdapter = new MessageAdapter(MessageActivity.this, chatList, imageURL);
                messageAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(messageAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void status(String status) {
        dReference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        HashMap<String, Object> mapStatus = new HashMap<>();
        mapStatus.put("status", status);
        dReference.updateChildren(mapStatus);
    }

    private void typing(String typing) {
        dReference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        HashMap<String, Object> mapTyping = new HashMap<>();
        mapTyping.put("typing", typing);
        dReference.updateChildren(mapTyping);
    }

    private void checkTyping(String userID) {
        DatabaseReference dRef = FirebaseDatabase.getInstance().getReference("Users").child(userID).child("typing");
        dRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String typing = Objects.requireNonNull(snapshot.getValue()).toString();
                    if (typing.equals(firebaseUser.getUid())) {
                        typingAnimation = findViewById(R.id.typing);
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

                if (!Objects.requireNonNull(snapshot.child("token").getValue()).toString().equals("default")) {
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
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Constants.NOTIFICATION_URL, to, response -> {
            Log.d("NOTIFICATION", "send notification response: " + response);

        }, error -> {
            Log.d("NOTIFICATION", "send notification error: " + error);
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                map.put("Authorization", "key=" + Constants.AUTHORIZATION);
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

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (seenListener != null) {
            dReference.removeEventListener(seenListener);
        }
        status("offline");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 300) {
            if (data != null) {
                selectedImages = data.getStringArrayListExtra(Pix.IMAGE_RESULTS);

//                Intent intent = new Intent(MessageActivity.this, SendMediaService.class);
//                intent.putExtra("userID", userID);
//                intent.putExtra("chatID", chatID);
//                intent.putStringArrayListExtra("media", selectedImages);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    MessageActivity.this.stopService(intent);
//                    ContextCompat.startForegroundService(this, intent);
//                } else {
//                    startService(intent);
//                }
                final ProgressDialog dialog = new ProgressDialog(MessageActivity.this);
                dialog.setMessage("Uploading...");
                dialog.show();
                for (String cameraImageURL : selectedImages) {
                    StorageReference camReference = FirebaseStorage.getInstance().getReference("images/message");
                    if (cameraImageURL != null) {
                        Uri uri = Uri.fromFile(new File(cameraImageURL));
                        String s = String.valueOf(System.currentTimeMillis());
                        final StorageReference fileReference = camReference.child(s + "." + getFileExtension(uri));

                        uploadImageTask = fileReference.putFile(uri);
                        uploadImageTask.continueWithTask(task -> {
                            if (!task.isSuccessful()) {
                                throw Objects.requireNonNull(task.getException());
                            }
                            return fileReference.getDownloadUrl();
                        }).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                assert downloadUri != null;
                                String mUri = downloadUri.toString();

                                sendMessage(firebaseUser.getUid(), userID, mUri, "image");

                                dialog.dismiss();
                            } else {
                                Toast.makeText(getApplicationContext(), "Fails", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } else {
                        Toast.makeText(getApplicationContext(), "No image selected!!!", Toast.LENGTH_SHORT).show();
                    }
                    input_layout.setVisibility(View.INVISIBLE);
                }
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            //case PermUtil.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
            case Constants.CAMERA_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCameraImage();
                } else {
                    Toast.makeText(this, "Approve permissions to open Pix ImagePicker", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case Constants.RECORDING_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getRecordingMessage();
                } else
                    Toast.makeText(this, "Recording permission denied", Toast.LENGTH_SHORT).show();
                break;
            }
            case Constants.STORAGE_REQUEST_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getRecordingMessage();
                    Log.d("PERMISSION1", "");
                } else {
                    if (Environment.isExternalStorageManager()) {
                        getRecordingMessage();
                        Log.d("PERMISSION2", String.valueOf(Environment.isExternalStorageManager()));
                    } else {
                        Log.d("PERMISSION3", "");
                        Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                        //request for the permission
                        Intent intent1 = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri1 = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri1);
                        startActivity(intent1);
                    }
                }
                break;
            }
        }
    }

    private void getCameraImage() {
        Options options = Options.init()
                .setRequestCode(300)                                           //Request code for activity results
                .setCount(5)                                                   //Number of images to restict selection count
                .setFrontfacing(false)                                         //Front Facing camera on start
                .setExcludeVideos(true)
                .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT)     //Orientaion
                .setPath("/ChatApp/Media");                                       //Custom Path For media Storage
        if (selectedImages != null) {
            options.setPreSelectedUrls(selectedImages);
        }
        Pix.start(this, options);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uploadImage() {
        final ProgressDialog dialog = new ProgressDialog(MessageActivity.this);
        dialog.setMessage("Uploading...");
        dialog.show();
        if (imageUri != null) {
            extension = getFileExtension(imageUri);
            Log.d("EXTENSION", extension);
            if (extension.equals("mp4")) {
                sReference = FirebaseStorage.getInstance().getReference("videos/message");
            } else if (extension.equals("3gp")) {
                sReference = FirebaseStorage.getInstance().getReference("audio/message");
            } else {
                sReference = FirebaseStorage.getInstance().getReference("images/message");
            }

            String s = String.valueOf(System.currentTimeMillis());
            final StorageReference fileReference = sReference.child(s + "." + getFileExtension(imageUri));

            uploadImageTask = fileReference.putFile(imageUri);
            uploadImageTask.continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }
                return fileReference.getDownloadUrl();
            }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    assert downloadUri != null;
                    String mUri = downloadUri.toString();
                    if (extension.equals("mp4")) {
                        sendMessage(firebaseUser.getUid(), userID, mUri, "video");
                    } else if (extension.equals("3gp")) {
                        sendMessage(firebaseUser.getUid(), userID, mUri, "audio");
                    } else {
                        sendMessage(firebaseUser.getUid(), userID, mUri, "image");
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Fails", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        } else {
            Toast.makeText(getApplicationContext(), "No image selected!!!", Toast.LENGTH_SHORT).show();
        }
        input_layout.setVisibility(View.INVISIBLE);
    }

    private String getFileExtension(Uri imageUri) {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(imageUri));
    }
}