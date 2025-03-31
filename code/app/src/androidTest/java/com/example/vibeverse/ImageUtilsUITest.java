package com.example.vibeverse;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.vibeverse.ImageUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// ImageUtilsTest.java
@RunWith(AndroidJUnit4.class)
public class ImageUtilsUITest {

    @Test
    public void testCompressBitmap_shouldReduceSize() {
        Bitmap original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap compressed = ImageUtils.compressBitmap(original);

        assertEquals(80, compressed.getWidth());
        assertEquals(80, compressed.getHeight());
    }

}