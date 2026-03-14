package com.example.moneychangersdatacollectionsystem.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        // 1. Check if location is enabled at all
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.e("LocationHelper", "Location services are disabled")
            return@withContext null
        }

        // 2. Try Google Play Services first (Fused Location) with a solid 20s timeout
        val fusedLocation = try {
            withTimeoutOrNull(20000) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cts = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }.addOnFailureListener { e ->
                        Log.e("LocationHelper", "Fused location failed", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                    continuation.invokeOnCancellation {
                        cts.cancel()
                    }
                }
            }
        } catch (e: Exception) {
            null
        }

        if (fusedLocation != null) return@withContext fusedLocation

        // 3. Fallback to LocationManager (Works on 100% of Android devices including Huawei)
        return@withContext try {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            
            // Freshness check (2 minutes)
            if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 120000) {
                bestLocation
            } else {
                requestFreshLocation()
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "LocationManager fallback failed", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(location)
            }
            
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Try Network provider first for speed indoors, fallback to GPS
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> locationManager.getProviders(true).firstOrNull()
        }

        if (provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }

        continuation.invokeOnCancellation {
            locationManager.removeUpdates(listener)
        }
    }
}
