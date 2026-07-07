package com.deviceinfo.gad.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = BluePrimaryDark,
    secondaryContainer = SecondaryContainerBlueDark,
    onSecondaryContainer = OnSecondaryContainerBlueDark,
    background = BackgroundGrayDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onBackground = OnSurfaceTextDark,
    onSurface = OnSurfaceTextDark,
    onSurfaceVariant = OnSurfaceVariantGrayDark,
    outlineVariant = OutlineGrayDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BluePrimary,
    secondaryContainer = SecondaryContainerBlue,
    onSecondaryContainer = OnSecondaryContainerBlue,
    background = BackgroundGray,
    surface = SurfaceWhite,
    onPrimary = Color.White,
    onBackground = OnSurfaceText,
    onSurface = OnSurfaceText,
    onSurfaceVariant = OnSurfaceVariantGray,
    outlineVariant = OutlineGray
  )

@Composable
fun MyApplicationTheme(
  themeMode: String = "system",
  accentColor: String = "blue",
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    "light" -> false
    "dark", "black" -> true
    else -> isSystemInDarkTheme()
  }

  val isAmoled = themeMode == "black"

  val accentColorValueLight = when (accentColor) {
      "purple" -> Color(0xFF9C27B0)
      "green" -> Color(0xFF4CAF50)
      "orange" -> Color(0xFFFF9800)
      "red" -> Color(0xFFF44336)
      "teal" -> Color(0xFF009688)
      "pink" -> Color(0xFFE91E63)
      "indigo" -> Color(0xFF3F51B5)
      else -> BluePrimary // blue
  }

  val accentColorValueDark = when (accentColor) {
      "purple" -> Color(0xFFD05CE3)
      "green" -> Color(0xFF81C784)
      "orange" -> Color(0xFFFFB74D)
      "red" -> Color(0xFFE57373)
      "teal" -> Color(0xFF4DB6AC)
      "pink" -> Color(0xFFF06292)
      "indigo" -> Color(0xFF7986CB)
      else -> BluePrimaryDark // blue
  }

  val colorScheme =
    when {
      !isAmoled && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      isAmoled -> DarkColorScheme.copy(
          primary = accentColorValueDark,
          background = Color.Black,
          surface = Color(0xFF0F0F0F),
          surfaceVariant = Color(0xFF141414),
          onBackground = Color.White,
          onSurface = Color.White
      )
      darkTheme -> DarkColorScheme.copy(primary = accentColorValueDark)
      else -> LightColorScheme.copy(primary = accentColorValueLight)
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

