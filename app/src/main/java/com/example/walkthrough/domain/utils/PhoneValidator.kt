package com.example.walkthrough.domain.utils

object PhoneValidator {
    fun isValid(phone: String): Boolean {
        if (phone.isBlank()) return true // пустое поле допустимо
        val cleaned = phone.replace(Regex("[^\\d+]"), "")
        return cleaned.length in 10..15 && cleaned.matches(Regex("^[+]?\\d+$"))
    }

    fun formatForDisplay(phone: String): String {
        if (phone.isBlank()) return ""
        val cleaned = phone.replace(Regex("[^\\d]"), "")
        return when {
            cleaned.length == 11 && cleaned.startsWith("7") -> "+7 (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7, 9)}-${cleaned.substring(9, 11)}"
            cleaned.length == 10 -> "+7 (${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6, 8)}-${cleaned.substring(8, 10)}"
            else -> phone
        }
    }
}