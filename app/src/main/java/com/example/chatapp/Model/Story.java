package com.example.chatapp.Model;

import java.util.Date;

public class Story {
    //private String id;
    private String username;
    private String content;
    private String time;
    private String url;
    private String type;

    public Story(String username, String content, String time, String url, String type) {
        this.username = username;
        this.content = content;
        this.time = time;
        this.url = url;
        this.type = type;
    }

    public Story() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Story{" +
                "username='" + username + '\'' +
                ", content='" + content + '\'' +
                ", time='" + time + '\'' +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
