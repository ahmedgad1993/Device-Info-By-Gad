package com.deviceinfo.gad.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SocialIcons {
    val Facebook: ImageVector
        get() = ImageVector.Builder(
            name = "Facebook", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(22.675f, 0f)
                lineTo(1.325f, 0f)
                curveTo(0.593f, 0f, 0f, 0.593f, 0f, 1.325f)
                lineTo(0f, 22.676f)
                curveTo(0f, 23.407f, 0.593f, 24f, 1.325f, 24f)
                lineTo(12.82f, 24f)
                lineTo(12.82f, 14.706f)
                lineTo(9.692f, 14.706f)
                lineTo(9.692f, 11.084f)
                lineTo(12.82f, 11.084f)
                lineTo(12.82f, 8.413f)
                curveTo(12.82f, 5.312f, 14.713f, 3.625f, 17.479f, 3.625f)
                curveTo(18.804f, 3.625f, 19.941f, 3.724f, 20.272f, 3.768f)
                lineTo(20.272f, 7.007f)
                lineTo(18.352f, 7.008f)
                curveTo(16.85f, 7.008f, 16.559f, 7.722f, 16.559f, 8.772f)
                lineTo(16.559f, 11.084f)
                lineTo(20.151f, 11.084f)
                lineTo(19.683f, 14.706f)
                lineTo(16.559f, 14.706f)
                lineTo(16.559f, 24f)
                lineTo(22.675f, 24f)
                curveTo(23.407f, 24f, 24f, 23.407f, 24f, 22.676f)
                lineTo(24f, 1.325f)
                curveTo(24f, 0.593f, 23.407f, 0f, 22.675f, 0f)
                close()
            }
        }.build()

    val Gmail: ImageVector
        get() = ImageVector.Builder(
            name = "Gmail", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(24f, 5.457f)
                lineTo(24f, 18.917f)
                curveTo(24f, 20.063f, 23.072f, 21f, 21.917f, 21f)
                lineTo(17.917f, 21f)
                lineTo(17.917f, 9.457f)
                lineTo(12f, 13.9f)
                lineTo(6.083f, 9.457f)
                lineTo(6.083f, 21f)
                lineTo(2.083f, 21f)
                curveTo(0.928f, 21f, 0f, 20.063f, 0f, 18.917f)
                lineTo(0f, 5.457f)
                curveTo(0f, 3.864f, 1.838f, 2.977f, 3.083f, 3.911f)
                lineTo(12f, 10.6f)
                lineTo(20.917f, 3.911f)
                curveTo(22.162f, 2.977f, 24f, 3.864f, 24f, 5.457f)
                close()
            }
        }.build()

    val WhatsApp: ImageVector
        get() = ImageVector.Builder(
            name = "WhatsApp", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20.52f, 3.44f)
                curveTo(18.24f, 1.16f, 15.21f, 0f, 11.98f, 0f)
                curveTo(5.34f, 0f, 0.0f, 5.34f, 0.0f, 11.98f)
                curveTo(0.0f, 14.11f, 0.55f, 16.18f, 1.58f, 18.01f)
                lineTo(0.0f, 24.0f)
                lineTo(6.12f, 22.4f)
                curveTo(7.88f, 23.34f, 9.89f, 23.85f, 11.98f, 23.85f)
                curveTo(18.62f, 23.85f, 23.96f, 18.51f, 23.96f, 11.87f)
                curveTo(23.96f, 8.64f, 22.7f, 5.61f, 20.42f, 3.34f)
                lineTo(20.52f, 3.44f)
                close()
                moveTo(12.0f, 21.84f)
                curveTo(10.23f, 21.84f, 8.52f, 21.36f, 7.02f, 20.46f)
                lineTo(6.66f, 20.25f)
                lineTo(3.11f, 21.18f)
                lineTo(4.06f, 17.72f)
                lineTo(3.83f, 17.35f)
                curveTo(2.83f, 15.75f, 2.3f, 13.91f, 2.3f, 12.0f)
                curveTo(2.3f, 6.64f, 6.66f, 2.28f, 12.02f, 2.28f)
                curveTo(14.62f, 2.28f, 17.06f, 3.29f, 18.9f, 5.13f)
                curveTo(20.74f, 6.96f, 21.75f, 9.4f, 21.75f, 12.0f)
                curveTo(21.75f, 17.36f, 17.37f, 21.84f, 12.0f, 21.84f)
                close()
                moveTo(17.33f, 14.56f)
                curveTo(17.04f, 14.41f, 15.62f, 13.71f, 15.35f, 13.61f)
                curveTo(15.08f, 13.51f, 14.89f, 13.46f, 14.7f, 13.76f)
                curveTo(14.51f, 14.05f, 13.97f, 14.69f, 13.8f, 14.88f)
                curveTo(13.63f, 15.07f, 13.46f, 15.09f, 13.17f, 14.94f)
                curveTo(12.88f, 14.79f, 11.95f, 14.49f, 10.84f, 13.49f)
                curveTo(9.97f, 12.7f, 9.38f, 11.75f, 9.21f, 11.45f)
                curveTo(9.04f, 11.15f, 9.19f, 10.99f, 9.34f, 10.84f)
                curveTo(9.47f, 10.71f, 9.63f, 10.51f, 9.77f, 10.33f)
                curveTo(9.92f, 10.15f, 9.97f, 10.03f, 10.07f, 9.83f)
                curveTo(10.17f, 9.63f, 10.12f, 9.46f, 10.04f, 9.31f)
                curveTo(9.97f, 9.16f, 9.39f, 7.71f, 9.15f, 7.12f)
                curveTo(8.91f, 6.55f, 8.67f, 6.62f, 8.49f, 6.61f)
                curveTo(8.33f, 6.6f, 8.13f, 6.6f, 7.94f, 6.6f)
                curveTo(7.75f, 6.6f, 7.44f, 6.67f, 7.18f, 6.96f)
                curveTo(6.91f, 7.25f, 6.16f, 7.96f, 6.16f, 9.4f)
                curveTo(6.16f, 10.84f, 7.2f, 12.23f, 7.35f, 12.43f)
                curveTo(7.5f, 12.63f, 9.42f, 15.58f, 12.38f, 16.85f)
                curveTo(13.08f, 17.15f, 13.63f, 17.33f, 14.07f, 17.47f)
                curveTo(14.77f, 17.69f, 15.41f, 17.66f, 15.9f, 17.58f)
                curveTo(16.45f, 17.49f, 17.57f, 16.88f, 17.8f, 16.21f)
                curveTo(18.03f, 15.54f, 18.03f, 14.97f, 17.95f, 14.85f)
                curveTo(17.88f, 14.73f, 17.69f, 14.63f, 17.4f, 14.48f)
                lineTo(17.33f, 14.56f)
                close()
            }
        }.build()
}
