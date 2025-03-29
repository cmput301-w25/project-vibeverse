package com.example.vibeverse;

public class Achievement {
    private String id;
    private String name;
    private String description;
    private String unlocks;
    private String iconRes;

    private int total;

    private int tier; // bronze, silver, gold

    private int completion_xp;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnlocks() {
        return unlocks;
    }

    public void setUnlocks(String unlocks) {
        this.unlocks = unlocks;
    }

    public String getIconRes() {
        return iconRes;
    }

    public void setIconRes(String iconRes) {
        this.iconRes = iconRes;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public int getCompletion_xp() {
        return completion_xp;
    }

    public void setCompletion_xp(int completion_xp) {
        this.completion_xp = completion_xp;
    }
}
