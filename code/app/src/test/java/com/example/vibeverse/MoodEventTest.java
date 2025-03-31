package com.example.vibeverse;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class MoodEventTest {

    private MoodEvent moodEvent;

    @Before
    public void setUp() {
        
        moodEvent = new MoodEvent("user123", "Happy", "😄", "Had a great day!", "With friends", false);
        moodEvent.setDate(new Date());
    }

    @Test
    public void testMoodEventPropertiesSetCorrectly() {
        assertEquals("Happy", moodEvent.getMoodTitle());
        assertEquals("😄", moodEvent.getEmoji());
        assertEquals("Had a great day!", moodEvent.getReasonWhy());
        assertEquals("With friends", moodEvent.getSocialSituation());
        assertEquals("user123", moodEvent.getOwnerUserId());
        assertEquals(5, moodEvent.getIntensity());
        assertNotNull(moodEvent.getDate());
    }
}
