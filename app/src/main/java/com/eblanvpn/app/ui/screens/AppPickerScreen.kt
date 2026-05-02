package com.eblanvpn.app.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.PerAppMode
import com.eblanvpn.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val icon: Painter?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    settings: AppSettings,
    onPerAppMode: (PerAppMode) -> Unit,
    onTogglePackage: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var modeMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
        loading = false
    }

    val filtered = remember(apps, query, showSystem) {
        apps.asSequence()
            .filter { showSystem || !it.isSystem }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
            .toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Приложения VPN",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "Назад", tint = TextSecondary)
                }
            },
            actions = {
                if (settings.perAppList.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Сброс", color = PurplePrimary)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDeep)
        )

        // Mode selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .clickable { modeMenuExpanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Apps, null, tint = PurpleLight, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Режим",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    settings.perAppMode.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    settings.perAppMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = TextHint)

            DropdownMenu(
                expanded = modeMenuExpanded,
                onDismissRequest = { modeMenuExpanded = false },
                modifier = Modifier.background(SurfaceElevated)
            ) {
                PerAppMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(mode.label, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(mode.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        leadingIcon = {
                            if (settings.perAppMode == mode) {
                                Icon(Icons.Rounded.Check, null, tint = PurplePrimary)
                            } else {
                                Spacer(Modifier.width(24.dp))
                            }
                        },
                        onClick = {
                            onPerAppMode(mode)
                            modeMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Поиск приложений…", color = TextHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextHint) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, null, tint = TextHint)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurplePrimary,
                unfocusedBorderColor = SurfaceElevated,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PurplePrimary,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Toggle: show system apps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .clickable { showSystem = !showSystem }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Settings, null, tint = TextHint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Показывать системные",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = showSystem,
                onCheckedChange = { showSystem = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PurplePrimary,
                    uncheckedThumbColor = TextHint,
                    uncheckedTrackColor = SurfaceElevated
                )
            )
        }

        if (settings.perAppMode == PerAppMode.OFF) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Выберите режим, чтобы выбирать приложения",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PurplePrimary)
            }
        } else {
            Text(
                text = "Выбрано: ${settings.perAppList.size} • Всего: ${filtered.size}",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val checked = app.packageName in settings.perAppList
                    val enabled = settings.perAppMode != PerAppMode.OFF

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .clickable(enabled = enabled) { onTogglePackage(app.packageName) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app.icon != null) {
                                androidx.compose.foundation.Image(
                                    painter = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(Icons.Rounded.Android, null, tint = TextHint, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (enabled) TextPrimary else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextHint,
                                maxLines = 1
                            )
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onTogglePackage(app.packageName) },
                            enabled = enabled,
                            colors = CheckboxDefaults.colors(
                                checkedColor = PurplePrimary,
                                uncheckedColor = TextHint
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val ownPackage = context.packageName
    val rawApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return rawApps.asSequence()
        .filter { it.packageName != ownPackage }
        .map { info ->
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val label = runCatching { pm.getApplicationLabel(info).toString() }
                .getOrDefault(info.packageName)
            val iconPainter: Painter? = runCatching {
                val drawable: Drawable = pm.getApplicationIcon(info)
                val bitmap = drawable.toBitmap(width = 96, height = 96)
                BitmapPainter(bitmap.asImageBitmap())
            }.getOrNull()
            InstalledApp(
                packageName = info.packageName,
                label = label,
                isSystem = isSystem,
                icon = iconPainter
            )
        }
        .sortedWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
        .toList()
}
