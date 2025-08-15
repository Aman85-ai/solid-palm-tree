package com.example.html2pdf

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
	@get:Rule
	val rule = ActivityScenarioRule(MainActivity::class.java)

	@Test
	fun controlsAreVisible() {
		onView(withId(R.id.htmlInput)).check(matches(isDisplayed()))
		onView(withId(R.id.btnPreview)).check(matches(isDisplayed()))
		onView(withId(R.id.btnConvert)).check(matches(isDisplayed()))
		onView(withId(R.id.btnSave)).check(matches(isDisplayed()))
		onView(withId(R.id.btnShare)).check(matches(isDisplayed()))
	}
}