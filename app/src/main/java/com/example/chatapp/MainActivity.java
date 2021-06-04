package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.bumptech.glide.Glide;
import com.example.chatapp.Fragment.ChatsFragment;
import com.example.chatapp.Fragment.ProfileFragment;
import com.example.chatapp.Fragment.StoryFragment;
import com.example.chatapp.Fragment.UsersFragment;
import com.example.chatapp.Object.Chat;
import com.example.chatapp.Object.User;
import com.example.chatapp.Service.FirebaseNotificationService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;

    FirebaseUser firebaseUser;
    DatabaseReference reference;

    int unRead;
    int selectedTab;

    ProgressDialog progressDialog;
    CountDownTimer countDownTimer;
    int i = 0;

    @Override
    protected void onStart() {
        super.onStart();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(MainActivity.this, StartActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait ...");
        progressDialog.setCancelable(false);
        progressDialog.setProgress(i);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);

        //token
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MAIN_ACTIVITY", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    FirebaseNotificationService firebaseNotificationService = new FirebaseNotificationService();
                    assert token != null;
                    firebaseNotificationService.onNewToken(token);
                    // Log and toast
                    Log.d("TOKEN", token);
                });

//        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        //show own username and profile picture
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser fUser = firebaseAuth.getCurrentUser();
            if (fUser != null) {
                reference = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());
                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        assert user != null;
                        if (user.getUsername() != null) {
                            username.setText(user.getUsername());
                        }

                        if (user.getImageURL().equals("default")) {
                            profile_image.setImageResource(R.mipmap.ic_launcher);
                        } else {
                            Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        //bottom navigation
        AHBottomNavigation bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);
        FloatingActionButton floatingActionButton = new FloatingActionButton(MainActivity.this);
        bottomNavigation.manageFloatingActionButtonBehavior(floatingActionButton);

        AHBottomNavigationItem item1 = new AHBottomNavigationItem(R.string.message, R.drawable.ic_baseline_chat_24, R.color.colorPrimaryDark);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(R.string.users, R.drawable.ic_baseline_person_search_24, R.color.colorPrimary);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem(R.string.story, R.drawable.ic_baseline_video_camera_back_24, R.color.purple_200);
        AHBottomNavigationItem item4 = new AHBottomNavigationItem(R.string.profile, R.drawable.ic_baseline_person_24, R.color.colorBottomNavigationPrimary);

        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);
        bottomNavigation.addItem(item4);

        bottomNavigation.setColored(true);
        bottomNavigation.setBehaviorTranslationEnabled(false);
        bottomNavigation.setAccentColor(Color.parseColor("#F63D2B"));
        bottomNavigation.setCurrentItem(0);
        setFragment(new ChatsFragment());
        bottomNavigation.setForceTint(true);
        bottomNavigation.setColored(true);
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.SHOW_WHEN_ACTIVE);
        if (unRead>0) {
            AHNotification notification = new AHNotification.Builder()
                    .setText(String.valueOf(unRead))
                    .setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorBottomNavigationNotification))
                    .setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white))
                    .build();
            bottomNavigation.setNotification(notification, 0);
        }
        bottomNavigation.setOnTabSelectedListener((position, wasSelected) -> {
            selectedTab = position;
            if (selectedTab == 0) {setFragment(new ChatsFragment());}
            if (selectedTab == 1) {setFragment(new UsersFragment());}
            if (selectedTab == 2) {setFragment(new StoryFragment());}
            if (selectedTab == 3) {setFragment(new ProfileFragment());}
            return true;
        });



        progressDialog.show();
        countDownTimer = new CountDownTimer(1500,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressDialog.setMessage("Loading...");
            }

            @Override
            public void onFinish() {
                progressDialog.dismiss();
            }
        }.start();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
                unRead = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    assert chat != null;
                    if (chat.getReceiver().equals(firebaseUser.getUid())) {
                        if (!chat.isSeen()) {
                            unRead++;
                        }
                    }
                }

                if (unRead != 0) {
                    viewPagerAdapter.addFragment(new ChatsFragment(), "(" + unRead + ")" + " Chats");
                } else {
                    viewPagerAdapter.addFragment(new ChatsFragment(), "Chats");
                }

                viewPagerAdapter.addFragment(new UsersFragment(), "Users");
                viewPagerAdapter.addFragment(new ProfileFragment(), "Profile");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_frame, fragment);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contextual_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return true;
        }
        return false;
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {

        final private ArrayList<Fragment> fragments;
        final private ArrayList<String> titles;

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }


        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fm, String title) {
            fragments.add(fm);
            titles.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        Log.d("MAIN_ACTIVITY", firebaseUser.getUid());
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
        status("offline");
    }
}