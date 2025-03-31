package com.example.vibeverse;

import com.google.firebase.firestore.PropertyName;
import java.time.LocalDateTime;

/**
 * Represents a notification sent to a user in the application.
 * <p>
 * Notifications can be of various types (e.g., follow request, comment liked, etc.) and include
 * information about the notification such as content, date and time, sender, receiver, and related mood event.
 * </p>
 */
public class Notification {
    /**
     * Enum representing the types of notifications.
     */
    public enum NotifType {
        FOLLOW_REQUEST,
        COMMENT_LIKED,
        POST_LIKED,
        POST_COMMENTED_ON,
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

    /**
     * Returns the mood owner ID associated with the notification.
     *
     * @return the mood owner ID.
     */
    public String getMoodOwnerId() {
        return moodOwnerId;
    }

    /**
     * Sets the mood owner ID associated with the notification.
     *
     * @param moodOwnerId the mood owner ID to set.
     */
    public void setMoodOwnerId(String moodOwnerId) {
        this.moodOwnerId = moodOwnerId;
    }

    @PropertyName("isRead")
    private boolean isRead;

    private String id;

    /**
     * Constructs a Notification for all types except follow requests.
     *
     * @param id            The notification ID.
     * @param content       The content/message of the notification.
     * @param dateTime      The date and time when the notification was created.
     * @param notifType     The type of the notification.
     * @param senderUserId  The user ID of the sender.
     * @param receiverUserId The user ID of the receiver.
     */
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
        } else {
            this.requestStatus = "pending";
        }
    }

    /**
     * Constructs a Notification for notification types that involve a mood event.
     *
     * @param id             The notification ID.
     * @param content        The content/message of the notification.
     * @param dateTime       The date and time when the notification was created.
     * @param notifType      The type of the notification.
     * @param senderUserId   The user ID of the sender.
     * @param receiverUserId The user ID of the receiver.
     * @param moodEventId    The ID of the mood event related to the notification.
     * @param moodOwnerId    The user ID of the owner of the mood event.
     */
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
        } else {
            this.requestStatus = "pending";
        }
        this.moodEventId = moodEventId;
        this.moodOwnerId = moodOwnerId;
    }

    /**
     * Empty constructor required for Firestore deserialization.
     */
    public Notification() {
    }

    /**
     * Returns the mood event ID associated with the notification.
     *
     * @return the mood event ID.
     */
    public String getMoodEventId() {
        return moodEventId;
    }

    /**
     * Sets the mood event ID associated with the notification.
     *
     * @param moodEventId the mood event ID to set.
     */
    public void setMoodEventId(String moodEventId) {
        this.moodEventId = moodEventId;
    }

    /**
     * Returns the request status of the notification.
     *
     * @return the request status (e.g., pending, accepted, rejected), or null if not applicable.
     */
    public String getRequestStatus() {
        return requestStatus;
    }

    /**
     * Sets the request status of the notification.
     *
     * @param requestStatus the request status to set.
     */
    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    /**
     * Returns the unique notification ID.
     *
     * @return the notification ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique notification ID.
     *
     * @param id the notification ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns whether the notification has been read.
     *
     * @return true if read, false otherwise.
     */
    @PropertyName("isRead")
    public boolean isRead() {
        return isRead;
    }

    /**
     * Sets the read status of the notification.
     *
     * @param read true to mark as read, false otherwise.
     */
    public void setRead(boolean read) {
        isRead = read;
    }

    /**
     * Returns the notification type.
     *
     * @return the notification type.
     */
    public NotifType getNotifType() {
        return notifType;
    }

    /**
     * Sets the notification type.
     *
     * @param notifType the notification type to set.
     */
    public void setNotifType(NotifType notifType) {
        this.notifType = notifType;
    }

    /**
     * Returns the content of the notification.
     *
     * @return the notification content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of the notification.
     *
     * @param content the notification content to set.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the date and time of the notification.
     *
     * @return the date and time as a String.
     */
    public String getDateTime() {
        return dateTime;
    }

    /**
     * Sets the date and time of the notification.
     *
     * @param dateTime the date and time to set.
     */
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Returns the sender's user ID.
     *
     * @return the sender's user ID.
     */
    public String getSenderUserId() {
        return senderUserId;
    }

    /**
     * Sets the sender's user ID.
     *
     * @param senderUserId the sender's user ID to set.
     */
    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    /**
     * Returns the receiver's user ID.
     *
     * @return the receiver's user ID.
     */
    public String getReceiverUserId() {
        return receiverUserId;
    }

    /**
     * Sets the receiver's user ID.
     *
     * @param receiverUserId the receiver's user ID to set.
     */
    public void setReceiverUserId(String receiverUserId) {
        this.receiverUserId = receiverUserId;
    }
}
