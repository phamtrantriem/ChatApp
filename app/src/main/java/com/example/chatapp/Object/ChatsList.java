package com.example.chatapp.Object;

import java.util.Date;

public class ChatsList {
    private String id;
    private String lastMessageDate;

    public ChatsList(String id, String lastMessageDate) {
        this.id = id;
        this.lastMessageDate = lastMessageDate;
    }

    public ChatsList() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(String lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    @Override
    public String toString() {
        return "ChatsList{" +
                "id='" + id + '\'' +
                ", lastMessageDate=" + lastMessageDate +
                '}';
    }
}
