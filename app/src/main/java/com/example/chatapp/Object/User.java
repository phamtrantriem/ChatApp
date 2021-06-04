package com.example.chatapp.Object;

import androidx.annotation.NonNull;

public class User {
    private String id;
    private String username;
    private String imageURL;
    private String name;
    private String email;
    private String phone;
    private String status;
    private String typing;
    private String token;

    public User(String id, String username, String imageURL, String name, String email, String phone, String status, String typing) {
        this.id = id;
        this.username = username;
        this.imageURL = imageURL;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.typing = typing;

    }

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTyping() {
        return typing;
    }

    public void setTyping(String typing) {
        this.typing = typing;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", imageURL='" + imageURL + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", status='" + status + '\'' +
                ", typing='" + typing + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
