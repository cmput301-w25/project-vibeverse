package com.example.vibeverse;

import com.google.firebase.firestore.PropertyName;

import java.time.LocalDateTime;

public class Notification {
    public enum NotifType {
        FOLLOW_REQUEST,
        COMMENT_LIKED,
        POST_LIKED,
        POST_COMMENTED_ON
    }

    private NotifType notifType;
    private String content;
    private String dateTime;
    private String senderUserId;
    private String receiverUserId;

    @PropertyName("isRead")
    private boolean isRead;



    public Notification(String content, String dateTime, NotifType notifType, String senderUserId, String receiverUserId) {
        this.content = content;
        this.dateTime = dateTime;
        this.notifType = notifType;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.isRead = false;
    }

    // Empty constructor required for Firestore
    public Notification() {
    }

    public NotifType getNotifType() {
        return notifType;
    }

    @PropertyName("isRead")
    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setNotifType(NotifType notifType) {
        this.notifType = notifType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(String receiverUserId) {
        this.receiverUserId = receiverUserId;
    }


}
