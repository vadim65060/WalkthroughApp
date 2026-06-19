package com.example.walkthrough.presentation.screens.addhouse

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.walkthrough.di.RepositoryHolder
import com.example.walkthrough.domain.utils.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHouseScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val houseRepository = RepositoryHolder.getHouseRepository()
    val viewModel = remember { AddHouseViewModel(houseRepository) }

    // Состояния формы
    var address by remember { mutableStateOf("") }
    var cityCode by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var isLocationLoading by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var useManualInput by remember { mutableStateOf(false) } // false – авто-заполнение
    val scope = rememberCoroutineScope()

    // Лаунчер для запроса разрешений
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Вызов suspend-функции внутри корутины
            scope.launch {
                requestLocation(
                    context = context,
                    onStart = { isLocationLoading = true },
                    onSuccess = { addr, code ->
                        if (!useManualInput) {
                            address = addr
                            cityCode = code
                        }
                        locationError = null
                    },
                    onError = { error ->
                        locationError = error
                    },
                    onFinish = { isLocationLoading = false }
                )
            }
        } else {
            Toast.makeText(
                context,
                "Геолокация запрещена, адрес нужно ввести вручную",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Запрос разрешения при первом открытии
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Разрешение уже есть – определяем местоположение
            requestLocation(
                context = context,
                onStart = { isLocationLoading = true },
                onSuccess = { addr, code ->
                    if (!useManualInput) {
                        address = addr
                        cityCode = code
                    }
                    locationError = null
                },
                onError = { error -> locationError = error },
                onFinish = { isLocationLoading = false }
            )
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить дом") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Индикация загрузки геолокации
            if (isLocationLoading) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Определение местоположения...")
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ошибка геолокации
            if (locationError != null) {
                Text(
                    text = locationError!!,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Toggle: ручной ввод
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ручной ввод адреса")
                Switch(
                    checked = useManualInput,
                    onCheckedChange = { isManual ->
                        useManualInput = isManual
                        if (isManual) {
                            val cityPart = address.substringBefore(",").trim()
                            address = if (cityPart.isNotEmpty()) "$cityPart, " else ""
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка повторного запроса геолокации (доступна, если ручной ввод выключен)
            Button(
                onClick = {
                    // Вызов suspend-функции внутри корутины
                    scope.launch {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            requestLocation(
                                context = context,
                                onStart = { isLocationLoading = true },
                                onSuccess = { addr, code ->
                                    if (!useManualInput) {
                                        address = addr
                                        cityCode = code
                                    }
                                    locationError = null
                                },
                                onError = { error -> locationError = error },
                                onFinish = { isLocationLoading = false }
                            )
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLocationLoading && !isSaving && !useManualInput
            ) {
                Text("📍 Определить адрес")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Поле адреса
            OutlinedTextField(
                value = address,
                onValueChange = { if (useManualInput) address = it },
                label = { Text("Адрес дома") },
                placeholder = { Text("ул. Ленина, д. 1") },
                modifier = Modifier.fillMaxWidth(),
                enabled = useManualInput && !isSaving && !isLocationLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Поле кода города
            OutlinedTextField(
                value = cityCode,
                onValueChange = { if (useManualInput) cityCode = it },
                label = { Text("Код города (для телефонов)") },
                placeholder = { Text("812, 495, 343...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = useManualInput && !isSaving && !isLocationLoading,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка сохранения
            Button(
                onClick = {
                    if (address.isNotBlank()) {
                        scope.launch {
                            isSaving = true
                            val houseId = viewModel.addHouse(address, cityCode)
                            isSaving = false
                            if (houseId != null) {
                                navController.navigate("walkthrough/$houseId") {
                                    popUpTo("homes") { inclusive = false }
                                }
                            } else {
                                Toast.makeText(context, "Ошибка сохранения дома", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = address.isNotBlank() && !isSaving && !isLocationLoading
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Сохранить и начать обход")
            }
        }
    }
}

// Вспомогательная suspend-функция для запроса геолокации
private suspend fun requestLocation(
    context: android.content.Context,
    onStart: () -> Unit,
    onSuccess: (address: String, cityCode: String) -> Unit,
    onError: (String) -> Unit,
    onFinish: () -> Unit
) {
    try {
        onStart()
        LocationHelper.init(context)
        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            val addressResult = LocationHelper.getAddressFromLocation(
                context,
                location.latitude,
                location.longitude
            )
            if (addressResult != null) {
                onSuccess(addressResult.fullAddress, addressResult.cityCode)
            } else {
                onError("Не удалось определить адрес")
            }
        } else {
            onError("Не удалось получить координаты")
        }
    } catch (e: Exception) {
        onError("Ошибка геолокации: ${e.message}")
    } finally {
        onFinish()
    }
}