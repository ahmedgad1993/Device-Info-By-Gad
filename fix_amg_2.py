with open('app/src/main/java/com/deviceinfo/gad/ui/components/AnimatedMeshGradient.kt', 'r') as f:
    text = f.read()

text = text.replace('val color4 = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)', 'val color4 = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)\n    val bgColor = MaterialTheme.colorScheme.background')
text = text.replace('drawRect(color = MaterialTheme.colorScheme.background)', 'drawRect(color = bgColor)')

with open('app/src/main/java/com/deviceinfo/gad/ui/components/AnimatedMeshGradient.kt', 'w') as f:
    f.write(text)
