package com.eblanvpn.app.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eblanvpn.app.data.model.Subscription
import com.eblanvpn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    subscriptions: List<Subscription>,
    refreshingIds: Set<Long>,
    onAdd: (name: String, url: String) -> Unit,
    onRefresh: (Subscription) -> Unit,
    onRefreshAll: () -> Unit,
    onDelete: (Subscription) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("Подписки", color = TextPrimary, fontWeight = FontWeight.Bold)
                        if (subscriptions.isNotEmpty()) {
                            Text(
                                "${subscriptions.size} шт.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Назад", tint = TextSecondary)
                    }
                },
                actions = {
                    if (subscriptions.isNotEmpty()) {
                        val anyRefreshing = refreshingIds.isNotEmpty()
                        IconButton(onClick = onRefreshAll, enabled = !anyRefreshing) {
                            Icon(
                                Icons.Rounded.Sync,
                                "Обновить все",
                                tint = if (anyRefreshing) TextHint else PurpleLight
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDeep)
            )

            if (subscriptions.isEmpty()) {
                EmptySubscriptionsState(
                    onAdd = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(subscriptions, key = { it.id }) { sub ->
                        SubscriptionItem(
                            subscription = sub,
                            isRefreshing = sub.id in refreshingIds,
                            onRefresh = { onRefresh(sub) },
                            onDelete = { onDelete(sub) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = PurplePrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Добавить подписку")
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onConfirm = { name, url ->
                onAdd(name, url)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun SubscriptionItem(
    subscription: Subscription,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(PurplePrimary.copy(alpha = 0.07f), SurfaceCard)
                )
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(PurplePrimary, CyanAccent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CloudSync, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subscription.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    subscription.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Серверов: ${subscription.serverCount}  •  ${relativeTime(subscription.lastUpdated)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }

            Spacer(Modifier.width(8.dp))

            if (isRefreshing) {
                CircularProgressIndicator(
                    color = PurpleLight,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, "Обновить", tint = PurpleLight)
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreVert, "Меню", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Обновить", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, null, tint = TextSecondary) },
                        onClick = { showMenu = false; onRefresh() }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить", color = Error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSubscriptionDialog(
    onConfirm: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Новая подписка", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (необязательно)") },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Ссылка подписки") },
                    placeholder = { Text("https://…", color = TextHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (url.isNotBlank()) onConfirm(name, url) },
                enabled = url.isNotBlank()
            ) {
                Text("Добавить", color = PurplePrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EmptySubscriptionsState(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Rounded.CloudSync,
            contentDescription = null,
            tint = TextHint,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Нет подписок",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Добавьте ссылку подписки — приложение само скачает\nи обновит список серверов",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Добавить подписку")
        }
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = SurfaceElevated,
    focusedLabelColor = PurpleLight,
    unfocusedLabelColor = TextHint,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PurplePrimary,
    focusedContainerColor = SurfaceElevated,
    unfocusedContainerColor = SurfaceElevated
)

private fun relativeTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "ещё не обновлялось"
    val diff = System.currentTimeMillis() - epochMillis
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "только что"
        minutes < 60 -> "$minutes мин. назад"
        hours < 24 -> "$hours ч. назад"
        days < 30 -> "$days дн. назад"
        else -> "давно"
    }
}
