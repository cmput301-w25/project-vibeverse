package com.example.vibeverse;

import java.util.List;

/**
 * A wrapper class for a list of Achievement objects.
 */
public class AchievementsWrapper {
    private List<Achievement> achievements;

    /**
     * Returns the list of Achievement objects.
     *
     * @return the list of achievements.
     */
    public List<Achievement> getAchievements() {
        return achievements;
    }

    /**
     * Sets the list of Achievement objects.
     *
     * @param achievements the list of achievements to set.
     */
    public void setAchievements(List<Achievement> achievements) {
        this.achievements = achievements;
    }
}
