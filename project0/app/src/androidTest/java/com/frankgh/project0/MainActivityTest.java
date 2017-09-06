package com.frankgh.project0;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

/**
 * @author Francisco Guerrero <email>afguerrerohernan@wpi.edu</email> on 9/5/17.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule(MainActivity.class);

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.frankgh.project0", appContext.getPackageName());
    }

    @Test
    public void allTextViewsAreDisplayed() {
        onView(withText("My")).check(matches(isDisplayed()));
        onView(withText("Name")).check(matches(isDisplayed()));
        onView(withText("is")).check(matches(isDisplayed()));
        onView(withText("Andres")).check(matches(isDisplayed()));
        onView(withText("Guerrero")).check(matches(isDisplayed()));
    }
}
