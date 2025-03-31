package com.example.vibeverse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class NotificationTest {

    private Notification followNotif;
    private Notification postNotif;

    @Before
    public void setUp() {
        followNotif = new Notification(
                "notif123",
                "UserA sent you a follow request",
                "2024-03-29T10:00:00",
                Notification.NotifType.FOLLOW_REQUEST,
                "userA",
                "userB"
        );

        postNotif = new Notification(
                "notif456",
                "UserA liked your post",
                "2024-12-12T10:00:00",
                Notification.NotifType.POST_LIKED,
                "UserA",
                "UserB",
                "post123",
                "UserB" // moodOwnerId
        );


    }

    @Test
    public void testFollowRequestDefaults() {
        assertEquals("pending", followNotif.getRequestStatus());
        assertFalse(followNotif.isRead());
        assertNull(followNotif.getMoodEventId());
    }

    @Test
    public void testPostNotificationFields() {
        assertEquals("post123", postNotif.getMoodEventId());
        assertEquals("UserA", postNotif.getSenderUserId());
        assertEquals("UserB", postNotif.getReceiverUserId());
        assertEquals(Notification.NotifType.POST_LIKED, postNotif.getNotifType());
        assertNull(postNotif.getRequestStatus()); // not a follow request
        assertFalse(postNotif.isRead());
    }

    @Test
    public void testMarkAsRead() {
        followNotif.setRead(true);
        assertTrue(followNotif.isRead());
    }

    @Test
    public void testUpdateRequestStatus() {
        followNotif.setRequestStatus("accepted");
        assertEquals("accepted", followNotif.getRequestStatus());
    }
}