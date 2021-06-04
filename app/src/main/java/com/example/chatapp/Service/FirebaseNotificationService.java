package com.example.chatapp.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.chatapp.MessageActivity;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FirebaseNotificationService extends FirebaseMessagingService {

    private String userID = FirebaseAuth.getInstance().getUid();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getNotification() != null) {
            Map<String, String> map = remoteMessage.getData();
            String title = map.get("title");
            String message = map.get("message");
            String userID = map.get("userID");
            String userURL = map.get("userURL");
            String chatID = map.get("chatID");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createOreoNotification(getApplicationContext(), title, message, userID, userURL, chatID);
            } else {
                createNotification(title, message, userID, userURL, chatID);
            }
        }

    }

    @Override
    public void onNewToken(@NonNull String s) {
        sendRegistrationToServer(s);
        Log.d("TOKEN_IN_SERVICE", s);
        super.onNewToken(s);
    }

    private void sendRegistrationToServer(String token) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(userID);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> map = new HashMap<>();
                map.put("token", token);
                reference.updateChildren(map);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void createNotification (String title, String message, String userID, String userURL, String chatId) {

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constaints.CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ResourcesCompat.getColor(getResources(),R.color.colorPrimary, null))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setSound(uri);

        //open activity when click noti
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("userID", chatId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createOreoNotification (Context context, String title, String message, String userID, String userURL, String chatID) {


        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("userID", chatID);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Notification notification = new NotificationCompat.Builder(this, Constaints.CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(ResourcesCompat.getColor(getResources(),R.color.colorPrimary, null))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify(getNotificationID(), notification);
    }

    private int getNotificationID() {
        return (int) new Date().getTime();
    }
}
