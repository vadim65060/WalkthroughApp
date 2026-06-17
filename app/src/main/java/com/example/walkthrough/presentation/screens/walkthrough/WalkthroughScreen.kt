package com.example.walkthrough.presentation.screens.walkthrough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.walkthrough.di.RepositoryHolder
import com.example.walkthrough.domain.utils.PhoneValidator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun WalkthroughScreen(
    navController: NavController,
    houseId: Long
) {
    val houseRepository = RepositoryHolder.getHouseRepository()
    val apartmentRepository = RepositoryHolder.getApartmentRepository()

    val viewModel = remember(houseId) {
        WalkthroughViewModel(apartmentRepository, houseRepository, houseId)
    }

    val currentApartment by viewModel.currentApartmentNumber.collectAsStateWithLifecycle()
    val stepDirection by viewModel.stepDirection.collectAsStateWithLifecycle()
    val savedSuccess by viewModel.savedSuccess.collectAsStateWithLifecycle()

    // Состояния для полей формы
    var fullName by remember { mutableStateOf("") }
    var appeals by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var attitude by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }

    // Состояние для выпадающего списка отношения
    var expanded by remember { mutableStateOf(false) }
    val attitudeOptions = listOf("позитивно", "нейтрально", "негативно")

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Поле для ручного ввода номера квартиры
    var apartmentNumberInput by remember { mutableStateOf(currentApartment.toString()) }

    // Фокус и навигация по полям
    val focusManager = LocalFocusManager.current
    val focusRequesterFullName = remember { FocusRequester() }
    val focusRequesterAppeals = remember { FocusRequester() }
    val focusRequesterPhone = remember { FocusRequester() }
    val focusRequesterComment = remember { FocusRequester() }

    // Загружаем данные при изменении номера квартиры
    LaunchedEffect(currentApartment) {
        isLoading = true
        val existing = viewModel.loadApartment(houseId, currentApartment)
        if (existing != null && !existing.isNotHome) {
            fullName = existing.fullName
            appeals = existing.appeals
            phone = existing.phone
            attitude = existing.attitude
            comment = existing.comment
        } else {
            fullName = ""
            appeals = ""
            phone = ""
            attitude = ""
            comment = ""
        }
        isLoading = false
        viewModel.resetSavedSuccess()
        apartmentNumberInput = currentApartment.toString()
    }

    LaunchedEffect(savedSuccess) {
        if (savedSuccess && !isLoading) {
            fullName = ""
            appeals = ""
            phone = ""
            attitude = ""
            comment = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обход квартир") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("apartments/$houseId") }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Список квартир")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Карточка с номером квартиры и шагом
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = apartmentNumberInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                apartmentNumberInput = newValue
                                val number = newValue.toIntOrNull()
                                if (number != null && number >= 1) {
                                    viewModel.setCurrentApartmentNumber(number)
                                }
                            }
                        },
                        label = { Text("Номер квартиры") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isLoading,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Шаг: ${if (stepDirection == 1) "+1" else "-1"}")
                        Switch(
                            checked = stepDirection == 1,
                            onCheckedChange = { viewModel.toggleStepDirection() },
                            enabled = !isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Карточка с данными о жильце
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ФИО
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("ФИО") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterFullName),
                        enabled = !isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusRequesterAppeals.requestFocus() }
                        )
                    )

                    // Обращения
                    OutlinedTextField(
                        value = appeals,
                        onValueChange = { appeals = it },
                        label = { Text("Обращения") },
                        placeholder = { Text("Здравствуйте, добрый день и т.д.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterAppeals),
                        enabled = !isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusRequesterPhone.requestFocus() }
                        )
                    )

                    // Телефон
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = phone.isNotBlank() && !PhoneValidator.isValid(phone)
                        },
                        label = { Text("Телефон") },
                        placeholder = { Text("+7 123 456-78-90") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterPhone),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        isError = phoneError,
                        supportingText = {
                            if (phoneError) {
                                Text("Введите корректный номер телефона")
                            }
                        },
                        enabled = !isLoading,
                        singleLine = true,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusRequesterComment.requestFocus() }
                        )
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
                                    text = { Text(when(option) {
                                        "позитивно" -> "😊 Позитивно"
                                        "нейтрально" -> "😐 Нейтрально"
                                        else -> "😞 Негативно"
                                    }) },
                                    onClick = {
                                        attitude = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Комментарий
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Комментарий") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterComment),
                        maxLines = 3,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (phoneError) return@Button
                        scope.launch {
                            viewModel.saveApartment(
                                houseId = houseId,
                                apartmentNumber = currentApartment,
                                fullName = fullName,
                                appeals = appeals,
                                phone = phone,
                                attitude = attitude,
                                comment = comment
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && !phoneError
                ) {
                    Text("💾 Сохранить")
                }

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.markNotHome(houseId, currentApartment)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    enabled = !isLoading
                ) {
                    Text("🚪 Нет дома")
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}