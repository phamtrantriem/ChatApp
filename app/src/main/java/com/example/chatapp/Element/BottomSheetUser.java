package com.example.chatapp.Element;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.Object.User;
import com.example.chatapp.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.FirebaseCommonRegistrar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BottomSheetUser extends BottomSheetDialogFragment {

    private String userID;

    public BottomSheetUser(String userID) {
        this.userID = userID;
    }

    CardView card_view_call, card_view_view_info, card_view_back;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable  ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet, container, false);
        card_view_call = view.findViewById(R.id.card_view_call);
        card_view_view_info = view.findViewById(R.id.card_view_view_info);
        card_view_back = view.findViewById(R.id.card_view_back);

        card_view_call.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("userID", userID);
            startActivity(intent);
        });

        card_view_view_info.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar);
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
            DatabaseReference iReference = FirebaseDatabase.getInstance().getReference("Users").child(userID);
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
                        dialog_image.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(getContext()).load(user.getImageURL()).into(dialog_image);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            dialog_back.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });

        card_view_back.setOnClickListener(v -> {
            view.setVisibility(View.INVISIBLE);
        });
        return view;
    }
}
