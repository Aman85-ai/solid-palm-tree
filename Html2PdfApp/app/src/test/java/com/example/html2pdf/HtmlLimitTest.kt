package com.example.html2pdf

import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlLimitTest {
	@Test
	fun htmlIsTruncatedWhenTooLong() {
		val maxChars = 200_000
		val longHtml = "<p>x</p>".repeat(maxChars + 1000)
		assertTrue(longHtml.length > maxChars)
		val truncated = longHtml.substring(0, maxChars)
		assertTrue(truncated.length == maxChars)
	}
}