package com.example.chatapp.Fragment;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chatapp.Adapter.UsersAdapter;
import com.example.chatapp.Object.Chat;
import com.example.chatapp.Object.ChatsList;
import com.example.chatapp.Object.User;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.DatabaseMetaData;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private UsersAdapter usersAdapter;
    private List<User> userList;

    FirebaseUser fUser;
    DatabaseReference reference;

    private  List<ChatsList> sortedUserList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chats, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        sortedUserList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("ChatsList").child(fUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (userList != null) {
                    userList.clear();
                }
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatsList chatsList = dataSnapshot.getValue(ChatsList.class);
                    sortedUserList.add(chatsList);
                }
                readChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return view;
    }

    private void readChats() {
        userList = new ArrayList<>();

        Collections.sort(sortedUserList, (o1, o2) -> {
            if (o1.getLastMessageDate() == null || o2.getLastMessageDate() == null)
                return 0;
            return o2.getLastMessageDate().compareTo(o1.getLastMessageDate());
        });

        Log.d("CHAT_FRAGMENT", sortedUserList.toString());


        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();

                for (ChatsList chatsList : sortedUserList) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        User user = dataSnapshot.getValue(User.class);
                        assert user != null;
                        if (user.getId().equals(chatsList.getId())) {
                            userList.add(user);
                        }
                    }
                }

                usersAdapter = new UsersAdapter(getContext(), userList, true);
                recyclerView.setAdapter(usersAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

//    private void readChats1() {
//        userList = new ArrayList<>();
//
//        reference = FirebaseDatabase.getInstance().getReference("Users");
//        reference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                userList.clear();
//                ArrayList<User> listTemp;
//                //get user from db
//                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
//                    User user = dataSnapshot.getValue(User.class);
//                    assert user != null;
                    //get userID in relationship from usernamelist
//                    for (String userID : usernameList) {
                        // compare if user in db equals user in usernamelist
//                        if (user.getId().equals(userID)) {
                            //if list null
//                            if (userList.size() != 0) {
//                                //check if user existed in userList to avoid looping user
//                                for (User userInList : userList) {
//                                    if (!user.getId().equals(userInList.getId())) {
//                                        userList.add(user);
//                                    }
//                                }
//                            } else {
//                                // userlist null, add user to list
//                                userList.add(user);
//                            }
//                            userList.add(user);
//                        }
//                    }
//                }

//                usersAdapter = new UsersAdapter(getContext(), userList, true);
//                recyclerView.setAdapter(usersAdapter);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
//    }
}