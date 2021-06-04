package com.example.chatapp.Fragment;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.MainActivity;
import com.example.chatapp.Object.User;
import com.example.chatapp.R;
import com.example.chatapp.Service.Constaints;
import com.example.chatapp.StartActivity;
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

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {


    CircleImageView profile_image;
    TextView username, txtEmail, txtName, txtPhone;
    ImageButton btn_logout;

    DatabaseReference dReference;
    StorageReference sReference;

    private Uri imageUri;

    StorageTask<UploadTask.TaskSnapshot> uploadImageTask;

    FirebaseUser fUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profile_image = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);
        txtEmail = view.findViewById(R.id.txtDesEmailValue);
        txtName = view.findViewById(R.id.txtDesNameValue);
        txtPhone = view.findViewById(R.id.txtDesPhoneValue);

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        sReference = FirebaseStorage.getInstance().getReference("uploads");
        profile_image.setOnClickListener(v -> openImage());

        dReference = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());
        dReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    User user = snapshot.getValue(User.class);
                    assert user != null;
                    username.setText(user.getUsername());
                    txtName.setText(user.getName());
                    txtPhone.setText(user.getPhone());
                    txtEmail.setText(user.getEmail());
                    if (user.getImageURL().equals("default")) {
                        profile_image.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(requireContext()).load(user.getImageURL()).into(profile_image);
                    };
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        //logout
        btn_logout = view.findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getContext(), StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });

        return view;
    }
    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, Constaints.IMAGE_REQUEST);

    }

    private String getFileExtension(Uri imageUri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(imageUri));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Constaints.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (uploadImageTask != null && uploadImageTask.isInProgress()) {
                Toast.makeText(getContext(), "Upload in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }


    private void uploadImage() {
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage("Uploading...");
        dialog.show();

        if (imageUri != null) {
            String s = String.valueOf(System.currentTimeMillis());
            final StorageReference fileReference = sReference.child(s + "." + getFileExtension(imageUri));

            uploadImageTask = fileReference.putFile(imageUri);
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

                    dReference = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("imageURL", mUri);
                    dReference.updateChildren(hashMap);

                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Fails", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        } else {
            Toast.makeText(getContext(), "No image selected!!!", Toast.LENGTH_SHORT).show();
        }
    }
}