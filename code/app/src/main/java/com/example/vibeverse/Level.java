package com.example.vibeverse;

public class Level {
    private int level;
    private int xpRequired;
    private String unlocks; // <-- new field

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXpRequired() {
        return xpRequired;
    }

    public void setXpRequired(int xpRequired) {
        this.xpRequired = xpRequired;
    }

    public String getUnlocks() {
        return unlocks;
    }

    public void setUnlocks(String unlocks) {
        this.unlocks = unlocks;
    }
}
