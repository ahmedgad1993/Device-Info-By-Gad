package com.deviceinfo.gad

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
//import androidx.compose.ui.test.junit4.createComposeRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

//    @get:Rule
//    val composeTestRule = createComposeRule()

  @Test
  fun testCpuHardwareViewModelInit() {
      val app = ApplicationProvider.getApplicationContext<android.app.Application>()
      val viewModel = CpuHardwareViewModel(app)
      
      org.junit.Assert.assertNotNull(viewModel.cpuArch)
      org.junit.Assert.assertNotNull(viewModel.osVersion)
  }
}

