package com.example.vibeverse;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

public class PhotographTest {

    @Test
    public void testGetFormattedDetails_KBFormat() throws Exception {
        // Prepare a test date using a fixed format.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        Date testDate = sdf.parse("20230310_101010");

        // Create a mockPhotograph with a file size of 500 KB (should be displayed in KB)
        mockPhotograph photo = new mockPhotograph("http://example.com/image.jpg", 500, testDate, "TestLocation");

        String details = photo.getFormattedDetails();

        // Verify that the formatted string contains the expected parts.
        assertTrue("Details should include the file size in KB", details.contains("500 KB"));
        assertTrue("Details should include the location", details.contains("TestLocation"));
        // We assume the date formatting produces a month abbreviation like "Mar"
        assertTrue("Details should include the date", details.contains("Mar"));
    }

    @Test
    public void testGetFormattedDetails_MBFormat() throws Exception {
        // Prepare a test date using a fixed format.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        Date testDate = sdf.parse("20230310_101010");

        // Create a mockPhotograph with a file size of 1500 KB (should be displayed in MB)
        mockPhotograph photo = new mockPhotograph("http://example.com/image.jpg", 1500, testDate, "TestLocation");

        String details = photo.getFormattedDetails();

        // The file size should be converted to MB (1500/1024 ≈ 1.46 MB)
        assertTrue("Details should include MB size", details.contains("MB"));
        assertTrue("Details should include the location", details.contains("TestLocation"));
        assertTrue("Details should include the date", details.contains("Mar"));
    }
}
