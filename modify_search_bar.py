import re

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'r') as f:
    content = f.read()

search_bar_replacement = """                AnimatedContent(targetState = isSelectionMode, label = "search_bar_anim") { selectionMode ->
                    if (selectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${selectedPackages.size} Selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { isSelectionMode = false; selectedPackages = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            placeholder = { Text("Search Apps...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    Box {
                                        IconButton(onClick = { isMenuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                        }
                                        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Refresh") },"""

content = re.sub(r'                OutlinedTextField\([\s\S]*?text = \{ Text\("Refresh"\) \},', search_bar_replacement, content)

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'w') as f:
    f.write(content)

