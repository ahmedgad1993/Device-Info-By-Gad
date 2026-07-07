package com.deviceinfo.gad
import com.deviceinfo.gad.R
import androidx.compose.ui.res.stringResource

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.format.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.fetch.Fetcher
import coil.fetch.FetchResult
import coil.fetch.DrawableResult
import coil.request.Options
import coil.decode.DataSource

data class AppIconData(val packageName: String)

class AppIconFetcher(
    private val data: AppIconData,
    private val options: Options,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val drawable = try {
            context.packageManager.getApplicationIcon(data.packageName)
        } catch (e: Throwable) {
            androidx.core.content.ContextCompat.getDrawable(context, android.R.mipmap.sym_def_app_icon)!!
        }
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppIconData> {
        override fun create(data: AppIconData, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, options, context)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsTab(viewModel: AppsViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(AppIconFetcher.Factory(context))
            }
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
            .build()
    }
    
    val allApps by viewModel.appList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    
    // UI State
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(AppFilterType.USER) }
    var sortType by remember { mutableStateOf(AppSortType.NAME_AZ) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var expandedPackage by remember { mutableStateOf<String?>(null) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }
    
    val backupUri by viewModel.backupDestinationUri.collectAsStateWithLifecycle()
    
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                viewModel.backupDestinationUri.value = uri
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Backup path saved")
                }
            } catch (e: Exception) {
                coroutineScope.launch { snackbarHostState.showSnackbar("Failed to set backup path") }
            }
        }
    }

    val filteredApps = remember(allApps, debouncedQuery, filterType, sortType) {
        var list = allApps
        if (debouncedQuery.isNotBlank()) {
            list = list.filter { it.appName.contains(debouncedQuery, ignoreCase = true) }
        }
        list = when (filterType) {
            AppFilterType.USER -> list.filter { !it.isSystemApp }
            AppFilterType.SYSTEM -> list.filter { it.isSystemApp }
            AppFilterType.ALL -> list
        }
        when (sortType) {
            AppSortType.NAME_AZ -> list.sortedBy { it.appName.lowercase() }
            AppSortType.NAME_ZA -> list.sortedByDescending { it.appName.lowercase() }
            AppSortType.NEWEST -> list.sortedByDescending { it.installDate }
            AppSortType.LARGEST -> list.sortedByDescending { it.sizeBytes }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadApps()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AnimatedContent(targetState = isSelectionMode, label = "search_bar_anim") { selectionMode ->
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
                            placeholder = { Text(stringResource(R.string.ui_search_apps)) },
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
                                                text = { Text(stringResource(R.string.ui_refresh)) },
                                        onClick = { 
                                            viewModel.loadApps(forceRefresh = true)
                                            isMenuExpanded = false 
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isSelectionMode) "Exit Selection" else "Select") },
                                        onClick = { 
                                            isSelectionMode = !isSelectionMode
                                            if (!isSelectionMode) selectedPackages = emptySet()
                                            isMenuExpanded = false 
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ui_set_backup_path)) },
                                        onClick = { 
                                            folderPicker.launch(null)
                                            isMenuExpanded = false 
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ui_sort_a_z)) },
                                        onClick = { sortType = AppSortType.NAME_AZ; isMenuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ui_sort_z_a)) },
                                        onClick = { sortType = AppSortType.NAME_ZA; isMenuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ui_sort_newest)) },
                                        onClick = { sortType = AppSortType.NEWEST; isMenuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ui_sort_largest)) },
                                        onClick = { sortType = AppSortType.LARGEST; isMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == AppFilterType.USER,
                        onClick = { filterType = AppFilterType.USER },
                        label = { Text(stringResource(R.string.ui_user)) }
                    )
                    FilterChip(
                        selected = filterType == AppFilterType.SYSTEM,
                        onClick = { filterType = AppFilterType.SYSTEM },
                        label = { Text(stringResource(R.string.ui_system)) }
                    )
                    FilterChip(
                        selected = filterType == AppFilterType.ALL,
                        onClick = { filterType = AppFilterType.ALL },
                        label = { Text(stringResource(R.string.ui_all)) }
                    )
                }
                
                AnimatedVisibility(visible = isSelectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allSelected = selectedPackages.size == filteredApps.size && filteredApps.isNotEmpty()
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
                            TextButton(onClick = {
                                if (allSelected) selectedPackages = emptySet()
                                else selectedPackages = filteredApps.map { it.packageName }.toSet()
                            }) {
                                Text(if (allSelected) "Deselect All" else "Select All")
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${selectedPackages.size} Selected", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isSelectionMode && selectedPackages.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${selectedPackages.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(onClick = {
                            val appsToBackup = filteredApps.filter { selectedPackages.contains(it.packageName) }
                            if (appsToBackup.isNotEmpty()) {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val backupDestName = StorageUtil.getReadablePath(context, backupUri)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Backing up ${appsToBackup.size} app(s) to $backupDestName...") }
                                viewModel.backupApks(context, appsToBackup, backupUri, onProgress = {
                                    // Progress feedback
                                }) { s, f, path, errMsg ->
                                    coroutineScope.launch {
                                        val finalPath = if (backupUri != null) StorageUtil.getReadablePath(context, backupUri) else path
                                        val msg = if (f > 0) "Backup finished. Succ: $s, Fail: $f. ${errMsg ?: ""}" else "Successfully backed up to $finalPath"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                            isSelectionMode = false
                            selectedPackages = emptySet()
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Backup", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.ui_backup))
                        }
                    }
                }
            }
        }
    ) { paddingVals ->
        AnimatedContent(
            targetState = isLoading && allApps.isEmpty(),
            label = "apps_loading_state"
        ) { loading ->
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingVals), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AppShortcut, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (allApps.isEmpty()) "No applications found" else "No matching apps", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (debouncedQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { searchQuery = "" }) {
                                Text(stringResource(R.string.ui_clear_search))
                            }
                        }
                    }
                }
            } else {
                androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.loadApps(forceRefresh = true) },
                    modifier = Modifier.fillMaxSize().padding(paddingVals)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        AppRow(
                            modifier = Modifier.animateItem(),
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
                            onCopyPackage = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(context.getString(R.string.ui_package_name), app.packageName))
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.pkg_copied))
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
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
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        label = "bg_color"
    )
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp).animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = AppIconData(app.packageName),
                    imageLoader = imageLoader,
                    contentDescription = app.appName,
                    modifier = Modifier.size(36.dp).clip(CircleShape).animateContentSize(),
                    error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Apps)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                "SYS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick = onCopyPackage
                ) {
                    Text(
                        app.packageName, 
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${app.versionName} • ${Formatter.formatShortFileSize(context, app.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
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
                        Text(stringResource(R.string.ui_backup))
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
                        Text(stringResource(R.string.ui_share))
                    }
                    TextButton(onClick = { 
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.ui_app_info))
                    }
                    TextButton(onClick = { 
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.ui_uninstall), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

enum class AppFilterType { ALL, USER, SYSTEM }
enum class AppSortType { NAME_AZ, NAME_ZA, NEWEST, LARGEST }

