package com.example.chatapp;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    EditText txtRegisUsername, txtRegisPassword, txtRegisEmail, txtReRegisPassword, txtRegisFullName, txtRegisPhone;
    Button btnRegister;
    CountryCodePicker ccp;

    FirebaseAuth fAuth;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtRegisEmail = findViewById(R.id.txtRegisEmail);
        txtRegisPassword = findViewById(R.id.txtRegisPassword);
        txtReRegisPassword = findViewById(R.id.txtReRegisPassword);
        txtRegisUsername = findViewById(R.id.txtRegisUsername);
        txtRegisFullName = findViewById(R.id.txtRegisFullName);
        txtRegisPhone = findViewById(R.id.txtRegisPhone);
        ccp = findViewById(R.id.countryCodePicker);


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
            String name = txtRegisFullName.getText().toString();
            String phone = txtRegisPhone.getText().toString();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(email) || TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                Toast.makeText(RegisterActivity.this, "All fields must not be empty!!!", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters!!!", Toast.LENGTH_SHORT).show();
            } else {
                String phoneNum = ccp.getSelectedCountryCodeWithPlus() + phone;
                register(username, password, email, name, phoneNum);
            }

        });

    }

    private void register(String username, String password, String email, String name, String phone) {
        fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = fAuth.getCurrentUser();
                assert firebaseUser != null;
                String userID = firebaseUser.getUid();

                reference = FirebaseDatabase.getInstance().getReference("Users").child(userID);

                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("id", userID);
                hashMap.put("username", username);
                hashMap.put("imageURL", "default");
                hashMap.put("name", name);
                hashMap.put("email", email);
                hashMap.put("phone", phone);
                hashMap.put("status", "offline");

                reference.setValue(hashMap).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(RegisterActivity.this, "Register successfully!!!", Toast.LENGTH_SHORT).show();
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