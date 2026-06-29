package com.example.mobiletest.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletest.data.model.Segment
import com.example.mobiletest.data.model.ShippingItinerary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipListScreen(
    modifier: Modifier = Modifier,
    vm: ShipListViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.message) {
        if (state.message.isNotBlank() && state.message != "No data") {
            delay(2000)
            vm.onClearMessage()
        }
    }

    LaunchedEffect(state.showCreateDialog) {
        if (!state.showCreateDialog && state.message.startsWith("Created")) {
            listState.animateScrollToItem(0)
        }
    }



    if (state.showCreateDialog) {
        ShippingDialog(
            title = "Create Shipment",
            value = state.validityInput,
            onValueChange = vm::onValidityInput,
            onConfirm = vm::onConfirmCreate,
            onDismiss = vm::onDismissCreate,
            confirmEnabled = !state.loading
        )
    }

    if (state.showUpdateDialog) {
        ShippingDialog(
            title = "Update Shipment",
            value = state.updateValidityInput,
            onValueChange = vm::onUpdateValidityInput,
            onConfirm = vm::onConfirmUpdate,
            onDismiss = vm::onDismissUpdate,
            confirmEnabled = !state.loading
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ship List") },
                actions = {
                    IconButton(onClick = vm::onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                    IconButton(onClick = vm::onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (state.list.isEmpty() && !state.loading && !state.refreshing && state.message.isNotBlank()) {
                Text(
                    if (state.message == "No data") "No data, tap + to add" else state.message,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.userId,
                    onValueChange = vm::onUserIdChanged,
                    label = { Text("UserId") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = vm::onPullRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.list, key = { it.id ?: "" }) { item ->
                            ShippingCard(
                                modifier = Modifier.animateItem(),
                                item = item,
                                nowMillis = state.nowMillis,
                                onEdit = { vm.onUpdate(item.id ?: "") },
                                onDelete = { vm.onDelete(item.id ?: "") }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.message.isNotBlank() && state.message != "No data",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                val ok = state.message.startsWith("Created") ||
                        state.message.startsWith("Updated") ||
                        state.message.startsWith("Deleted")
                Text(
                    state.message,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (ok) Color.Green else Color.Red)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            if (state.loading) {
                //拦截所有交互
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = true, onClick = {})
                        .background(Color.Transparent)
                )
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ShippingCard(
    modifier: Modifier = Modifier,
    item: ShippingItinerary,
    nowMillis: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val expiryMillis = item.expiryTime?.toLongOrNull()?.times(1000L) ?: 0L
    val remaining = if (expiryMillis > 0) expiryMillis - nowMillis else 0L
    val valid = remaining > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            if (!valid) {
                Text(
                    "Expired",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red.copy(alpha = 0.1f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column {
                val route = buildRoute(item.segments)
                if (route.isNotBlank()) {
                    Text(route, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    item.shipReference ?: "--",
                    fontSize = 13.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (remaining > 0) {
                            Text(
                                formatCountdown(remaining),
                                fontSize = 18.sp,
                                color = Color.Blue
                            )
                        }
                        Text(
                            "Token: ${item.shipToken ?: "--"}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    if (millis <= 0) return "00:00:00"
    val total = millis / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "${pad(h)}:${pad(m)}:${pad(s)}"
}

private fun pad(value: Long): String = value.toString().padStart(2, '0')

@Composable
private fun ShippingDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Validity (seconds)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = confirmEnabled) {
                Text("Cancel")
            }
        }
    )
}

private fun buildRoute(segments: List<Segment>?): String {
    if (segments.isNullOrEmpty()) return ""
    val cities = mutableListOf<String?>()
    segments.forEach { seg ->
        val pair = seg.originAndDestinationPair ?: return@forEach
        if (cities.isEmpty()) cities.add(pair.originCity ?: pair.origin?.code)
        cities.add(pair.destinationCity ?: pair.destination?.code)
    }
    return cities.filterNotNull().joinToString(" → ")
}
