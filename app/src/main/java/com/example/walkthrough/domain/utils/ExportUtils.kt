package com.example.walkthrough.domain.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

object ExportUtils {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportToCsv(house: com.example.walkthrough.domain.models.House, apartments: List<com.example.walkthrough.domain.models.Apartment>): String {
        val sb = StringBuilder()
        sb.append("Квартира;ФИО;Обращения;Телефон;Отношение;Комментарий;Дата обхода;Нет дома\n")
        apartments.sortedBy { it.apartmentNumber }.forEach { apt ->
            sb.append("${apt.apartmentNumber};")
            sb.append("${escapeCsv(apt.fullName)};")
            sb.append("${escapeCsv(apt.appeals)};")
            sb.append("${escapeCsv(apt.phone)};")
            sb.append("${escapeCsv(apt.attitude)};")
            sb.append("${escapeCsv(apt.comment)};")
            sb.append("${DateFormatter.formatDate(apt.lastVisitDate)};")
            sb.append("${if (apt.isNotHome) "Да" else "Нет"}\n")
        }
        return sb.toString()
    }

    fun exportToJson(housesData: List<HouseDataExport>): String {
        return gson.toJson(housesData)
    }

    // Сохранение CSV в выбранную папку через SAF (требуется Context)
    suspend fun saveCsvToUri(context: Context, folderUri: Uri, fileName: String, content: String): Boolean {
        return try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            val file = folder.createFile("text/csv", "$fileName.csv") ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Сохранение JSON в выбранную папку через SAF
    suspend fun saveJsonToUri(context: Context, folderUri: Uri, fileName: String, content: String): Boolean {
        return try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            val file = folder.createFile("application/json", "$fileName.json") ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Сохранение во внешнюю папку приложения (оставляем для обратной совместимости)
    fun saveCsvToFile(context: Context, fileName: String, content: String): File? {
        return try {
            val file = File(context.getExternalFilesDir(null), "$fileName.csv")
            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveJsonToFile(context: Context, fileName: String, content: String): File? {
        return try {
            val file = File(context.getExternalFilesDir(null), "$fileName.json")
            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "export") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
    }

    private fun escapeCsv(field: String): String {
        if (field.contains(";") || field.contains("\n") || field.contains("\"")) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}

data class HouseDataExport(
    val address: String,
    val lastUpdated: String,
    val apartments: List<ApartmentDataExport>
)

data class ApartmentDataExport(
    val number: Int,
    val fullName: String,
    val appeals: String,
    val phone: String,
    val attitude: String,
    val comment: String,
    val lastVisitDate: String,
    val isNotHome: Boolean
)