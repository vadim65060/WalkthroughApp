package com.example.walkthrough.presentation.screens.apartments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.walkthrough.di.RepositoryHolder
import com.example.walkthrough.domain.models.Apartment
import com.example.walkthrough.domain.utils.DateFormatter
import com.example.walkthrough.presentation.components.ApartmentCard
import com.example.walkthrough.presentation.components.LoadingIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentsListScreen(
    navController: NavController,
    houseId: Long
) {
    val apartmentRepository = RepositoryHolder.getApartmentRepository()
    val viewModel = remember {
        ApartmentsListViewModel(apartmentRepository)
    }

    val apartments by viewModel.apartments.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingApartment by remember { mutableStateOf<Apartment?>(null) }

    LaunchedEffect(houseId) {
        viewModel.loadApartments(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Квартиры в доме") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("walkthrough/$houseId") {
                        popUpTo("apartments/$houseId") { inclusive = true }
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Продолжить обход")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LoadingIndicator()
            } else if (apartments.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Нет добавленных квартир",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Начните обход, чтобы добавить квартиры",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navController.navigate("walkthrough/$houseId") {
                                popUpTo("apartments/$houseId") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Начать обход")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apartments) { apartment ->
                        ApartmentCard(
                            apartmentNumber = apartment.apartmentNumber,
                            fullName = apartment.fullName,
                            phone = apartment.phone,
                            attitude = apartment.attitude,
                            lastVisitDate = DateFormatter.formatDate(apartment.lastVisitDate),
                            isNotHome = apartment.isNotHome,
                            onClick = {
                                editingApartment = apartment
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && editingApartment != null) {
        EditApartmentDialog(
            apartment = editingApartment!!,
            onDismiss = {
                showEditDialog = false
                editingApartment = null
            },
            onSave = { updatedApartment ->
                viewModel.viewModelScope.launch {
                    viewModel.updateApartment(updatedApartment)
                }
                showEditDialog = false
                editingApartment = null
            },
            onDelete = {
                viewModel.viewModelScope.launch {
                    viewModel.deleteApartment(editingApartment!!)
                }
                showEditDialog = false
                editingApartment = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditApartmentDialog(
    apartment: Apartment,
    onDismiss: () -> Unit,
    onSave: (Apartment) -> Unit,
    onDelete: () -> Unit
) {
    var fullName by remember { mutableStateOf(apartment.fullName) }
    var appeals by remember { mutableStateOf(apartment.appeals) }
    var phone by remember { mutableStateOf(apartment.phone) }
    var attitude by remember { mutableStateOf(apartment.attitude) }
    var comment by remember { mutableStateOf(apartment.comment) }
    var isNotHome by remember { mutableStateOf(apartment.isNotHome) }

    // Состояние для выпадающего списка отношения
    var expanded by remember { mutableStateOf(false) }
    val attitudeOptions = listOf("позитивно", "нейтрально", "негативно")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Квартира ${apartment.apartmentNumber}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("ФИО") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = appeals,
                    onValueChange = { appeals = it },
                    label = { Text("Обращения") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Выпадающий список для отношения
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = attitude,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Отношение") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        attitudeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (option) {
                                            "позитивно" -> "😊 Позитивно"
                                            "нейтрально" -> "😐 Нейтрально"
                                            else -> "😞 Негативно"
                                        }
                                    )
                                },
                                onClick = {
                                    attitude = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Нет дома")
                    Switch(
                        checked = isNotHome,
                        onCheckedChange = { isNotHome = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = apartment.copy(
                        fullName = fullName,
                        appeals = appeals,
                        phone = phone,
                        attitude = attitude,
                        comment = comment,
                        isNotHome = isNotHome,
                        lastVisitDate = System.currentTimeMillis()
                    )
                    onSave(updated)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}