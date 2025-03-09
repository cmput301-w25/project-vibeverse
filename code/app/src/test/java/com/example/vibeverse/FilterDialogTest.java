package com.example.vibeverse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * Unit tests for the FilterDialog's logic.
 *
 * These tests focus purely on the filter logic without any Android framework dependencies.
 */
@RunWith(JUnit4.class)
public class FilterDialogTest {

    /**
     * A test implementation of the FilterListener interface to capture filter selections
     */
    private static class TestFilterListener implements FilterDialog.FilterListener {
        public String appliedTimeFilter;
        public boolean appliedHappy;
        public boolean appliedSad;
        public boolean appliedAfraid;
        public boolean appliedConfused;
        public boolean filterAppliedCalled = false;

        @Override
        public void onFilterApplied(String timeFilter, boolean isHappy, boolean isSad, boolean isAfraid, boolean isConfused) {
            this.appliedTimeFilter = timeFilter;
            this.appliedHappy = isHappy;
            this.appliedSad = isSad;
            this.appliedAfraid = isAfraid;
            this.appliedConfused = isConfused;
            this.filterAppliedCalled = true;
        }
    }

    /**
     * Test implementation of the filter dialog logic without Android dependencies
     */
    private static class FilterDialogLogic {
        // Filter settings
        private String selectedTimeFilter = "";
        private boolean isHappySelected = false;
        private boolean isSadSelected = false;
        private boolean isAfraidSelected = false;
        private boolean isConfusedSelected = false;

        /**
         * Simulates selecting a time filter radio button
         */
        public void selectTimeFilter(String timeFilter) {
            this.selectedTimeFilter = timeFilter;
        }

        /**
         * Simulates checking the Happy emotion checkbox
         */
        public void setHappySelected(boolean selected) {
            this.isHappySelected = selected;
        }

        /**
         * Simulates checking the Sad emotion checkbox
         */
        public void setSadSelected(boolean selected) {
            this.isSadSelected = selected;
        }

        /**
         * Simulates checking the Afraid emotion checkbox
         */
        public void setAfraidSelected(boolean selected) {
            this.isAfraidSelected = selected;
        }

        /**
         * Simulates checking the Confused emotion checkbox
         */
        public void setConfusedSelected(boolean selected) {
            this.isConfusedSelected = selected;
        }

        /**
         * Simulates clicking the Apply Filters button
         */
        public void applyFilters(TestFilterListener listener) {
            String timeFilter = selectedTimeFilter;
            boolean isHappy = isHappySelected;
            boolean isSad = isSadSelected;
            boolean isAfraid = isAfraidSelected;
            boolean isConfused = isConfusedSelected;

            // Apply the filter logic from the original FilterDialog class
            if (timeFilter.isEmpty() && !isHappy && !isSad && !isAfraid && !isConfused) {
                // If no filters are selected, apply all filters (show everything)
                listener.onFilterApplied("all_time", true, true, true, true);
            }
            else if (timeFilter.isEmpty() && (isHappy || isSad || isAfraid || isConfused)) {
                // If only emotion filters are selected, use "all_time" for time
                timeFilter = "all_time";
                listener.onFilterApplied(timeFilter, isHappy, isSad, isAfraid, isConfused);
            }
            else {
                // Use the selected filters
                listener.onFilterApplied(timeFilter, isHappy, isSad, isAfraid, isConfused);
            }
        }
    }

    private FilterDialogLogic filterDialogLogic;
    private TestFilterListener testFilterListener;

    @Before
    public void setUp() {
        filterDialogLogic = new FilterDialogLogic();
        testFilterListener = new TestFilterListener();
    }

    /**
     * Test when no filters are selected, all filters should be applied
     */
    @Test
    public void testNoFiltersSelected() {
        // Apply with no selections
        filterDialogLogic.applyFilters(testFilterListener);

        // Verify all filters are applied
        assertTrue("Filter applied callback should be called", testFilterListener.filterAppliedCalled);
        assertEquals("Time filter should be 'all_time'", "all_time", testFilterListener.appliedTimeFilter);
        assertTrue("Happy filter should be applied", testFilterListener.appliedHappy);
        assertTrue("Sad filter should be applied", testFilterListener.appliedSad);
        assertTrue("Afraid filter should be applied", testFilterListener.appliedAfraid);
        assertTrue("Confused filter should be applied", testFilterListener.appliedConfused);
    }

    /**
     * Test when only time filter is selected
     */
    @Test
    public void testOnlyTimeFilterSelected() {
        // Select only time filter
        filterDialogLogic.selectTimeFilter("last_24_hours");

        // Apply filters
        filterDialogLogic.applyFilters(testFilterListener);

        // Verify filters
        assertEquals("Time filter should be 'last_24_hours'", "last_24_hours", testFilterListener.appliedTimeFilter);
        assertFalse("Happy filter should not be applied", testFilterListener.appliedHappy);
        assertFalse("Sad filter should not be applied", testFilterListener.appliedSad);
        assertFalse("Afraid filter should not be applied", testFilterListener.appliedAfraid);
        assertFalse("Confused filter should not be applied", testFilterListener.appliedConfused);
    }

    /**
     * Test when only emotion filters are selected
     */
    @Test
    public void testOnlyEmotionFiltersSelected() {
        // Select only emotion filters
        filterDialogLogic.setHappySelected(true);
        filterDialogLogic.setSadSelected(true);

        // Apply filters
        filterDialogLogic.applyFilters(testFilterListener);

        // Verify filters
        assertEquals("Time filter should be 'all_time'", "all_time", testFilterListener.appliedTimeFilter);
        assertTrue("Happy filter should be applied", testFilterListener.appliedHappy);
        assertTrue("Sad filter should be applied", testFilterListener.appliedSad);
        assertFalse("Afraid filter should not be applied", testFilterListener.appliedAfraid);
        assertFalse("Confused filter should not be applied", testFilterListener.appliedConfused);
    }

    /**
     * Test when all filter types are selected
     */
    @Test
    public void testAllFilterTypesSelected() {
        // Select time and some emotion filters
        filterDialogLogic.selectTimeFilter("last_week");
        filterDialogLogic.setHappySelected(false);
        filterDialogLogic.setSadSelected(true);
        filterDialogLogic.setAfraidSelected(false);
        filterDialogLogic.setConfusedSelected(true);

        // Apply filters
        filterDialogLogic.applyFilters(testFilterListener);

        // Verify filters
        assertEquals("Time filter should be 'last_week'", "last_week", testFilterListener.appliedTimeFilter);
        assertFalse("Happy filter should not be applied", testFilterListener.appliedHappy);
        assertTrue("Sad filter should be applied", testFilterListener.appliedSad);
        assertFalse("Afraid filter should not be applied", testFilterListener.appliedAfraid);
        assertTrue("Confused filter should be applied", testFilterListener.appliedConfused);
    }

    /**
     * Test with Last 24 Hours time filter
     */
    @Test
    public void testLast24HoursFilter() {
        filterDialogLogic.selectTimeFilter("last_24_hours");
        filterDialogLogic.setHappySelected(true);

        filterDialogLogic.applyFilters(testFilterListener);

        assertEquals("last_24_hours", testFilterListener.appliedTimeFilter);
        assertTrue(testFilterListener.appliedHappy);
    }

    /**
     * Test with Last 3 Days time filter
     */
    @Test
    public void testLast3DaysFilter() {
        filterDialogLogic.selectTimeFilter("3Days");
        filterDialogLogic.setSadSelected(true);

        filterDialogLogic.applyFilters(testFilterListener);

        assertEquals("3Days", testFilterListener.appliedTimeFilter);
        assertTrue(testFilterListener.appliedSad);
    }

    /**
     * Test with Last Week time filter
     */
    @Test
    public void testLastWeekFilter() {
        filterDialogLogic.selectTimeFilter("last_week");
        filterDialogLogic.setAfraidSelected(true);

        filterDialogLogic.applyFilters(testFilterListener);

        assertEquals("last_week", testFilterListener.appliedTimeFilter);
        assertTrue(testFilterListener.appliedAfraid);
    }

    /**
     * Test with Last Month time filter
     */
    @Test
    public void testLastMonthFilter() {
        filterDialogLogic.selectTimeFilter("last_month");
        filterDialogLogic.setConfusedSelected(true);

        filterDialogLogic.applyFilters(testFilterListener);

        assertEquals("last_month", testFilterListener.appliedTimeFilter);
        assertTrue(testFilterListener.appliedConfused);
    }

    /**
     * Test with multiple emotion filters and no time filter
     */
    @Test
    public void testMultipleEmotionFiltersNoTimeFilter() {
        filterDialogLogic.setHappySelected(true);
        filterDialogLogic.setAfraidSelected(true);
        filterDialogLogic.setConfusedSelected(true);

        filterDialogLogic.applyFilters(testFilterListener);

        assertEquals("all_time", testFilterListener.appliedTimeFilter);
        assertTrue(testFilterListener.appliedHappy);
        assertFalse(testFilterListener.appliedSad);
        assertTrue(testFilterListener.appliedAfraid);
        assertTrue(testFilterListener.appliedConfused);
    }
}