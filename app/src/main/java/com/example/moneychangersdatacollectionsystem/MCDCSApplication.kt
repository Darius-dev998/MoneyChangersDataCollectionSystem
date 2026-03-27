package com.example.moneychangersdatacollectionsystem

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MCDCSApplication — Application entry point.
 *
 * FIXES APPLIED:
 *  #1  applicationScope added.
 *      Provides a coroutine scope that lives for the entire process lifetime
 *      and cannot be cancelled by any individual Activity lifecycle event.
 *      All infrastructure-level async work (self-healing, initialization)
 *      must run here, not in MainActivity.lifecycleScope.
 *
 *  #2  Self-healing resetSyncStatus moved HERE from MainActivity.
 *      In MainActivity it ran on lifecycleScope, which is cancelled on any
 *      config change (rotation, keyboard open). If the device rotates during
 *      first launch, records stuck in SYNCING are never reset.
 *      Running here on applicationScope guarantees it always completes.
 *
 *  #3  ProcessLifecycleOwner observer registered for clean process shutdown.
 *      Provides a real onStop callback that fires when the entire app
 *      backgrounds — unlike onTerminate() which never fires on real devices.
 */
@HiltAndroidApp
class MCDCSApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "MCDCSApplication"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * FIX #1: Application-scoped coroutine scope.
     * SupervisorJob: failure in one child does not cancel the scope.
     * CoroutineExceptionHandler: logs unhandled exceptions without crashing.
     */
    val applicationScope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.Main +
                CoroutineExceptionHandler { _, throwable ->
                    Log.e(TAG, "Unhandled exception in applicationScope", throwable)
                }
    )

    /**
     * FIX #2: CollectionRepository injected here for self-healing.
     * Runs on applicationScope (immune to Activity lifecycle cancellation).
     */
    @Inject
    lateinit var collectionRepository: com.example.moneychangersdatacollectionsystem.domain.repository.CollectionRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (com.example.moneychangersdatacollectionsystem.BuildConfig.DEBUG)
                    android.util.Log.DEBUG
                else
                    android.util.Log.ERROR
            )
            .build()

    override fun onCreate() {
        super.onCreate()

        // FIX #2: Self-healing runs at application start on applicationScope.
        // Cannot be cancelled by Activity recreation or config changes.
        performSelfHealing()

        // FIX #3: Register process lifecycle observer for clean shutdown.
        registerProcessLifecycleObserver()
    }

    private fun performSelfHealing() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                collectionRepository.resetSyncStatus(
                    com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus.SYNCING,
                    com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus.PENDING
                )
                Log.d(TAG, "Self-healing: SYNCING records reset to PENDING.")
            } catch (e: Exception) {
                Log.e(TAG, "Self-healing failed: ${e.message}", e)
            }
        }
    }

    private fun registerProcessLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    Log.d(TAG, "App backgrounded.")
                }
            }
        )
    }
}