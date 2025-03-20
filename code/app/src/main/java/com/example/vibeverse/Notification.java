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

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    private String requestStatus;

    private String id;



    @PropertyName("isRead")
    private boolean isRead;



    public Notification(String id, String content, String dateTime, NotifType notifType, String senderUserId, String receiverUserId) {
        this.id = id;
        this.content = content;
        this.dateTime = dateTime;
        this.notifType = notifType;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.isRead = false;
        if (notifType != NotifType.FOLLOW_REQUEST) {
            this.requestStatus = null;
        }
        else{
            this.requestStatus = "pending";
        }
    }

    // Empty constructor required for Firestore
    public Notification() {
    }

    public NotifType getNotifType() {
        return notifType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
