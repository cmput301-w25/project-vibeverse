package com.example.vibeverse;

import android.os.IBinder;
import android.view.WindowManager;
import androidx.test.espresso.Root;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ToastMatcher extends TypeSafeMatcher<Root> {
    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    @Override
    public boolean matchesSafely(Root root) {
        int type = root.getWindowLayoutParams2().type;
        if (type == WindowManager.LayoutParams.TYPE_TOAST ||
                type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) {
            IBinder windowToken = root.getDecorView().getWindowToken();
            IBinder appToken = root.getDecorView().getApplicationWindowToken();
            // If the window token and application window token are the same, then this window isn't contained by any other.
            return windowToken == appToken;
        }
        return false;
    }
}
