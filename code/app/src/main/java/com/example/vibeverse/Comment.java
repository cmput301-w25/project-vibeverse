package com.example.vibeverse;

import java.util.Date;

public class Comment {

    private String commentId;
    private String content;
    private String authorUserId;
    private Date timestamp;
    private String postId;


    private String parentCommentId; // the commentId of the comment it is replying to

    public Comment() {

    }

    public Comment(String commentId, String content, String authorUserId, Date timestamp, String postId, String parentCommentId) {
        this.commentId = commentId;
        this.content = content;
        this.authorUserId = authorUserId;
        this.timestamp = timestamp;
        this.postId = postId;
        this.parentCommentId = parentCommentId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getRepliesTo() {
        return parentCommentId;
    }

    public void setRepliesTo(String repliesTo) {
        this.parentCommentId = repliesTo;
    }


}
