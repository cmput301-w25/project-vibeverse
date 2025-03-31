package com.example.vibeverse;

/**
 * Represents an achievement with its details such as id, name, description, unlocks, icon resource,
 * total count, tier, and the XP awarded upon completion.
 */
public class Achievement {
    private String id;
    private String name;
    private String description;
    private String unlocks;
    private String iconRes;

    private int total;

    private int tier; // bronze, silver, gold

    private int completion_xp;

    /**
     * Returns the name of the achievement.
     *
     * @return the name of the achievement.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the achievement.
     *
     * @param name the name to set for the achievement.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the unique identifier of the achievement.
     *
     * @return the id of the achievement.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the achievement.
     *
     * @param id the id to set for the achievement.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the description of the achievement.
     *
     * @return the description of the achievement.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the achievement.
     *
     * @param description the description to set for the achievement.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the unlock information associated with the achievement.
     *
     * @return the unlock information of the achievement.
     */
    public String getUnlocks() {
        return unlocks;
    }

    /**
     * Sets the unlock information for the achievement.
     *
     * @param unlocks the unlock information to set.
     */
    public void setUnlocks(String unlocks) {
        this.unlocks = unlocks;
    }

    /**
     * Returns the resource identifier for the achievement's icon.
     *
     * @return the icon resource identifier.
     */
    public String getIconRes() {
        return iconRes;
    }

    /**
     * Sets the resource identifier for the achievement's icon.
     *
     * @param iconRes the icon resource identifier to set.
     */
    public void setIconRes(String iconRes) {
        this.iconRes = iconRes;
    }

    /**
     * Returns the total count associated with the achievement.
     *
     * @return the total count.
     */
    public int getTotal() {
        return total;
    }

    /**
     * Sets the total count for the achievement.
     *
     * @param total the total count to set.
     */
    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * Returns the tier of the achievement (e.g., bronze, silver, gold).
     *
     * @return the tier of the achievement.
     */
    public int getTier() {
        return tier;
    }

    /**
     * Sets the tier of the achievement.
     *
     * @param tier the tier to set (e.g., bronze, silver, gold).
     */
    public void setTier(int tier) {
        this.tier = tier;
    }

    /**
     * Returns the completion XP associated with the achievement.
     *
     * @return the XP awarded upon completion.
     */
    public int getCompletion_xp() {
        return completion_xp;
    }

    /**
     * Sets the completion XP for the achievement.
     *
     * @param completion_xp the XP to set for the achievement's completion.
     */
    public void setCompletion_xp(int completion_xp) {
        this.completion_xp = completion_xp;
    }
}
