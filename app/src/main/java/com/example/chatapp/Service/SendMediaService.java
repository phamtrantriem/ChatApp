package com.example.chatapp.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.chatapp.R;
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
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class SendMediaService extends Service {

    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private String userID, chatID;
    private ArrayList<String> imagesList;
    private int MAX_PROGRESS;
    private FirebaseUser firebaseUser;

    public SendMediaService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userID = intent.getStringExtra("userID");
        chatID = intent.getStringExtra("chatID");
        imagesList = intent.getStringArrayListExtra("media");
        Log.d("LIST_IMAGE_INTENT", imagesList.toString());
        MAX_PROGRESS = imagesList.size();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        startForeground(100, getNotification().build());

        if (MAX_PROGRESS > 0) {
            for (int i = 0; i < imagesList.size(); i++) {
                String filename = compressImage(imagesList.get(i));
                Log.d("FILENAME0", filename);
                uploadImage(filename);
                builder.setProgress(MAX_PROGRESS, i+1, false);
                manager.notify(600, builder.build());
            }

            builder.setContentTitle("Sending completed").setProgress(0,0,false);
            manager.notify(600, builder.build());
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private NotificationCompat.Builder getNotification() {
        builder = new NotificationCompat.Builder(this, "Android")
                .setContentText("Sending Media")
                .setProgress(MAX_PROGRESS, 0, false)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(600, builder.build());
        return builder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel channel = new NotificationChannel("android", "Message", NotificationManager.IMPORTANCE_HIGH);
        channel.setShowBadge(true);
        channel.setLightColor(R.color.colorPrimary);
        channel.setDescription("Sending media");
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private String compressImage(String filename) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "ChatApp/Media" + System.currentTimeMillis());
        if (!file.exists()) {
            file.mkdirs();
        }
        return SiliCompressor.with(this).compress(filename, file, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uploadImage(String filename) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(
                chatID + "/Media/"
                        + firebaseUser.getUid()
                        + "/sent/"
                        + System.currentTimeMillis());
        Log.d("FILENAME", filename);
        Uri uri = Uri.fromFile(new File(filename));
        storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
           Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
           task.addOnCompleteListener(task1 -> {
              if (task1.isSuccessful()) {

                  String url = uri.toString();

                  DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                  HashMap<String, Object> hashMap = new HashMap<>();
                  hashMap.put("sender", firebaseUser.getUid());
                  hashMap.put("receiver", chatID);
                  hashMap.put("message", url);
                  hashMap.put("seen", false);

                  LocalDateTime current = LocalDateTime.now();
                  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");
                  String formatted = current.format(formatter);

                  hashMap.put("timeSend", formatted);
                  hashMap.put("timeSeen", "");
                  hashMap.put("type", "image");

                  reference.child("Chats").push().setValue(hashMap);
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
              }
           });
        });
    }
}
