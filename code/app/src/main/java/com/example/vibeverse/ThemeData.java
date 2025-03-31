package com.example.vibeverse;

public class ThemeData {
    private String id;
    private String name;
    private String bundleTitle;
    private String unlockedBy;
    private String backgroundRes;

    /**
     * Returns the unique identifier of the theme.
     *
     * @return the theme ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the theme.
     *
     * @return the theme name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the bundle title of the theme.
     *
     * @return the bundle title.
     */
    public String getBundleTitle() {
        return bundleTitle;
    }

    /**
     * Returns the identifier of the entity that unlocked the theme.
     *
     * @return the unlockedBy value.
     */
    public String getUnlockedBy() {
        return unlockedBy;
    }

    /**
     * Returns the resource name for the theme's background image.
     *
     * @return the background resource name.
     */
    public String getBackgroundRes() {
        return backgroundRes;
    }
}
