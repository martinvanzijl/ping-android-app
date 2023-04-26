package com.martinvz.ping;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
public class ScheduledPingActivityTest {

    @Rule
    public ActivityScenarioRule<ScheduledPingActivity> activityRule =
            new ActivityScenarioRule<>(ScheduledPingActivity.class);

    @Test
    public void startScheduledPingLabelCorrect() {
        // Check the original button label.
        onView(withId(R.id.buttonScheduleSave)).check(matches(withText(R.string.button_schedule_start_label)));

        // TODO: Select a contact.

//        // Click the "start" button.
//        onView(withId(R.id.buttonScheduleSave)).perform(click());
//
//        // Check the label is updated.
//        onView(withId(R.id.buttonScheduleSave)).check(matches(withText(R.string.button_schedule_restart_label)));
    }
}