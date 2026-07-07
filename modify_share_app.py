import re

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'r') as f:
    content = f.read()

share_button = """                    TextButton(onClick = onBackup) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup")
                    }
                    TextButton(onClick = { 
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Check out ${app.appName}")
                                putExtra(Intent.EXTRA_TEXT, "Hey! I found this cool app called ${app.appName}. Check it out: https://play.google.com/store/apps/details?id=${app.packageName}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share App"))
                        } catch(e: Exception) {}
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }"""

content = content.replace('                    TextButton(onClick = onBackup) {\n                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))\n                        Spacer(modifier = Modifier.width(4.dp))\n                        Text("Backup")\n                    }', share_button)

with open('app/src/main/java/com/deviceinfo/gad/ApplicationsTab.kt', 'w') as f:
    f.write(content)
