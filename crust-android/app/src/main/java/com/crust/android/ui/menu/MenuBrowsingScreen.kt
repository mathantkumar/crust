package com.crust.android.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crust.android.graphql.GetActiveMenuQuery
import com.crust.android.network.NetworkStatus
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBrowsingScreen(vm: MenuViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val networkStatus by vm.networkStatus.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<GetActiveMenuQuery.MenuItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crust Menu") },
                actions = { SyncStatusChip(networkStatus) }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MenuUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                is MenuUiState.Success -> MenuContent(
                    menu = state.menu,
                    onItemClick = { selectedItem = it }
                )

                is MenuUiState.Error -> {
                    val display = state.cachedMenu
                    if (display != null) {
                        Column {
                            OfflineBanner(state.message)
                            MenuContent(menu = display, onItemClick = { selectedItem = it })
                        }
                    } else {
                        ErrorState(message = state.message, onRetry = vm::loadMenu)
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        ModifierDialog(item = item, onDismiss = { selectedItem = null })
    }
}

@Composable
private fun SyncStatusChip(status: NetworkStatus) {
    val isOnline = status == NetworkStatus.Online
    AssistChip(
        onClick = {},
        label = { Text(if (isOnline) "Online" else "Offline") },
        leadingIcon = {
            Icon(
                imageVector = if (isOnline) Icons.Filled.SignalWifi4Bar else Icons.Filled.SignalWifiOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isOnline)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.padding(end = 8.dp)
    )
}

@Composable
private fun OfflineBanner(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Text(
            text = "Offline — showing cached menu. ($message)",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MenuContent(
    menu: GetActiveMenuQuery.GetActiveMenu,
    onItemClick: (GetActiveMenuQuery.MenuItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(menu.categories, key = { it.id }) { category ->
            CategoryCard(category = category, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun CategoryCard(
    category: GetActiveMenuQuery.Category,
    onItemClick: (GetActiveMenuQuery.MenuItem) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                    category.menuItems.forEach { item ->
                        MenuItemRow(item = item, onClick = { onItemClick(item) })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: GetActiveMenuQuery.MenuItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
            if (item.modifierGroups.isNotEmpty()) {
                Text(
                    text = "${item.modifierGroups.size} option group(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        item.basePrice?.let { price ->
            Text(
                text = "$${"%.2f".format(price)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ModifierDialog(
    item: GetActiveMenuQuery.MenuItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.modifierGroups.isEmpty()) {
                    item {
                        Text(
                            "No modifiers available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                items(item.modifierGroups, key = { it.id }) { group ->
                    ModifierGroupSection(group)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ModifierGroupSection(group: GetActiveMenuQuery.ModifierGroup) {
    Column {
        Text(
            text = group.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        group.modifiers.forEach { modifier ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = modifier.name, style = MaterialTheme.typography.bodyMedium)
                modifier.priceAdjustment?.takeIf { it != 0.0 }?.let { adj ->
                    val sign = if (adj > 0) "+" else ""
                    Text(
                        text = "$sign$${"%.2f".format(adj)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Unable to load menu", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
