package com.example.vibeverse;

import static org.junit.Assert.*;



import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class CommentTest {

    private Comment comment;
    private Date now;

    @Before
    public void setUp() {
        now = new Date();
        comment = new Comment(
                "comment123",
                "This is a test comment",
                "user456",
                now,
                "post789",
                "parent001"
        );
    }

    @Test
    public void testGetters() {
        assertEquals("comment123", comment.getCommentId());
        assertEquals("This is a test comment", comment.getContent());
        assertEquals("user456", comment.getAuthorUserId());
        assertEquals(now, comment.getTimestamp());
        assertEquals("post789", comment.getPostId());
        assertEquals("parent001", comment.getRepliesTo());
    }

    @Test
    public void testSetters() {
        Date later = new Date(now.getTime() + 10000);

        comment.setCommentId("c002");
        comment.setContent("Updated content");
        comment.setAuthorUserId("newUser");
        comment.setTimestamp(later);
        comment.setPostId("post002");
        comment.setRepliesTo("newParent");

        assertEquals("c002", comment.getCommentId());
        assertEquals("Updated content", comment.getContent());
        assertEquals("newUser", comment.getAuthorUserId());
        assertEquals(later, comment.getTimestamp());
        assertEquals("post002", comment.getPostId());
        assertEquals("newParent", comment.getRepliesTo());
    }
}