import re

with open('app/src/main/java/com/deviceinfo/gad/HardwareTests.kt', 'r') as f:
    content = f.read()

# Add new tests to the if-else chain
new_if_branches = """    } else if (activeTest == "refresh_rate") {
        RefreshRateTest { activeTest = "" }
    } else if (activeTest == "burn_in") {
        BurnInTest { activeTest = "" }
    } else if (activeTest == "multi_paint") {"""
content = content.replace('    } else if (activeTest == "multi_paint") {', new_if_branches)

# Add cards to the UI
new_cards = """                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Refresh Rate", icon = androidx.compose.material.icons.Icons.Default.Speed, modifier = Modifier.weight(1f)) { activeTest = "refresh_rate" }
                    DiagnosticTestCard(title = "Burn-in Fixer", icon = androidx.compose.material.icons.Icons.Default.FlipToBack, modifier = Modifier.weight(1f)) { activeTest = "burn_in" }
                }"""
content = content.replace('DiagnosticTestCard(title = "Multi Paint", icon = androidx.compose.material.icons.Icons.Default.FormatPaint, modifier = Modifier.weight(1f)) { activeTest = "multi_paint" }\n                }', 'DiagnosticTestCard(title = "Multi Paint", icon = androidx.compose.material.icons.Icons.Default.FormatPaint, modifier = Modifier.weight(1f)) { activeTest = "multi_paint" }\n                }\n' + new_cards)

with open('app/src/main/java/com/deviceinfo/gad/HardwareTests.kt', 'w') as f:
    f.write(content)
