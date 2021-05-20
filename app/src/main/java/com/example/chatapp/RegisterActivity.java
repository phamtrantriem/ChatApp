package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatapp.Object.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    EditText txtRegisUsername, txtRegisPassword, txtRegisEmail;
    Button btnRegister;

    FirebaseAuth fAuth;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtRegisUsername = findViewById(R.id.txtRegisUsername);
        txtRegisPassword = findViewById(R.id.txtRegisPassword);
        txtRegisEmail = findViewById(R.id.txtRegisEmail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Register");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fAuth = FirebaseAuth.getInstance();
        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> {
            String username = txtRegisUsername.getText().toString();
            String password = txtRegisPassword.getText().toString();
            String email = txtRegisEmail.getText().toString();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(email)) {
                Toast.makeText(RegisterActivity.this, "All fields must not be empty!!!", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters!!!", Toast.LENGTH_SHORT).show();
            } else {
                register(username, password, email);
            }
        });

    }

    private void register(String username, String password, String email) {
        fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = fAuth.getCurrentUser();
                assert firebaseUser != null;
                String userID = firebaseUser.getUid();

                reference = FirebaseDatabase.getInstance().getReference("Users").child(userID);

//               Map<String, String> hashMap = new HashMap<>();
//               hashMap.put("id", userID);
//               hashMap.put("username", username);
//               hashMap.put("imageURL", "default");

                reference.setValue(new User(userID, username, "default")).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(RegisterActivity.this, "added db", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } else {
                //Toast.makeText(RegisterActivity.this, "Register failed!!", Toast.LENGTH_SHORT).show();
                Log.w("TAG", "createUserWithEmail:failure", task.getException());
                Toast.makeText(RegisterActivity.this, "Authentication failed.",
                        Toast.LENGTH_SHORT).show();

            }
        });
    }
}