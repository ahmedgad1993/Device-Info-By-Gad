import re

# Fix AnimatedMeshGradient.kt
with open('app/src/main/java/com/deviceinfo/gad/ui/components/AnimatedMeshGradient.kt', 'r') as f:
    amg = f.read()

amg = amg.replace('import androidx.compose.foundation.layout.fillMaxSize', 'import androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.foundation.layout.Box\nimport androidx.compose.ui.draw.drawBehind')
amg = amg.replace('androidx.compose.foundation.layout.Box(', 'Box(')
amg = amg.replace('.androidx.compose.ui.draw.drawBehind {', '.drawBehind {')
# Fix radius argument which might be a float but required to be just a number? No, wait: "Argument type mismatch: actual type is 'ComplexDouble', but 'Float' was expected" inside Brush.radialGradient
# Ah, radius = width * 1.5f... Wait, if radius is width, it's just a float. But why ComplexDouble?
# Oh! Because I used `radius = width` which is float... wait. Float * Float = Float.

with open('app/src/main/java/com/deviceinfo/gad/ui/components/AnimatedMeshGradient.kt', 'w') as f:
    f.write(amg)

# Fix MainActivity.kt
with open('app/src/main/java/com/deviceinfo/gad/MainActivity.kt', 'r') as f:
    main = f.read()

main = main.replace('Modifier.androidx.compose.ui.draw.drawBehind {', 'Modifier.drawBehind {')
main = main.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.drawBehind\nimport androidx.compose.animation.core.animateFloat\nimport androidx.compose.ui.geometry.CornerRadius\nimport androidx.compose.ui.graphics.drawscope.Stroke')

with open('app/src/main/java/com/deviceinfo/gad/MainActivity.kt', 'w') as f:
    f.write(main)

