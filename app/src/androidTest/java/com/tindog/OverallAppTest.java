package com.tindog;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.tindog.ui.SplashScreenActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class OverallAppTest {

    @Rule
    public ActivityTestRule<SplashScreenActivity> mActivityTestRule = new ActivityTestRule<>(SplashScreenActivity.class);

    @Test
    public void firstTimeEnteringAppTest() {

        onView(withId(R.id.password))
                .perform(replaceText("alligator"), closeSoftKeyboard());

        onView(withId(R.id.button_done))
                .perform(click());

        onView(withId(R.id.preferences_gender_spinner))
                .perform(click());

        onData(anything())
                .inAdapterView(childAtPosition(withClassName(is("android.widget.PopupWindow$PopupBackgroundView")), 0))
                .atPosition(1)
                .perform(click());

        onView(withId(R.id.action_done))
                .perform(click());

        //overallAppTest();
    }

    @Test
    public void overallAppTest() {

        // Waiting for the splashscreen to finish loading
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.task_selection_find_dog))
                .perform(click());

        onView(withId(R.id.search_screen_profile_selection_recycler_view))
                .perform(actionOnItemAtPosition(0, click()));

        onView(withId(R.id.profiles_pager))
                .perform(swipeLeft())
                .perform(swipeLeft())
                .perform(swipeRight());

        //Skipping test of share button for now, since it requires the user to return to the app manually
        //onView(withId(R.id.dog_profile_share_fab))
        //        .perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        onView(withText("Edit search preferences"))
                .perform(click());

        onView(withId(R.id.preferences_age_spinner))
                .perform(click());

        onData(anything())
                .inAdapterView(childAtPosition(withClassName(is("android.widget.PopupWindow$PopupBackgroundView")), 0))
                .atPosition(1)
                .perform(click());

        onView(withId(R.id.action_done))
                .perform(click());

        onView(allOf(withContentDescription("Navigate up")))
                .perform(click());

        onView(allOf(withContentDescription("Navigate up")))
                .perform(click());

        try {
            onView(allOf(withContentDescription("Interstitial close button")))
                    .perform(click());
        } catch (Exception e) {
            e.printStackTrace();
        }

        onView(withId(R.id.task_selection_update_map))
                .perform(click());

        // Waiting a few seconds to see that the map data loads
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(allOf(withContentDescription("Navigate up")))
                .perform(click());

        onView(withId(R.id.task_selection_find_dog))
                .perform(click());

        onView(withId(R.id.search_screen_show_in_map_button))
                .perform(click());

        // Waiting a few seconds to see that the map data loads
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(allOf(withContentDescription("Navigate up")))
                .perform(click());

        onView(withId(R.id.search_screen_profile_selection_recycler_view))
                .perform(actionOnItemAtPosition(1, click()));

    }

    private static Matcher<View> childAtPosition(final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
