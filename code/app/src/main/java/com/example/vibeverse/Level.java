package com.example.vibeverse;

/**
 * Represents a level in the application, containing the level number, the required XP to reach this level,
 * and information about what is unlocked upon reaching this level.
 */
public class Level {
    private int level;
    private int xpRequired;
    private String unlocks; // <-- new field

    /**
     * Returns the level number.
     *
     * @return the level number.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the level number.
     *
     * @param level the level number to set.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the XP required to reach this level.
     *
     * @return the required XP.
     */
    public int getXpRequired() {
        return xpRequired;
    }

    /**
     * Sets the XP required to reach this level.
     *
     * @param xpRequired the required XP to set.
     */
    public void setXpRequired(int xpRequired) {
        this.xpRequired = xpRequired;
    }

    /**
     * Returns the unlock information for this level.
     *
     * @return a String representing the unlocked feature or item, or "N/A" if none.
     */
    public String getUnlocks() {
        return unlocks;
    }

    /**
     * Sets the unlock information for this level.
     *
     * @param unlocks the unlock information to set.
     */
    public void setUnlocks(String unlocks) {
        this.unlocks = unlocks;
    }
}
