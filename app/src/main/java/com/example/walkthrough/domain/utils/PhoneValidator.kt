package com.example.walkthrough.domain.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneValidator {
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private val defaultRegion = "RU"

    /**
     * Проверяет номер телефона.
     * @param phone строка номера
     * @param areaCode код города (например, "812") – если null, то не подставляется.
     */
    fun isValid(phone: String, areaCode: String? = null): Boolean {
        if (phone.isBlank()) return true

        // Сначала пробуем распарсить как есть
        try {
            val parsed = phoneUtil.parse(phone, defaultRegion)
            if (phoneUtil.isValidNumber(parsed)) return true
        } catch (_: Exception) {
            // игнорируем
        }

        // Если номер невалидный и есть код города, пробуем подставить его
        if (!areaCode.isNullOrBlank()) {
            val cleaned = phone.replace(Regex("[^\\d]"), "")
            // Домашний номер обычно 5-7 цифр
            if (cleaned.length in 5..7) {
                val fullNumber = areaCode + cleaned
                try {
                    val parsed = phoneUtil.parse(fullNumber, defaultRegion)
                    return phoneUtil.isValidNumber(parsed)
                } catch (_: Exception) {
                    // невалидный
                }
            }
        }

        return false
    }

    fun formatForDisplay(phone: String, areaCode: String? = null): String {
        if (phone.isBlank()) return ""

        val fullNumber = if (areaCode != null && phone.replace(Regex("[^\\d]"), "").length in 5..7) {
            areaCode + phone.replace(Regex("[^\\d]"), "")
        } else {
            phone
        }

        return try {
            val parsed = phoneUtil.parse(fullNumber, defaultRegion)
            if (phoneUtil.isValidNumber(parsed)) {
                phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            } else {
                phone
            }
        } catch (_: Exception) {
            phone
        }
    }

    fun formatE164(phone: String, areaCode: String? = null): String? {
        if (phone.isBlank()) return null

        val fullNumber = if (areaCode != null && phone.replace(Regex("[^\\d]"), "").length in 5..7) {
            areaCode + phone.replace(Regex("[^\\d]"), "")
        } else {
            phone
        }

        return try {
            val parsed = phoneUtil.parse(fullNumber, defaultRegion)
            if (phoneUtil.isValidNumber(parsed)) {
                phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}