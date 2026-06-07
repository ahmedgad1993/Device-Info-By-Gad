package com.example

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

enum class SortType { NAME_AZ, NAME_ZA, DATE_NEW_OLD, SIZE_LARGE_SMALL }

data class AppItem(
    val name: String,
    val packageName: String,
    val versionName: String,
    val sizeBytes: Long,
    val installDate: Long,
    val isSystem: Boolean,
    val sourceDir: String
)

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var apps by mutableStateOf<List<AppItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val pm = getApplication<Application>().packageManager
            val installedApps = withContext(Dispatchers.IO) {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
            val list = mutableListOf<AppItem>()
            withContext(Dispatchers.IO) {
                for (appInfo in installedApps) {
                    try {
                        val packageInfo = pm.getPackageInfo(appInfo.packageName, 0)
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val file = File(appInfo.sourceDir)
                        val size = if (file.exists()) file.length() else 0L

                        list.add(
                            AppItem(
                                name = pm.getApplicationLabel(appInfo).toString(),
                                packageName = appInfo.packageName,
                                versionName = packageInfo.versionName ?: "Unknown",
                                sizeBytes = size,
                                installDate = packageInfo.firstInstallTime,
                                isSystem = isSystem,
                                sourceDir = appInfo.sourceDir
                            )
                        )
                    } catch (e: Exception) {
                    }
                }
            }
            apps = list
            isLoading = false
        }
    }

    fun getBackupPath(): String? {
        return prefs.getString("backup_path_uri", null)
    }

    fun saveBackupPath(uri: Uri) {
        prefs.edit().putString("backup_path_uri", uri.toString()).apply()
        // taking persistable URI permission
        try {
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
        }
    }

    fun enterMultiSelectMode(initialPackage: String? = null) {
        _isMultiSelectMode.value = true
        if (initialPackage != null) {
            _selectedPackages.value = setOf(initialPackage)
        } else {
            _selectedPackages.value = emptySet()
        }
    }

    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedPackages.value = emptySet()
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackages.value = current
        
        if (_selectedPackages.value.isEmpty()) {
            exitMultiSelectMode()
        }
    }

    fun selectAllApps(displayedApps: List<AppItem>) {
        val packages = displayedApps.map { it.packageName }.toSet()
        _selectedPackages.value = packages
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    fun backupSelectedApps(displayedApps: List<AppItem>) {
        val selectedItems = displayedApps.filter { _selectedPackages.value.contains(it.packageName) }
        exitMultiSelectMode()
        if (selectedItems.isEmpty()) return

        val uriStr = getBackupPath()

        viewModelScope.launch {
            _backupStatus.value = "backing_up"
            val successCount = withContext(Dispatchers.IO) {
                var c = 0
                val resolver = getApplication<Application>().contentResolver
                
                if (uriStr != null) {
                    val dirUri = Uri.parse(uriStr)
                    val dirDoc = DocumentFile.fromTreeUri(getApplication(), dirUri)
                    if (dirDoc != null && dirDoc.canWrite()) {
                        for (app in selectedItems) {
                            try {
                                val apkName = "${app.name}_${app.versionName}.apk".replace(" ", "_").replace("/", "_")
                                var fileDoc = dirDoc.findFile(apkName)
                                if (fileDoc == null) {
                                    fileDoc = dirDoc.createFile("application/vnd.android.package-archive", apkName)
                                }
                                if (fileDoc != null) {
                                    resolver.openOutputStream(fileDoc.uri)?.use { out ->
                                        FileInputStream(File(app.sourceDir)).use { inStream ->
                                            inStream.copyTo(out)
                                        }
                                    }
                                    c++
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    // Fallback to default Download directory
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    
                    for (app in selectedItems) {
                        try {
                            val apkName = "${app.name}_${app.versionName}.apk".replace(" ", "_").replace("/", "_")
                            val outFile = File(downloadsDir, apkName)
                            java.io.FileOutputStream(outFile).use { out ->
                                FileInputStream(File(app.sourceDir)).use { inStream ->
                                    inStream.copyTo(out)
                                }
                            }
                            c++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                c
            }
            if (successCount > 0) {
                _backupStatus.value = "success"
            } else {
                _backupStatus.value = "failed"
            }
        }
    }
    
    fun backupSingleApp(app: AppItem) {
        val displayed = listOf(app)
        val oldSelected = _selectedPackages.value
        _selectedPackages.value = setOf(app.packageName)
        backupSelectedApps(displayed)
        _selectedPackages.value = oldSelected
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsTab(viewModel: AppsViewModel = viewModel()) {
    val context = LocalContext.current
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    var showSystemApps by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var sortType by remember { mutableStateOf(SortType.NAME_AZ) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showTopOverflow by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    
    val backupPathSavedStr = stringResource(R.string.backup_path_saved)
    val backupPathNotSetStr = stringResource(R.string.backup_path_not_set)
    val backupSuccessStr = stringResource(R.string.backup_success)
    val backupFailedStr = stringResource(R.string.backup_failed)

    val dirPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            viewModel.saveBackupPath(it)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    LaunchedEffect(backupStatus) {
        when (backupStatus) {
            "path_not_set" -> snackbarHostState.showSnackbar(backupPathNotSetStr)
            "success" -> snackbarHostState.showSnackbar(backupSuccessStr)
            "failed" -> snackbarHostState.showSnackbar(backupFailedStr)
            "backing_up" -> snackbarHostState.showSnackbar("Backing up...", duration = SnackbarDuration.Short)
        }
        if (backupStatus != null) {
            viewModel.clearBackupStatus()
        }
    }

    val displayedApps = viewModel.apps.filter { 
        it.isSystem == showSystemApps && 
        (it.name.contains(searchQuery.text, ignoreCase = true) || it.packageName.contains(searchQuery.text, ignoreCase = true))
    }.sortedWith(Comparator { a, b ->
        when (sortType) {
            SortType.NAME_AZ -> a.name.compareTo(b.name, ignoreCase = true)
            SortType.NAME_ZA -> b.name.compareTo(a.name, ignoreCase = true)
            SortType.DATE_NEW_OLD -> b.installDate.compareTo(a.installDate)
            SortType.SIZE_LARGE_SMALL -> b.sizeBytes.compareTo(a.sizeBytes)
        }
    })

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Top Bar
            if (isMultiSelectMode) {
                TopAppBar(
                    title = { Text("${selectedPackages.size} Selected", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitMultiSelectMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.backupSelectedApps(displayedApps) }) {
                            Text(stringResource(R.string.done_backup), fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showTopOverflow = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showTopOverflow, onDismissRequest = { showTopOverflow = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_all)) },
                                onClick = {
                                    viewModel.selectAllApps(displayedApps)
                                    showTopOverflow = false
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.tab_apps), fontSize = 18.sp) },
                    actions = {
                        TextButton(onClick = { viewModel.enterMultiSelectMode() }) {
                            Text(stringResource(R.string.select_apps))
                        }
                        IconButton(onClick = { showTopOverflow = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showTopOverflow, onDismissRequest = { showTopOverflow = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_backup_path)) },
                                onClick = {
                                    dirPickerLauncher.launch(null)
                                    showTopOverflow = false
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }

            // Search & Sort Top Bar
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_apps)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_name_az)) },
                            onClick = { sortType = SortType.NAME_AZ; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_name_za)) },
                            onClick = { sortType = SortType.NAME_ZA; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_date_new)) },
                            onClick = { sortType = SortType.DATE_NEW_OLD; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_size_large)) },
                            onClick = { sortType = SortType.SIZE_LARGE_SMALL; showSortMenu = false }
                        )
                    }
                }
            }

            // System/User toggle
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.user_apps), fontWeight = if (!showSystemApps) FontWeight.Bold else FontWeight.Normal, color = if (!showSystemApps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(
                    checked = showSystemApps,
                    onCheckedChange = { showSystemApps = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(stringResource(R.string.sys_apps), fontWeight = if (showSystemApps) FontWeight.Bold else FontWeight.Normal, color = if (showSystemApps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item {
                        Text(stringResource(R.string.all_apps) + ": ${displayedApps.size}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(displayedApps) { app ->
                        AppCard(
                            app = app,
                            context = context,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = selectedPackages.contains(app.packageName),
                            onAppSelect = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleAppSelection(app.packageName)
                                }
                            },
                            onAppLongClick = {
                                if (!isMultiSelectMode) {
                                    viewModel.enterMultiSelectMode(app.packageName)
                                }
                            },
                            onBackupClick = {
                                viewModel.backupSingleApp(app)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    app: AppItem,
    context: Context,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onAppSelect: () -> Unit,
    onAppLongClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(app.installDate))
    val sizeStr = formatBytes(app.sizeBytes)
    
    val packageManager = context.packageManager
    var appIcon by remember { mutableStateOf<Drawable?>(null) }
    
    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            try {
                appIcon = packageManager.getApplicationIcon(app.packageName)
            } catch(e: Exception){}
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = { onAppSelect() },
                onLongClick = { onAppLongClick() }
            ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onAppSelect() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                AsyncImage(
                    model = appIcon,
                    contentDescription = app.name,
                    modifier = Modifier.size(48.dp).padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                    Text(app.packageName, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                if (!app.isSystem && !isMultiSelectMode) {
                    FilledTonalIconButton(onClick = onBackupClick) {
                        Icon(Icons.Default.SaveAlt, contentDescription = stringResource(R.string.extract_apk))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_version), fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(app.versionName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_size), fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sizeStr, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_install_date), fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateStr, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
