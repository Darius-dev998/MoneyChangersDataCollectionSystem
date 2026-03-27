package com.example.moneychangersdatacollectionsystem.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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

/**
 * LocationHelper — GPS acquisition with Play Services primary + LocationManager fallback.
 *
 * FIX: requestFreshLocation() wrapped in withTimeoutOrNull(30s).
 *
 * The original had no timeout on the fallback LocationListener. If the device
 * is indoors with no GPS satellite visibility and no network location, the
 * listener is registered permanently and never removed. The coroutine hangs
 * forever with no result, and CollectionViewModel.collectGps() never completes
 * — the loading spinner spins until the user force-kills the app.
 *
 * Fix: 30-second timeout. The listener is always removed via invokeOnCancellation,
 * so no leak occurs whether the timeout fires or the coroutine is cancelled normally.
 *
 * Timeout hierarchy:
 *   — Fused Location (Play Services): 20s (unchanged — the primary, fastest path)
 *   — Last Known Location: instant (no timeout needed)
 *   — Fresh LocationManager request: 30s (new timeout — was unbounded)
 *   — Total worst case: ~50s before returning null
 *
 * NOTE: android.os.Bundle import removed — it was required by the deprecated
 * onStatusChanged() override which uses Bundle, but Kotlin resolves it from
 * the LocationListener default method chain. No explicit import needed.
 */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocationHelper"
        private const val FUSED_TIMEOUT_MS = 20_000L
        private const val FRESH_LOCATION_TIMEOUT_MS = 30_000L
        private const val LAST_KNOWN_MAX_AGE_MS = 120_000L  // 2 minutes
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {

        // 1. Verify at least one provider is enabled
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.e(TAG, "All location providers disabled — cannot acquire GPS fix")
            return@withContext null
        }

        // 2. Try Fused Location (Play Services) — best accuracy, fastest on real devices
        val fusedLocation = try {
            withTimeoutOrNull(FUSED_TIMEOUT_MS) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cts = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Fused location failed: ${e.message}")
                        if (continuation.isActive) continuation.resume(null)
                    }
                    continuation.invokeOnCancellation { cts.cancel() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fused location exception: ${e.message}")
            null
        }

        if (fusedLocation != null) {
            Log.d(TAG, "Fused location acquired: ${fusedLocation.latitude}, ${fusedLocation.longitude} (±${fusedLocation.accuracy}m)")
            return@withContext fusedLocation
        }

        // 3. Fallback: check last known location across all enabled providers
        return@withContext try {
            val providers = locationManager.getProviders(true)
            var bestLastKnown: Location? = null

            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLastKnown == null || l.accuracy < bestLastKnown.accuracy) {
                    bestLastKnown = l
                }
            }

            // Accept last-known if it's fresh enough (< 2 minutes)
            val ageMs = System.currentTimeMillis() - (bestLastKnown?.time ?: 0L)
            if (bestLastKnown != null && ageMs < LAST_KNOWN_MAX_AGE_MS) {
                Log.d(TAG, "Using last-known location (${ageMs / 1000}s old): ${bestLastKnown.latitude}, ${bestLastKnown.longitude}")
                bestLastKnown
            } else {
                Log.d(TAG, "Last-known location stale or unavailable — requesting fresh fix")
                requestFreshLocation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "LocationManager fallback failed: ${e.message}")
            null
        }
    }

    /**
     * Requests a fresh location fix from LocationManager.
     *
     * FIX: Wrapped in withTimeoutOrNull(30s).
     *
     * The original had no timeout — if no GPS fix was available (indoors,
     * no network location), the LocationListener was registered permanently
     * and the coroutine hung forever. invokeOnCancellation removes the
     * listener whether the timeout fires or the caller cancels.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? {
        // FIX: 30s timeout — listener is ALWAYS removed via invokeOnCancellation
        return withTimeoutOrNull(FRESH_LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) {
                            Log.d(TAG, "Fresh LocationManager fix: ${location.latitude}, ${location.longitude}")
                            continuation.resume(location)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                        LocationManager.NETWORK_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                        LocationManager.GPS_PROVIDER
                    else -> locationManager.getProviders(true).firstOrNull()
                }

                if (provider == null) {
                    Log.e(TAG, "No location provider available for fresh request")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                try {
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
                    Log.d(TAG, "Waiting for fresh fix on provider: $provider (${FRESH_LOCATION_TIMEOUT_MS / 1000}s timeout)")
                } catch (e: Exception) {
                    Log.e(TAG, "requestLocationUpdates failed: ${e.message}")
                    if (continuation.isActive) continuation.resume(null)
                }

                // Always remove the listener — fires on timeout or explicit cancellation
                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                    Log.d(TAG, "LocationListener removed (timeout or cancellation)")
                }
            }
        }.also { result ->
            if (result == null) {
                Log.w(TAG, "Fresh location request timed out after ${FRESH_LOCATION_TIMEOUT_MS / 1000}s — returning null")
            }
        }
    }
}