// walkthrough/presentation/screens/homes/HomesScreen.kt
package com.example.walkthrough.presentation.screens.homes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.walkthrough.di.RepositoryHolder
import com.example.walkthrough.domain.models.House
import com.example.walkthrough.domain.utils.DateFormatter
import com.example.walkthrough.presentation.components.HouseCard
import com.example.walkthrough.presentation.components.LoadingIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomesScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val houseRepository = RepositoryHolder.getHouseRepository()
    val apartmentRepository = RepositoryHolder.getApartmentRepository()

    val viewModel = remember {
        HomesViewModel(houseRepository, apartmentRepository)
    }

    val houses by viewModel.houses.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val apartmentsCounts by viewModel.apartmentsCounts.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Состояние для диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var houseToDelete by remember { mutableStateOf<House?>(null) }

    // Snackbar для ошибок
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Показываем ошибку через Snackbar
    LaunchedEffect(error) {
        if (error != null) {
            scope.launch {
                snackbarHostState.showSnackbar(error!!)
            }
            // Очищаем ошибку после показа
            // viewModel.clearError() // лучше добавить метод в ViewModel
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои дома") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("export") }) {
                        Icon(Icons.Default.Download, contentDescription = "Экспорт")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_house") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить дом")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator()
                }
                houses.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Нет добавленных домов",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите на кнопку +, чтобы добавить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(houses) { house ->
                            HouseCard(
                                address = house.address,
                                lastUpdated = DateFormatter.formatDate(house.lastUpdated),
                                apartmentsCount = apartmentsCounts[house.id] ?: 0,
                                onClick = {
                                    navController.navigate("walkthrough/${house.id}")
                                },
                                onDelete = {
                                    houseToDelete = house
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog && houseToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                houseToDelete = null
            },
            title = { Text("Удаление дома") },
            text = {
                Text(
                    "Вы уверены, что хотите удалить дом \"${houseToDelete!!.address}\"?\n" +
                            "Все данные о квартирах этого дома будут также удалены."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHouse(houseToDelete!!)
                        showDeleteDialog = false
                        houseToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    houseToDelete = null
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}