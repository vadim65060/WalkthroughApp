package com.example.walkthrough.presentation.screens.addhouse

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    val viewModel = remember {
        AddHouseViewModel(houseRepository)
    }

    var address by remember { mutableStateOf("") }
    var cityCode by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var isLocationLoading by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Лаунчер для запроса разрешений на геолокацию
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение получено — определяем местоположение
            scope.launch {
                isLocationLoading = true
                locationError = null
                try {
                    LocationHelper.init(context)
                    val location = LocationHelper.getCurrentLocation(context)
                    if (location != null) {
                        val addressResult = LocationHelper.getAddressFromLocation(
                            context,
                            location.latitude,
                            location.longitude
                        )
                        if (addressResult != null) {
                            address = addressResult.fullAddress
                            cityCode = addressResult.cityCode
                        } else {
                            locationError = "Не удалось определить адрес"
                        }
                    } else {
                        locationError = "Не удалось получить координаты"
                    }
                } catch (e: Exception) {
                    locationError = "Ошибка геолокации: ${e.message}"
                }
                isLocationLoading = false
            }
        } else {
            Toast.makeText(context, "Геолокация запрещена, адрес нужно ввести вручную", Toast.LENGTH_LONG).show()
        }
    }

    // Запрос разрешения при первом открытии
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Разрешение уже есть — определяем местоположение
            isLocationLoading = true
            locationError = null
            try {
                LocationHelper.init(context)
                val location = LocationHelper.getCurrentLocation(context)
                if (location != null) {
                    val addressResult = LocationHelper.getAddressFromLocation(
                        context,
                        location.latitude,
                        location.longitude
                    )
                    if (addressResult != null) {
                        address = addressResult.fullAddress
                        cityCode = addressResult.cityCode
                    } else {
                        locationError = "Не удалось определить адрес"
                    }
                } else {
                    locationError = "Не удалось получить координаты"
                }
            } catch (e: Exception) {
                locationError = "Ошибка геолокации: ${e.message}"
            }
            isLocationLoading = false
        } else {
            // Запрашиваем разрешение
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
            if (isLocationLoading) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Определение местоположения...")
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (locationError != null) {
                Text(
                    text = locationError!!,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес дома") },
                placeholder = { Text("ул. Ленина, д. 1") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && !isLocationLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cityCode,
                onValueChange = { cityCode = it },
                label = { Text("Код города (для телефонов)") },
                placeholder = { Text("812, 495, 343...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && !isLocationLoading,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (address.isNotBlank()) {
                        scope.launch {
                            isSaving = true
                            val houseId = viewModel.addHouse(address, cityCode)
                            isSaving = false
                            navController.navigate("walkthrough/$houseId") {
                                popUpTo("homes") { inclusive = false }
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