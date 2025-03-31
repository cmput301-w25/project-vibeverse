package com.example.vibeverse;

public class User {
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private String bio;
    private String dateOfBirth;
    private String gender;
    private boolean hasProfilePic;
    private long profilePicSizeKB;
    private String profilePicUri;

    private int followerCount;
    private int followingCount;

    /**
     * Returns the number of followers the user has.
     *
     * @return the follower count.
     */
    public int getFollowerCount() {
        return followerCount;
    }

    /**
     * Sets the number of followers for the user.
     *
     * @param followerCount the new follower count.
     */
    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * Returns the number of users the user is following.
     *
     * @return the following count.
     */
    public int getFollowingCount() {
        return followingCount;
    }

    /**
     * Sets the number of users the user is following.
     *
     * @param followingCount the new following count.
     */
    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    /**
     * Empty constructor required for Firestore deserialization.
     */
    public User() {
    }

    /**
     * Constructs a new User with essential fields.
     *
     * @param username the user's username.
     * @param fullName the user's full name.
     * @param email    the user's email address.
     */
    public User(String username, String fullName, String email) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return the userId.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user's unique identifier.
     *
     * @param userId the new userId.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Returns the user's username.
     *
     * @return the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's username.
     *
     * @param username the new username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the user's full name.
     *
     * @return the fullName.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the user's full name.
     *
     * @param fullName the new full name.
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the user's email address.
     *
     * @return the email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email the new email.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the user's bio.
     *
     * @return the bio.
     */
    public String getBio() {
        return bio;
    }

    /**
     * Sets the user's bio.
     *
     * @param bio the new bio.
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Returns the user's date of birth.
     *
     * @return the dateOfBirth.
     */
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the user's date of birth.
     *
     * @param dateOfBirth the new date of birth.
     */
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Returns the user's gender.
     *
     * @return the gender.
     */
    public String getGender() {
        return gender;
    }

    /**
     * Sets the user's gender.
     *
     * @param gender the new gender.
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Checks if the user has a profile picture.
     *
     * @return true if the user has a profile picture, false otherwise.
     */
    public boolean isHasProfilePic() {
        return hasProfilePic;
    }

    /**
     * Sets whether the user has a profile picture.
     *
     * @param hasProfilePic true if the user has a profile picture, false otherwise.
     */
    public void setHasProfilePic(boolean hasProfilePic) {
        this.hasProfilePic = hasProfilePic;
    }

    /**
     * Returns the size of the profile picture in kilobytes.
     *
     * @return the profilePicSizeKB.
     */
    public long getProfilePicSizeKB() {
        return profilePicSizeKB;
    }

    /**
     * Sets the size of the profile picture in kilobytes.
     *
     * @param profilePicSizeKB the new profile picture size.
     */
    public void setProfilePicSizeKB(long profilePicSizeKB) {
        this.profilePicSizeKB = profilePicSizeKB;
    }

    /**
     * Returns the URI of the user's profile picture.
     *
     * @return the profilePicUri.
     */
    public String getProfilePicUri() {
        return profilePicUri;
    }

    /**
     * Sets the URI of the user's profile picture.
     *
     * @param profilePicUri the new profile picture URI.
     */
    public void setProfilePicUri(String profilePicUri) {
        this.profilePicUri = profilePicUri;
    }
}