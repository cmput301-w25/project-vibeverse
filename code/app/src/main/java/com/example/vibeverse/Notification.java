package com.example.vibeverse;

import com.google.firebase.firestore.PropertyName;

import java.time.LocalDateTime;

public class Notification {
    public enum NotifType {
        FOLLOW_REQUEST,
        COMMENT_LIKED,
        POST_LIKED,
        POST_COMMENTED_ON
        ,
        COMMENT_REPLIED_TO
    }

    private NotifType notifType;
    private String content;
    private String dateTime;
    private String senderUserId;
    private String receiverUserId;
    private String requestStatus; // pending, accepted, rejected
    private String moodEventId; // the postId of the mood event that the notification is about

    private String moodOwnerId; // the ownerId of the mood event that the notification is about

    public String getMoodOwnerId() {
        return moodOwnerId;
    }

    public void setMoodOwnerId(String moodOwnerId) {
        this.moodOwnerId = moodOwnerId;
    }

    @PropertyName("isRead")
    private boolean isRead;


    private String id;

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

    // This constructor is to be used for all notification types except follow requests
    public Notification(String id, String content, String dateTime, NotifType notifType, String senderUserId, String receiverUserId, String moodEventId, String moodOwnerId) {
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

        this.moodEventId = moodEventId;
        this.moodOwnerId = moodOwnerId;
    }

    public String getMoodEventId() {
        return moodEventId;
    }

    public void setMoodEventId(String moodEventId) {
        this.moodEventId = moodEventId;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
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
