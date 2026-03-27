package com.example.moneychangersdatacollectionsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.moneychangersdatacollectionsystem.ui.navigation.NavGraph
import com.example.moneychangersdatacollectionsystem.ui.theme.MoneyChangersDataCollectionSystemTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — Clean entry point with zero business logic.
 *
 * FIXES APPLIED:
 *  #1  authRepository and collectionRepository injections REMOVED.
 *      Repositories must never be injected into Activities. The self-healing
 *      reset moved to MCDCSApplication.performSelfHealing() which runs on
 *      applicationScope and cannot be cancelled by Activity lifecycle events.
 *      Auth state is resolved internally by NavGraph via ViewModels.
 *
 *  #2  enableEdgeToEdge() moved to be the FIRST call in onCreate, before
 *      any window decoration is touched. Calling it after other window
 *      operations can produce inconsistent insets on some API levels.
 *
 *  #3  authRepository parameter removed from NavGraph call.
 *      Repositories must not be passed as parameters to Composables.
 *      NavGraph resolves auth state via hiltViewModel() internally.
 *
 *  #4  WindowCompat.setDecorFitsSystemWindows(false) added for proper
 *      edge-to-edge Compose insets handling on API 30+.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // FIX #2: enableEdgeToEdge FIRST — before any window decoration calls.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // FIX #4: Ensures Compose layout can draw behind system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MoneyChangersDataCollectionSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // FIX #3: No repository parameters — NavGraph uses hiltViewModel() internally.
                    NavGraph(navController = navController)
                }
            }
        }
    }
}