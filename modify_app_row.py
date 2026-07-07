import re

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'r') as f:
    content = f.read()

# Modify AppRow signature
app_row_sig = """fun AppRow(
    app: AppInfoModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isExpanded: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onBackup: () -> Unit,
    onCopyPackage: () -> Unit,
    modifier: Modifier = Modifier
) {"""
content = re.sub(r'fun AppRow\([\s\S]*?onCopyPackage: \(\) -> Unit\n\) \{', app_row_sig, content)

# Modify AppRow Card modifier
content = content.replace(
    'Card(\n        modifier = Modifier\n            .fillMaxWidth()',
    'Card(\n        modifier = modifier\n            .fillMaxWidth()'
)

# Modify the call
app_row_call = """                        AppRow(
                            modifier = Modifier.animateItem(),
                            app = app,"""
content = content.replace('                        AppRow(\n                            app = app,', app_row_call)

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'w') as f:
    f.write(content)
