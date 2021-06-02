package com.example.chatapp.Fragment;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
import com.google.firebase.database.Query;
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

    EditText search;

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
                Collections.sort(sortedUserList, (o1, o2) -> {
                    if (o1.getLastMessageDate() == null || o2.getLastMessageDate() == null)
                        return 0;
                    return o2.getLastMessageDate().compareTo(o1.getLastMessageDate());
                });
                readChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        search = view.findViewById(R.id.search_message);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                search(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

    private void search(String s) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("username").startAt(s).endAt(s+"\uf8ff");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (userList != null) {
                    userList.clear();
                }
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);

                    assert user != null;
                    assert fUser != null;
                    for (ChatsList chatsList : sortedUserList) {
                        if (!user.getId().equals((fUser.getUid())) && user.getId().equals(chatsList.getId()) ) {
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

    private void readChats() {
        userList = new ArrayList<>();

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
}