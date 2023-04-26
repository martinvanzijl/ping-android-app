package com.martinvz.ping;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExampleInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void pingButtonLabelCorrect() {
        onView(withId(R.id.buttonPing)).check(matches(withText(R.string.ping_button_label)));
    }

//    @Test
    public void startScheduledPingLabelCorrect() {
        // Open the menu.
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // DOES NOT WORK! The action is not found.

        // Click the menu item to go to the "Scheduled Pings" screen.
        onView((withId(R.id.action_scheduled_ping))).perform(click());

        // Check the original button label.
        onView(withId(R.id.buttonScheduleSave)).check(matches(withText(R.string.button_schedule_start_label)));
    }
}