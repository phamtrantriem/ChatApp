package com.example.chatapp.Fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.User;
import com.example.chatapp.R;
import com.example.chatapp.Service.Constants;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {


    CircleImageView profile_image;
    TextView username, txtEmail, txtName, txtPhone;
    ImageButton btn_logout;
    Button btn_upload_story;

    DatabaseReference dReference;
    StorageReference sReference, sStoryReference;

    Uri imageUri;
    Uri mediaUri;

    String name;

    StorageTask<UploadTask.TaskSnapshot> uploadImageTask, uploadMediaTask;

    FirebaseUser fUser;

    private ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int resultCode = result.getResultCode();
                Intent data = result.getData();
                if (resultCode == Constants.RESULT_OK && data != null && data.getData() != null) {
                    mediaUri = data.getData();
                    Log.d("MESSAGE_ACT_URI", mediaUri.toString());
                    if (uploadMediaTask != null && uploadMediaTask.isInProgress()) {
                        Toast.makeText(getContext(), "Upload in progress", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Upload successful", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @RequiresApi(api = Build.VERSION_CODES.O)
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
                    name = user.getUsername();
                    username.setText(user.getUsername());
                    txtName.setText(user.getName());
                    txtPhone.setText(user.getPhone());
                    txtEmail.setText(user.getEmail());
                    if (user.getImageURL().equals("default")) {
                        profile_image.setImageResource(R.drawable.ic_baseline_person_24);
                    } else {
                        Glide.with(requireContext()).load(user.getImageURL()).into(profile_image);
                    }
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

        btn_upload_story = view.findViewById(R.id.btn_upload_story);
        btn_upload_story.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(v.getContext(), android.R.style.Theme_NoTitleBar);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setContentView(R.layout.dialog_upload_story);

            EditText txtContent = dialog.findViewById(R.id.txtContentValue);
            String content = txtContent.getText().toString();
            VideoView videoView = dialog.findViewById(R.id.videoViewInput);

            ImageButton btn_gallery = dialog.findViewById(R.id.btn_upload_gallery);
            btn_gallery.setOnClickListener(v1 -> {
                openMedia();
                if (mediaUri != null) {
                    videoView.setVideoURI(mediaUri);
                }
            });

            ImageButton btn_upload = dialog.findViewById(R.id.btn_upload);
            btn_upload.setOnClickListener(v1 -> {
                if (mediaUri != null && !TextUtils.isEmpty(content)) {
                    uploadMedia(content);
                    dialog.dismiss();
                } else {
                    Toast.makeText(dialog.getContext(), "Cant not be null", Toast.LENGTH_SHORT).show();
                }
            });

            ImageButton dialog_back = dialog.findViewById(R.id.btn_dialog_back);
            dialog_back.setOnClickListener(v1 -> {
                dialog.dismiss();
            });
            dialog.show();
        });

        return view;
    }

    private void openMedia() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        someActivityResultLauncher.launch(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uploadMedia(String content) {
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage("Uploading...");
        dialog.show();
        if (mediaUri != null) {
            String extension = getFileExtension(mediaUri);
            Log.d("EXTENSION", extension);
            switch (extension) {
                case "mp4":
                    sStoryReference = FirebaseStorage.getInstance().getReference("videos/story");
                    break;
                case "jpg":
                case "png":
                    sStoryReference = FirebaseStorage.getInstance().getReference("images/story");
                    break;
                default:
                    Toast.makeText(getContext(), "Cant not upload this format file", Toast.LENGTH_SHORT).show();
            }

            String s = String.valueOf(System.currentTimeMillis());
            final StorageReference fileReference = sStoryReference.child(s + "." + getFileExtension(mediaUri));

            uploadMediaTask = fileReference.putFile(mediaUri);
            uploadMediaTask.continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }
                return fileReference.getDownloadUrl();
            }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    assert downloadUri != null;
                    String mUri = downloadUri.toString();
                    switch (extension) {
                        case "mp4":
                            sendMedia(name, content, mUri, "video");
                            break;
                        case "jpg":
                        case "png":
                            sendMedia(name, content, mUri, "image");
                            break;
                        default:
                            Toast.makeText(getContext(), "Cant not upload this format file", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Fails", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        } else {
            Toast.makeText(getContext(), "No image selected!!!", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public  void sendMedia(String name, String content,String mUri, String type) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("username", name);
        hashMap.put("content", content);

        LocalDateTime current = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");
        String formatted = current.format(formatter);

        hashMap.put("time", formatted);
        hashMap.put("url", mUri);
        hashMap.put("type", type);

        reference.child("Stories").push().setValue(hashMap);

    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, Constants.IMAGE_REQUEST);

    }

    private String getFileExtension(Uri imageUri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(imageUri));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Constants.RESULT_OK && data != null && data.getData() != null) {
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