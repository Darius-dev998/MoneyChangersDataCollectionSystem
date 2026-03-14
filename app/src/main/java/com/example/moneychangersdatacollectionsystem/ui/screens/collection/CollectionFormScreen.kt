package com.example.moneychangersdatacollectionsystem.ui.screens.collection

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneychangersdatacollectionsystem.domain.model.BusinessCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionFormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSuccess: () -> Unit = {},
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // State collection from ViewModel
    val businessName by viewModel.businessName.collectAsState()
    val category by viewModel.category.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onNavigateToSuccess()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Collection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { viewModel.onBusinessNameChanged(it) },
                        label = { Text("Business Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Text("Business Category", fontWeight = FontWeight.Bold)
                    // You can add back the FlowRow for categories here
                }

                item {
                    GPSDataSurface(
                        latitude = latitude,
                        longitude = longitude,
                        onUpdate = { viewModel.collectGps() }
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            Button(
                onClick = { viewModel.saveCollection() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Text("SUBMIT DATA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun YesNoButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.primary
        ),
        border = if (!selected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

@Composable
fun GPSDataSurface(latitude: Double, longitude: Double, onUpdate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("GPS Data", fontWeight = FontWeight.Bold)
            Text("Lat: ${if (latitude == 0.0) "Not set" else "%.6f".format(latitude)}", fontSize = 12.sp)
            Text("Lon: ${if (longitude == 0.0) "Not set" else "%.6f".format(longitude)}", fontSize = 12.sp)
            Button(onClick = onUpdate, modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                Text("UPDATE GPS COORDINATES")
            }
        }
    }
}
