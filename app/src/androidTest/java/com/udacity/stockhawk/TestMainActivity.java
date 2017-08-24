package com.udacity.stockhawk;

import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.udacity.stockhawk.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class TestMainActivity {

    @Rule
    public ActivityTestRule<MainActivity> mMainRule = new ActivityTestRule<MainActivity>(MainActivity.class);

    @Test
    public void clickStockTestApple(){

        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0,click()));
        onView(withId(R.id.txtname)).check(matches(withText("Apple Inc.")));
    }

    @Test
    public void clickStockTestFacebook(){
        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(1,click()));
        onView(withId(R.id.txtname)).check(matches(withText("Facebook, Inc.")));
    }

    @Test
    public void clickStockTestMicrosoft(){
        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(2,click()));
        onView(withId(R.id.txtname)).check(matches(withText("Microsoft Corporation")));
    }

    @Test
    public void onClickFABButton(){
        onView(withId(R.id.fab)).perform(click());
    }

    @Test
    public void addTextFABActivityButton(){
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_stock)).perform(typeText("GOOG"), closeSoftKeyboard());
    }

}
