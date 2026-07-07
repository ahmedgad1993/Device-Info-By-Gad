package com.deviceinfo.gad

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.deviceinfo.gad.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

import com.deviceinfo.gad.ui.components.GlassCard
import androidx.compose.material3.Text

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GlassCardScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun glasscard_screenshot() {
    composeTestRule.setContent { MyApplicationTheme { GlassCard { Text("Settings") } } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/glasscard.png")
  }
}
