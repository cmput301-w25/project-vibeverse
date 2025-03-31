package com.example.vibeverse;

import java.util.Date;

/**
 * Represents a comment made on a post.
 */
public class Comment {

    private String commentId;
    private String content;
    private String authorUserId;
    private Date timestamp;
    private String postId;


    private String parentCommentId; // the commentId of the comment it is replying to

    /**
     * Default constructor.
     */
    public Comment() {

    }

    /**
     * Constructs a Comment with the specified details.
     *
     * @param commentId       the unique identifier of the comment.
     * @param content         the content of the comment.
     * @param authorUserId    the user ID of the comment's author.
     * @param timestamp       the time when the comment was created.
     * @param postId          the ID of the post this comment belongs to.
     * @param parentCommentId the comment ID of the parent comment if this is a reply.
     */
    public Comment(String commentId, String content, String authorUserId, Date timestamp, String postId, String parentCommentId) {
        this.commentId = commentId;
        this.content = content;
        this.authorUserId = authorUserId;
        this.timestamp = timestamp;
        this.postId = postId;
        this.parentCommentId = parentCommentId;
    }

    /**
     * Returns the comment ID.
     *
     * @return the comment ID.
     */
    public String getCommentId() {
        return commentId;
    }

    /**
     * Sets the comment ID.
     *
     * @param commentId the comment ID to set.
     */
    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    /**
     * Returns the content of the comment.
     *
     * @return the comment content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of the comment.
     *
     * @param content the content to set.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the author user ID of the comment.
     *
     * @return the author's user ID.
     */
    public String getAuthorUserId() {
        return authorUserId;
    }

    /**
     * Sets the author user ID of the comment.
     *
     * @param authorUserId the user ID to set.
     */
    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    /**
     * Returns the timestamp of when the comment was created.
     *
     * @return the timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for the comment.
     *
     * @param timestamp the timestamp to set.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the post ID that this comment belongs to.
     *
     * @return the post ID.
     */
    public String getPostId() {
        return postId;
    }

    /**
     * Sets the post ID that this comment belongs to.
     *
     * @param postId the post ID to set.
     */
    public void setPostId(String postId) {
        this.postId = postId;
    }

    /**
     * Returns the ID of the comment this comment is replying to.
     *
     * @return the parent comment ID.
     */
    public String getRepliesTo() {
        return parentCommentId;
    }

    /**
     * Sets the ID of the comment this comment is replying to.
     *
     * @param repliesTo the parent comment ID to set.
     */
    public void setRepliesTo(String repliesTo) {
        this.parentCommentId = repliesTo;
    }


}
