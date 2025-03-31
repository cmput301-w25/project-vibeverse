package com.example.vibeverse;

import java.util.List;

/**
 * Wrapper class for a list of Level objects.
 * <p>
 * This class is used to encapsulate a collection of Level objects, typically for JSON deserialization.
 * </p>
 */
public class LevelsWrapper {
    private List<Level> levels;

    /**
     * Returns the list of Level objects.
     *
     * @return the list of levels.
     */
    public List<Level> getLevels() {
        return levels;
    }

    /**
     * Sets the list of Level objects.
     *
     * @param levels the list of levels to set.
     */
    public void setLevels(List<Level> levels) {
        this.levels = levels;
    }
}
