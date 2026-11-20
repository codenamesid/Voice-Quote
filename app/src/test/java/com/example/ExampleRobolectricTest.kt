package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("QuoteVoice", appName)
  }

  @Test
  fun `launcher intent and icons resolve cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    assertNotNull("Expected launchable MAIN/LAUNCHER activity", launchIntent)
    assertEquals("com.example.MainActivity", launchIntent?.component?.className)
    assertNotNull(context.getDrawable(R.mipmap.ic_launcher))
    assertNotNull(context.getDrawable(R.mipmap.ic_launcher_round))
  }
}
