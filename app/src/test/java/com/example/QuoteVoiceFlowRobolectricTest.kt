package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Deliverable 6: End-to-end Robolectric Compose UI test covering the critical contractor journey:
 * Onboarding -> Record (sample voice transcript) -> Build Quote -> Review/Edit ->
 * Branded Quote Preview (EN/ES toggle) -> Simulate E-Sign -> 1-Tap Convert to Invoice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuoteVoiceFlowRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMs: Long = 8_000L) {
        try {
            composeRule.waitUntil(timeoutMillis = timeoutMs) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            val tree = runCatching { composeRule.onRoot().printToString() }.getOrDefault("<no tree>")
            throw AssertionError("Timed out waiting for tag '$tag'. Current tree:\n$tree", e)
        }
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String, timeoutMs: Long = 8_000L) {
        try {
            composeRule.waitUntil(timeoutMillis = timeoutMs) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            val tree = runCatching { composeRule.onRoot().printToString() }.getOrDefault("<no tree>")
            throw AssertionError("Timed out waiting for text '$text'. Current tree:\n$tree", e)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun record_to_review_to_preview_to_invoice_flow() {
        // Wait for Room startup load to finish and render Onboarding
        waitForTag("onboarding_business_name_input")

        // 1. Onboarding: enter business name, pick Plumbing trade, finish onboarding
        composeRule.onNodeWithTag("onboarding_business_name_input")
            .performScrollTo()
            .performTextInput("Rivera Plumbing LLC")
        composeRule.onNodeWithTag("trade_chip_plumbing")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("onboarding_finish_button")
            .performScrollTo()
            .performClick()

        // 2. Record Screen: wait for navigation, load English Plumbing sample transcript & tap Build Quote
        waitForTag("record_mic_button")
        composeRule.onNodeWithTag("record_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_placeholder_notice").assertIsDisplayed()

        composeRule.onNodeWithTag("sample_transcripts_row")
            .performScrollTo()
        composeRule.onNodeWithTag("sample_transcript_en_plumbing_water_heater")
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("build_quote_button")
            .performScrollTo()
            .performClick()

        // 3. Review/Edit Screen: wait for extraction + Room write, verify customer, proceed to Preview
        waitForTag("preview_quote_button")
        composeRule.onNodeWithText("Marcus Henderson").assertIsDisplayed()
        composeRule.onNodeWithTag("preview_quote_button").performClick()

        // 4. Quote Preview Screen: verify branded preview, toggle output to Spanish, e-sign, convert to Invoice
        waitForTag("branded_quote_preview_card")
        composeRule.onNodeWithTag("branded_quote_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("preview_lang_es")
            .performScrollTo()
            .performClick()
        waitForText("COTIZACIÓN")
        composeRule.onNodeWithText("COTIZACIÓN")
            .performScrollTo()
            .assertIsDisplayed()

        // Simulate customer e-sign & deposit
        composeRule.onNodeWithTag("simulate_esign_button")
            .performScrollTo()
            .performClick()
        waitForTag("confirm_esign_button")
        composeRule.onNodeWithTag("confirm_esign_button").performClick()
        waitForText("ACCEPTED")

        // 1-Tap Convert to Invoice
        composeRule.onNodeWithTag("convert_to_invoice_button")
            .performScrollTo()
            .performClick()

        // 5. Invoice Detail Screen: verify balance due, reminders switch, and Mark Paid CTA
        waitForTag("mark_invoice_paid_button")
        composeRule.onNodeWithTag("invoice_auto_reminder_switch")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("mark_invoice_paid_button")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
