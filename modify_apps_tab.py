import re

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'r') as f:
    content = f.read()

# Add expandedPackage state
content = content.replace(
    'var selectedPackages by remember { mutableStateOf(setOf<String>()) }',
    'var selectedPackages by remember { mutableStateOf(setOf<String>()) }\n    var expandedPackage by remember { mutableStateOf<String?>(null) }'
)

# Modify AppRow call
app_row_call = """                        AppRow(
                            app = app,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            isExpanded = expandedPackage == app.packageName,
                            imageLoader = imageLoader,
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedPackages = selectedPackages - app.packageName
                                    else selectedPackages = selectedPackages + app.packageName
                                } else {
                                    expandedPackage = if (expandedPackage == app.packageName) null else app.packageName
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedPackages = setOf(app.packageName)
                                    expandedPackage = null
                                }
                            },
                            onBackup = {
                                val backupDestName = StorageUtil.getReadablePath(context, backupUri)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Backing up ${app.appName} to $backupDestName...") }
                                viewModel.backupApks(context, listOf(app), backupUri, onProgress = {}) { s, f, path, errMsg ->
                                    coroutineScope.launch {
                                        val finalPath = if (backupUri != null) StorageUtil.getReadablePath(context, backupUri) else path
                                        val msg = if (s > 0) "Backed up to $finalPath" else "Failed to backup ${app.appName}. ${errMsg ?: ""}"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            },
                            onCopyPackage = {"""
content = re.sub(r'                        AppRow\([\s\S]*?onCopyPackage = \{', app_row_call, content)


# Modify AppRow signature and content
app_row_sig = """fun AppRow(
    app: AppInfoModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isExpanded: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onBackup: () -> Unit,
    onCopyPackage: () -> Unit
) {"""
content = re.sub(r'fun AppRow\([\s\S]*?onCopyPackage: \(\) -> Unit\n\) \{', app_row_sig, content)

# Modify AppRow body to include the actions row
actions_row = """            }
            
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = null)
            } else {
                IconButton(onClick = onClick) {
                    Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        AnimatedVisibility(visible = isExpanded && !isSelectionMode) {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onBackup) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup")
                    }
                    TextButton(onClick = { 
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("App Info")
                    }
                    TextButton(onClick = { 
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Uninstall", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}"""
content = re.sub(r'            \}\n            \n            if \(isSelectionMode\) \{[\s\S]*?\}\n    \}\n\}', actions_row, content)

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'w') as f:
    f.write(content)

