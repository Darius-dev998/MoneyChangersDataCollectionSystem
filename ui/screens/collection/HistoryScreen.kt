package com.example.moneychangersdatacollectionsystem.ui.screens.collection

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.moneychangersdatacollectionsystem.domain.model.BusinessCategory
import com.example.moneychangersdatacollectionsystem.domain.model.CollectionRecord
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import com.example.moneychangersdatacollectionsystem.domain.model.shortAgentId
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lazyPagingItems = viewModel.collectionsPaged.collectAsLazyPagingItems()
    val isLoading by viewModel.isLoading.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()
    
    // Search Filter States
    val searchId by viewModel.searchId.collectAsState()
    val searchName by viewModel.searchName.collectAsState()
    val searchCounty by viewModel.searchCounty.collectAsState()
    val searchCategory by viewModel.searchCategory.collectAsState()
    val searchMarital by viewModel.searchMarital.collectAsState()
    val searchEdu by viewModel.searchEdu.collectAsState()
    val searchGender by viewModel.searchGender.collectAsState()
    val searchIdType by viewModel.searchIdType.collectAsState()
    val searchRole by viewModel.searchRole.collectAsState()
    val searchHasBank by viewModel.searchHasBank.collectAsState()
    val searchRemittance by viewModel.searchRemittance.collectAsState()
    val searchAddress by viewModel.searchAddress.collectAsState()

    var selectedRecord by remember { mutableStateOf<CollectionRecord?>(null) }
    var isSearchConsoleExpanded by remember { mutableStateOf(false) }

    val counties = listOf("Montserrado", "Nimba", "Lofa", "Grand Bassa", "Bong", "Margibi", "Grand Gedeh", "Sinoe", "River Cess", "Grand Cape Mount", "Bomi", "Maryland", "River Gee", "Gbarpolu", "Grand Kru")
    val categories = BusinessCategory.entries.map { it.displayName }
    val maritalStatuses = listOf("Single", "Married", "Divorced", "Widowed")
    val educationStatuses = listOf("None", "Primary (1-6)", "Junior High (7-9)", "Senior High (10-12)", "Vocational/Technical", "Associate Degree", "Bachelor's Degree", "Master's Degree", "Doctorate")
    val idTypes = listOf("NIR (National ID)", "Passport", "Driver's License", "Voter's Card", "Resident Permit")
    val genders = listOf("Male", "Female")
    val roles = listOf("Owner", "Operator")

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Portal", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchConsoleExpanded = !isSearchConsoleExpanded }) {
                        Icon(
                            imageVector = if (isSearchConsoleExpanded) Icons.Default.FilterListOff else Icons.Default.FilterList, 
                            contentDescription = "Toggle Search"
                        )
                    }
                    IconButton(onClick = { viewModel.exportToCSV() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                    IconButton(onClick = { viewModel.refreshFromRemote() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Advanced Search Dashboard
            AnimatedVisibility(
                visible = isSearchConsoleExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Filters Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchField(value = searchId, onValueChange = viewModel::onSearchIdChanged, label = "Record ID", modifier = Modifier.weight(1f))
                            SearchField(value = searchName, onValueChange = viewModel::onSearchNameChanged, label = "Business Name", modifier = Modifier.weight(1f))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchDropdown(label = "County", options = counties, selected = searchCounty, onSelected = viewModel::onSearchCountyChanged, modifier = Modifier.weight(1f))
                            SearchDropdown(label = "Category", options = categories, selected = searchCategory, onSelected = viewModel::onSearchCategoryChanged, modifier = Modifier.weight(1f))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchDropdown(label = "Marital", options = maritalStatuses, selected = searchMarital, onSelected = viewModel::onSearchMaritalChanged, modifier = Modifier.weight(1f))
                            SearchDropdown(label = "Education", options = educationStatuses, selected = searchEdu, onSelected = viewModel::onSearchEduChanged, modifier = Modifier.weight(1f))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchDropdown(label = "Gender", options = genders, selected = searchGender, onSelected = viewModel::onSearchGenderChanged, modifier = Modifier.weight(1f))
                            SearchDropdown(label = "ID Type", options = idTypes, selected = searchIdType, onSelected = viewModel::onSearchIdTypeChanged, modifier = Modifier.weight(1f))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchDropdown(label = "Role", options = roles, selected = searchRole, onSelected = viewModel::onSearchRoleChanged, modifier = Modifier.weight(1f))
                            SearchField(value = searchAddress, onValueChange = viewModel::onSearchAddressChanged, label = "Physical Address", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchYesNo(label = "Bank A/C?", selected = searchHasBank, onSelected = viewModel::onSearchHasBankChanged, modifier = Modifier.weight(1f))
                            SearchYesNo(label = "Remittance?", selected = searchRemittance, onSelected = viewModel::onSearchRemittanceChanged, modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.clearFilters() }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) {
                                Text("CLEAR ALL")
                            }
                            Button(
                                onClick = { 
                                    viewModel.executeSearch()
                                    isSearchConsoleExpanded = false 
                                }, 
                                modifier = Modifier.weight(1.5f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("EXECUTE SEARCH", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SCALABLE Results List with Paging - Guaranteed Compile Fix
            Box(modifier = Modifier.fillMaxSize()) {
                if (lazyPagingItems.itemCount == 0 && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp), 
                                tint = Color.Gray
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No matching records found", style = MaterialTheme.typography.headlineSmall, color = Color.Gray)
                            Text("Try adjusting your filters", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(count = lazyPagingItems.itemCount) { index ->
                            val record = lazyPagingItems[index]
                            record?.let {
                                HistoryItem(
                                    record = it, 
                                    onClick = { selectedRecord = it }
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 4.dp)
                    }
                }
            }
        }

        // Detail Dialog
        selectedRecord?.let { record ->
            RecordDetailDialog(
                record = record,
                onDismiss = { selectedRecord = null }
            )
        }
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = TextStyle(fontSize = 14.sp)) },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDropdown(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = TextStyle(fontSize = 14.sp)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("-- Clear --", color = Color.Red) }, onClick = { onSelected(""); expanded = false })
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelected(opt); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchYesNo(label: String, selected: Boolean?, onSelected: (Boolean?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = selected == true,
                onClick = { onSelected(if (selected == true) null else true) },
                label = { Text("Yes", fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selected == false,
                onClick = { onSelected(if (selected == false) null else false) },
                label = { Text("No", fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HistoryItem(record: CollectionRecord, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(record.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.businessName,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = record.id,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                
                Surface(
                    color = if (record.syncStatus == SyncStatus.SYNCED) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (record.syncStatus == SyncStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (record.syncStatus == SyncStatus.SYNCED) Color(0xFF43A047) else Color(0xFFE53935),
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp), 
                    tint = Color.Gray
                )
                Spacer(Modifier.width(8.dp))
                Text(text = record.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp), 
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = record.county, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Text(text = dateString, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RecordDetailDialog(record: CollectionRecord, onDismiss: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    val dateString = dateFormat.format(Date(record.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = record.businessName, fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "System ID: ${record.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailGroup("Identity & Origin") {
                    DetailRow("County of Origin", record.originCounty)
                    DetailRow("Gender", record.gender ?: "N/A")
                    DetailRow("Marital Status", record.maritalStatus)
                    DetailRow("Education", record.educationStatus)
                }

                DetailGroup("Business Profile") {
                    DetailRow("Category", record.category)
                    DetailRow("Role", record.ownerOperatorStatus)
                    DetailRow("Bank Account", if (record.hasBankAccount == true) "YES" else if (record.hasBankAccount == false) "NO" else "N/A")
                    DetailRow("Remittance", if (record.moneyRemittance == true) "YES" else if (record.moneyRemittance == false) "NO" else "N/A")
                }
                
                if (record.persons.isNotEmpty()) {
                    Text(text = "Individuals Profile (${record.persons.size})", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                    record.persons.forEach { person ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = person.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = "Contact: ${person.contact}", fontSize = 13.sp)
                                Text(text = "Role: ${person.ownerOperatorStatus}", fontSize = 13.sp)
                                Text(text = "Address: ${person.physicalAddress}", fontSize = 13.sp)
                            }
                        }
                    }
                }

                DetailGroup("Location & Logistics") {
                    DetailRow("Agent ID", record.shortAgentId)
                    DetailRow("Current County", record.county)
                    DetailRow("Physical Address", record.physicalAddress)
                    DetailRow("GPS", "%.4f, %.4f".format(record.latitude, record.longitude))
                    DetailRow("Captured On", dateString)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Text("CLOSE PORTAL", fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}

@Composable
fun DetailGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 4.dp))
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = value.ifBlank { "Not provided" }, fontSize = 14.sp, modifier = Modifier.weight(1.5f), color = Color.DarkGray)
    }
}
