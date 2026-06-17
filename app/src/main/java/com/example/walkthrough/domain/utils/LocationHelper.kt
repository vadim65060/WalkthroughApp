package com.example.walkthrough.domain.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

object LocationHelper {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun init(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Получает текущее местоположение с высоким приоритетом.
     * Ожидает, пока точность не станет ≤ 20 метров, или 10 секунд таймаут.
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            var isResolved = false
            var timeoutJob: kotlinx.coroutines.Job? = null

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val bestLocation = result.locations.maxByOrNull { it.accuracy } ?: return
                    // Если точность хорошая или таймаут уже сработал, отправляем результат
                    if (bestLocation.accuracy <= 20f && !isResolved) {
                        isResolved = true
                        timeoutJob?.cancel()
                        continuation.resume(bestLocation)
                    }
                }
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateDistanceMeters(5f)
                .setMinUpdateIntervalMillis(5000)
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // Таймаут через 10 секунд – если не получили хорошую точность, возвращаем лучшее из того, что есть
                timeoutJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(10000.milliseconds)
                    if (!isResolved) {
                        isResolved = true
                        // Получаем последнее известное местоположение как fallback
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            continuation.resume(location)
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    isResolved = true
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    timeoutJob.cancel()
                }
            } catch (_: Exception) {
                if (!isResolved) {
                    isResolved = true
                    continuation.resume(null)
                }
            }
        }
    }

    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): AddressResult? {
        val geocoder = Geocoder(context, java.util.Locale.getDefault())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getAddressAsync(geocoder, latitude, longitude)
        } else {
            getAddressSync(geocoder, latitude, longitude)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddressAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): AddressResult? {
        return suspendCancellableCoroutine { continuation ->
            try {
                geocoder.getFromLocation(latitude, longitude, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val bestAddress = selectBestAddress(addresses)
                        val result = bestAddress?.let { address ->
                            val city = address.locality ?: address.subAdminArea ?: ""
                            val street = address.thoroughfare ?: address.featureName ?: ""
                            val houseNumber = address.subThoroughfare ?: ""
                            val shortStreet = shortenStreet(street)
                            val fullAddress = buildString {
                                if (city.isNotEmpty()) append(city)
                                if (shortStreet.isNotEmpty()) {
                                    if (isNotEmpty()) append(", ")
                                    append(shortStreet)
                                }
                                if (houseNumber.isNotEmpty()) {
                                    if (isNotEmpty()) append(", ")
                                    append(houseNumber)
                                }
                            }
                            AddressResult(
                                fullAddress = fullAddress,
                                city = city,
                                postalCode = address.postalCode ?: "",
                                cityCode = getCityCode(city),
                                countryCode = address.countryCode ?: "RU"
                            )
                        }
                        continuation.resume(result)
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                })
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getAddressSync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): AddressResult? {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 5)
            val bestAddress = selectBestAddress(addresses)
            bestAddress?.let { address ->
                val city = address.locality ?: address.subAdminArea ?: ""
                val street = address.thoroughfare ?: address.featureName ?: ""
                val houseNumber = address.subThoroughfare ?: ""
                val shortStreet = shortenStreet(street)
                val fullAddress = buildString {
                    if (city.isNotEmpty()) append(city)
                    if (shortStreet.isNotEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(shortStreet)
                    }
                    if (houseNumber.isNotEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(houseNumber)
                    }
                }
                AddressResult(
                    fullAddress = fullAddress,
                    city = city,
                    postalCode = address.postalCode ?: "",
                    cityCode = getCityCode(city),
                    countryCode = address.countryCode ?: "RU"
                )
            }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Выбирает лучший адрес из списка.
     * Приоритет: адрес с улицей и номером дома.
     * Если такого нет, выбираем адрес с наибольшим количеством заполненных полей.
     */
    private fun selectBestAddress(addresses: MutableList<Address>?): Address? {
        if (addresses.isNullOrEmpty()) return null

        // Сначала ищем адрес с улицей и номером дома
        val withStreetAndHouse = addresses.find { address ->
            !address.thoroughfare.isNullOrEmpty() && !address.subThoroughfare.isNullOrEmpty()
        }
        if (withStreetAndHouse != null) return withStreetAndHouse

        // Иначе выбираем адрес с максимальным количеством заполненных полей
        return addresses.maxByOrNull { address ->
            listOf(
                address.thoroughfare,
                address.subThoroughfare,
                address.locality,
                address.subAdminArea,
                address.postalCode
            ).count { !it.isNullOrEmpty() }
        }
    }

    private fun shortenStreet(street: String): String {
        return street
            .replace(Regex("(?i)\\bулица\\b"), "ул.")
            .replace(Regex("(?i)\\bпроспект\\b"), "пр.")
            .replace(Regex("(?i)\\bпереулок\\b"), "пер.")
            .replace(Regex("(?i)\\bшоссе\\b"), "ш.")
    }

    // Справочник кодов городов России
    private fun getCityCode(city: String): String {
        val cityCodes = mapOf(
            "Подпорожье" to "81365",
            "Лодейное Поле" to "81364",
            "Москва" to "495",
            "Санкт-Петербург" to "812",
            "Новосибирск" to "383",
            "Екатеринбург" to "343",
            "Казань" to "843",
            "Нижний Новгород" to "831",
            "Челябинск" to "351",
            "Омск" to "381",
            "Самара" to "846",
            "Ростов-на-Дону" to "863",
            "Уфа" to "347",
            "Красноярск" to "391",
            "Воронеж" to "473",
            "Пермь" to "342",
            "Волгоград" to "844",
            "Краснодар" to "861",
            "Саратов" to "845",
            "Тюмень" to "345"
        )
        cityCodes.forEach { (key, code) ->
            if (city.contains(key, ignoreCase = true)) return code
        }
        return ""
    }

    data class AddressResult(
        val fullAddress: String,
        val city: String,
        val postalCode: String,
        val cityCode: String,
        val countryCode: String
    )
}