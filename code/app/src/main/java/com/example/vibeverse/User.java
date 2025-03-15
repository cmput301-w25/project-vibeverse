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

    // Empty constructor required for Firestore
    public User() {
    }

    // Constructor with essential fields
    public User(String username, String fullName, String email) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isHasProfilePic() {
        return hasProfilePic;
    }

    public void setHasProfilePic(boolean hasProfilePic) {
        this.hasProfilePic = hasProfilePic;
    }

    public long getProfilePicSizeKB() {
        return profilePicSizeKB;
    }

    public void setProfilePicSizeKB(long profilePicSizeKB) {
        this.profilePicSizeKB = profilePicSizeKB;
    }

    public String getProfilePicUri() {
        return profilePicUri;
    }

    public void setProfilePicUri(String profilePicUri) {
        this.profilePicUri = profilePicUri;
    }
}