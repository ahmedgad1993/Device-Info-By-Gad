with open('app/src/main/java/com/deviceinfo/gad/ui/components/AnimatedMeshGradient.kt', 'r') as f:
    text = f.read()

text = text.replace('val offset1 by if', 'val offset1State = if')
text = text.replace('val offset2 by if', 'val offset2State = if')
text = text.replace('val color1 =', 'val offset1 = offset1State.value\n    val offset2 = offset2State.value\n    val color1 =')

with open('app/src/main/java/com/deviceinfo/gad/ui/components/AnimatedMeshGradient.kt', 'w') as f:
    f.write(text)
