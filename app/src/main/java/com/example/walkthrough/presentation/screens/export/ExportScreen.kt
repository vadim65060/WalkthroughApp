package com.example.walkthrough.presentation.screens.export

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.walkthrough.di.RepositoryHolder
import com.example.walkthrough.domain.utils.DateFormatter
import com.example.walkthrough.domain.utils.ExportUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController
) {
    val houseRepository = RepositoryHolder.getHouseRepository()
    val apartmentRepository = RepositoryHolder.getApartmentRepository()
    val viewModel = remember {
        ExportViewModel(houseRepository, apartmentRepository)
    }

    val houses by viewModel.houses.collectAsStateWithLifecycle()
    val selectedHouses by viewModel.selectedHouses.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }

    // URI выбранной папки для экспорта
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var folderName by remember { mutableStateOf<String?>(null) }

    // Лаунчер для выбора папки
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFolderUri = uri
            // Сохраняем права на запись в эту папку
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // Пытаемся получить имя папки (для отображения)
            val displayName = try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex("_display_name")
                        if (nameIndex != -1) it.getString(nameIndex) else uri.lastPathSegment
                    } else uri.lastPathSegment
                }
            } catch (e: Exception) {
                uri.lastPathSegment
            }
            folderName = displayName ?: "Выбранная папка"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экспорт данных") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.selectAllHouses() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Выбрать все")
                    }
                    Button(
                        onClick = { viewModel.clearSelection() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сбросить")
                    }
                }
            }

            // Выбор папки
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Папка для экспорта",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = folderName ?: "Не выбрана",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedFolderUri == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { folderPickerLauncher.launch(null) }
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Выбрать")
                    }
                }
            }

            Text(
                text = "Выберите дома для экспорта:",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(houses) { house ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = house.address,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Последний обход: ${DateFormatter.formatDate(house.lastUpdated)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = selectedHouses.contains(house.id),
                                onCheckedChange = { viewModel.toggleHouseSelection(house.id) }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedHouses.isEmpty()) {
                        Text(
                            text = "Выберите хотя бы один дом для экспорта",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (selectedFolderUri == null) {
                        Text(
                            text = "Выберите папку для сохранения файлов",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // CSV экспорт
                        Button(
                            onClick = {
                                if (selectedHouses.isEmpty() || selectedFolderUri == null) {
                                    Toast.makeText(
                                        context,
                                        if (selectedHouses.isEmpty()) "Выберите дома" else "Выберите папку",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                scope.launch {
                                    isExporting = true
                                    val csv = viewModel.generateCsv()
                                    if (csv != null) {
                                        val fileName = "export_${System.currentTimeMillis()}"
                                        val success = ExportUtils.saveCsvToUri(
                                            context,
                                            selectedFolderUri!!,
                                            fileName,
                                            csv
                                        )
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "CSV сохранён в выбранную папку",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Ошибка сохранения CSV",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    isExporting = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedHouses.isNotEmpty() && selectedFolderUri != null && !isExporting
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV")
                        }

                        // JSON экспорт
                        Button(
                            onClick = {
                                if (selectedHouses.isEmpty() || selectedFolderUri == null) {
                                    Toast.makeText(
                                        context,
                                        if (selectedHouses.isEmpty()) "Выберите дома" else "Выберите папку",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                scope.launch {
                                    isExporting = true
                                    val json = viewModel.generateJson()
                                    val fileName = "export_${System.currentTimeMillis()}"
                                    val success = ExportUtils.saveJsonToUri(
                                        context,
                                        selectedFolderUri!!,
                                        fileName,
                                        json
                                    )
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "JSON сохранён в выбранную папку",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Ошибка сохранения JSON",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    isExporting = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedHouses.isNotEmpty() && selectedFolderUri != null && !isExporting
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON")
                        }
                    }

                    // Копирование JSON в буфер обмена
                    Button(
                        onClick = {
                            if (selectedHouses.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Выберите дома для экспорта",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            scope.launch {
                                isExporting = true
                                val json = viewModel.generateJson()
                                ExportUtils.copyToClipboard(context, json, "houses_export")
                                isExporting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedHouses.isNotEmpty() && !isExporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Копировать JSON в буфер обмена")
                    }
                }
            }

            if (isExporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}