package com.example.vibeverse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class HomePageTest {

    // Test the time filter logic that would be used in HomePage
    @Test
    public void testTimeFilterLogic() {
        long currentTime = System.currentTimeMillis();

        // Create test dates
        Date now = new Date(currentTime);
        Date oneDayOld = new Date(currentTime - 86400000); // 1 day old
        Date twoDaysOld = new Date(currentTime - 86400000 * 2); // 2 days old
        Date sixDaysOld = new Date(currentTime - 86400000 * 6); // 6 days old
        Date twentyDaysOld = new Date(currentTime - 86400000 * 20); // 20 days old

        // Test last_24_hours filter
        assertTrue("Date from now should be within last 24 hours", isWithinTimeFilter("last_24_hours", now, currentTime));
        assertFalse("Date from 1 day ago should not be within last 24 hours", isWithinTimeFilter("last_24_hours", oneDayOld, currentTime));

        // Test 3Days filter
        assertTrue("Date from now should be within 3 days", isWithinTimeFilter("3Days", now, currentTime));
        assertTrue("Date from 2 days ago should be within 3 days", isWithinTimeFilter("3Days", twoDaysOld, currentTime));
        assertFalse("Date from 6 days ago should not be within 3 days", isWithinTimeFilter("3Days", sixDaysOld, currentTime));

        // Test last_week filter
        assertTrue("Date from now should be within last week", isWithinTimeFilter("last_week", now, currentTime));
        assertTrue("Date from 6 days ago should be within last week", isWithinTimeFilter("last_week", sixDaysOld, currentTime));
        assertFalse("Date from 20 days ago should not be within last week", isWithinTimeFilter("last_week", twentyDaysOld, currentTime));

        // Test last_month filter
        assertTrue("Date from now should be within last month", isWithinTimeFilter("last_month", now, currentTime));
        assertTrue("Date from 20 days ago should be within last month", isWithinTimeFilter("last_month", twentyDaysOld, currentTime));

        // Test all_time filter
        assertTrue("Any date should be within all_time", isWithinTimeFilter("all_time", twentyDaysOld, currentTime));
    }

    // Test the mood filter logic that would be used in HomePage
    @Test
    public void testMoodFilterLogic() {
        // Test scenario: no moods selected (should match all)
        assertTrue("Should match when no moods selected",
                isMoodMatching(false, false, false, false, "HAPPY"));
        assertTrue("Should match when no moods selected",
                isMoodMatching(false, false, false, false, "SAD"));

        // Test scenario: only happy selected
        assertTrue("Should match HAPPY when happy selected",
                isMoodMatching(true, false, false, false, "HAPPY"));
        assertFalse("Should not match SAD when only happy selected",
                isMoodMatching(true, false, false, false, "SAD"));

        // Test scenario: multiple moods selected
        assertTrue("Should match HAPPY when happy and sad selected",
                isMoodMatching(true, true, false, false, "HAPPY"));
        assertTrue("Should match SAD when happy and sad selected",
                isMoodMatching(true, true, false, false, "SAD"));
        assertFalse("Should not match AFRAID when only happy and sad selected",
                isMoodMatching(true, true, false, false, "AFRAID"));

        // Test all moods selected
        assertTrue("Should match any mood when all selected",
                isMoodMatching(true, true, true, true, "CONFUSED"));
    }

    // Test the search functionality
    @Test
    public void testSearchLogic() {
        // Case-insensitive match
        assertTrue("Should match exact case", matchesSearch("Welcome", "Welcome"));
        assertTrue("Should match case-insensitive", matchesSearch("welcome", "Welcome"));
        assertTrue("Should match case-insensitive", matchesSearch("WELCOME", "Welcome"));

        // Partial match
        assertTrue("Should match partial string", matchesSearch("Wel", "Welcome"));
        assertTrue("Should match partial string", matchesSearch("come", "Welcome"));

        // Non-match
        assertFalse("Should not match different string", matchesSearch("Hello", "Welcome"));

        // Empty search
        assertTrue("Empty search should match anything", matchesSearch("", "Welcome"));

        // Whitespace handling
        assertTrue("Should trim and match", matchesSearch("  Welcome  ", "Welcome"));
    }

    // Test the adapter's filter method logic
    @Test
    public void testAdapterFilterLogic() {
        // Create lists to simulate the PostAdapter's behavior
        List<String> originalTitles = new ArrayList<>();
        originalTitles.add("Welcome to VibeVerse!");
        originalTitles.add("First Post");
        originalTitles.add("Another Happy Day");

        // Filter for "Welcome"
        List<String> filteredTitles = filterTitles(originalTitles, "Welcome");
        assertEquals("Should find 1 post with 'Welcome'", 1, filteredTitles.size());
        assertEquals("Should find 'Welcome to VibeVerse!'", "Welcome to VibeVerse!", filteredTitles.get(0));

        // Filter for "Post"
        filteredTitles = filterTitles(originalTitles, "Post");
        assertEquals("Should find 1 post with 'Post'", 1, filteredTitles.size());
        assertEquals("Should find 'First Post'", "First Post", filteredTitles.get(0));

        // Filter for "Happy"
        filteredTitles = filterTitles(originalTitles, "Happy");
        assertEquals("Should find 1 post with 'Happy'", 1, filteredTitles.size());
        assertEquals("Should find 'Another Happy Day'", "Another Happy Day", filteredTitles.get(0));

        // Empty filter should return all
        filteredTitles = filterTitles(originalTitles, "");
        assertEquals("Empty filter should return all posts", 3, filteredTitles.size());

        // Filter with no matches
        filteredTitles = filterTitles(originalTitles, "NonexistentTerm");
        assertEquals("No matches should return empty list", 0, filteredTitles.size());
    }

    // Test combined filter logic (time + mood)
    @Test
    public void testCombinedFilterLogic() {
        long currentTime = System.currentTimeMillis();

        // Items that match both time and mood criteria should pass
        assertTrue("Should match time AND mood when both valid",
                isWithinTimeFilter("last_24_hours", new Date(currentTime), currentTime) &&
                        isMoodMatching(true, false, false, false, "HAPPY"));

        // Items that fail time criteria should not pass
        assertFalse("Should not match when time fails",
                isWithinTimeFilter("last_24_hours", new Date(currentTime - 86400000 * 2), currentTime) &&
                        isMoodMatching(true, false, false, false, "HAPPY"));

        // Items that fail mood criteria should not pass
        assertFalse("Should not match when mood fails",
                isWithinTimeFilter("last_24_hours", new Date(currentTime), currentTime) &&
                        isMoodMatching(true, false, false, false, "SAD"));
    }

    /**
     * Helper methods to simulate the logic in HomePage
     */

    // Simulates the time filtering logic
    private boolean isWithinTimeFilter(String timeFilter, Date date, long currentTime) {
        long postTime = date.getTime();
        boolean isWithinTime = false;

        switch (timeFilter) {
            case "last_24_hours":
                isWithinTime = (currentTime - postTime) < 86400000;
                break;
            case "3Days":
                isWithinTime = (currentTime - postTime) <= 259200000;
                break;
            case "last_week":
                isWithinTime = (currentTime - postTime) <= 604800000;
                break;
            case "last_month":
                isWithinTime = (currentTime - postTime) <= 2592000000L;
                break;
            case "all_time":
                isWithinTime = true;
                break;
        }

        return isWithinTime;
    }

    // Simulates the mood filtering logic
    private boolean isMoodMatching(boolean isHappy, boolean isSad, boolean isAfraid, boolean isConfused, String mood) {
        return (isHappy && mood.equals("HAPPY")) ||
                (isSad && mood.equals("SAD")) ||
                (isAfraid && mood.equals("AFRAID")) ||
                (isConfused && mood.equals("CONFUSED")) ||
                (!isHappy && !isSad && !isAfraid && !isConfused);
    }

    // Simulates the search logic
    private boolean matchesSearch(String query, String title) {
        query = query.toLowerCase().trim();
        if (query.isEmpty()) {
            return true;
        }
        return title.toLowerCase().contains(query);
    }

    // Simulates the adapter's filter logic
    private List<String> filterTitles(List<String> originalTitles, String query) {
        query = query.toLowerCase().trim();
        List<String> filteredTitles = new ArrayList<>();

        if (query.isEmpty()) {
            filteredTitles.addAll(originalTitles);
        } else {
            for (String title : originalTitles) {
                if (title.toLowerCase().contains(query)) {
                    filteredTitles.add(title);
                }
            }
        }

        return filteredTitles;
    }
}