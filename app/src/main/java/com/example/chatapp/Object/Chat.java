package com.example.chatapp.Object;

import androidx.annotation.NonNull;

public class Chat {
    private String sender;
    private String receiver;
    private String message;
    private boolean seen;
    private String timeSeen;
    private String timeSend;
    private String type;


    public Chat(String sender, String receiver, String message, boolean isSeen, String timeSeen, String timeSend, String type) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.seen = isSeen;
        this.timeSeen = timeSeen;
        this.timeSend = timeSend;
        this.type = type;
    }

    public Chat() {

    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getTimeSeen() {
        return timeSeen;
    }

    public void setTimeSeen(String timeSeen) {
        this.timeSeen = timeSeen;
    }

    public String getTimeSend() {
        return timeSend;
    }

    public void setTimeSend(String timeSend) {
        this.timeSend = timeSend;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", message='" + message + '\'' +
                ", isSeen=" + seen +
                ", timeSeen='" + timeSeen + '\'' +
                ", timeSend='" + timeSend + '\'' +
                '}';
    }
}
