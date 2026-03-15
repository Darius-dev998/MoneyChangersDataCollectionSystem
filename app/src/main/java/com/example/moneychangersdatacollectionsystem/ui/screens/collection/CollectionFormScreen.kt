package com.example.moneychangersdatacollectionsystem.ui.screens.collection

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.moneychangersdatacollectionsystem.domain.model.BusinessCategory
import com.example.moneychangersdatacollectionsystem.ui.components.FinTechBackground
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val county by viewModel.county.collectAsState()
    val category by viewModel.category.collectAsState()
    val umbrellaType by viewModel.umbrellaType.collectAsState()
    val otherUmbrellaType by viewModel.otherUmbrellaType.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val personCount by viewModel.personCount.collectAsState()
    val persons by viewModel.persons.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val idType by viewModel.idType.collectAsState()
    val idNumber by viewModel.idNumber.collectAsState()
    val contactNumber by viewModel.contactNumber.collectAsState()

    // Global Profile Fields
    val personName by viewModel.personName.collectAsState()
    val nationality by viewModel.nationality.collectAsState()
    val originCounty by viewModel.originCounty.collectAsState()
    val moneyRemittance by viewModel.moneyRemittance.collectAsState()
    val educationStatus by viewModel.educationStatus.collectAsState()
    val emergencyContactName by viewModel.emergencyContactName.collectAsState()
    val emergencyContactNumber by viewModel.emergencyContactNumber.collectAsState()
    val emergencyContactAddress by viewModel.emergencyContactAddress.collectAsState()
    val hasBankAccount by viewModel.hasBankAccount.collectAsState()
    val ownerOperatorStatus by viewModel.ownerOperatorStatus.collectAsState()
    val maritalStatus by viewModel.maritalStatus.collectAsState()
    val physicalAddress by viewModel.physicalAddress.collectAsState()

    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val individualPhoto by viewModel.individualPhoto.collectAsState()
    val idCardPhoto by viewModel.idCardPhoto.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    val counties = listOf("Montserrado", "Nimba", "Lofa", "Grand Bassa", "Bong", "Margibi", "Grand Gedeh", "Sinoe", "River Cess", "Grand Cape Mount", "Bomi", "Maryland", "River Gee", "Gbarpolu", "Grand Kru")
    val idTypes = listOf("NIR (National ID)", "Passport", "Driver's License", "Voter's Card", "Resident Permit")
    val genders = listOf("Male", "Female")
    val umbrellaTypes = listOf("Lone Star \"MTN\"", "Orange", "Other")
    val maritalStatuses = listOf("Single", "Married", "Divorced", "Widowed")
    val educationStatuses = listOf("None", "Primary", "Junior High", "Senior High", "Vocational/Technical", "Associate Degree", "Bachelor's Degree", "Master's Degree", "Doctorate")
    val ownerOperatorOptions = listOf("Owner", "Operator")

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var currentTarget by remember { mutableStateOf<PhotoTarget?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null && currentTarget != null) {
            viewModel.onPhotoCaptured(tempPhotoUri!!, currentTarget!!)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val file = File(context.filesDir, "Pictures").apply { if (!exists()) mkdirs() }
            val photoFile = File(file, "IMG_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.collectGps()
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Record saved and syncing...", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    FinTechBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Data Collection", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Section 1: Location & Category
                item {
                    FormSectionCard(title = "Location & Category", icon = Icons.Default.Settings) {
                        DropdownSelector(label = "Current County", options = counties, selectedOption = county, onOptionSelected = { viewModel.onCountyChanged(it) })
                        Spacer(modifier = Modifier.height(12.dp))
                        DropdownSelector(label = "Business Category", options = BusinessCategory.entries.map { it.displayName }, selectedOption = category?.displayName ?: "", onOptionSelected = { name ->
                            BusinessCategory.entries.find { it.displayName == name }?.let { viewModel.onCategoryChanged(it) }
                        })
                        AnimatedVisibility(visible = category == BusinessCategory.UMBRELLA) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                DropdownSelector(label = "Select Umbrella Type", options = umbrellaTypes, selectedOption = umbrellaType, onOptionSelected = { viewModel.onUmbrellaTypeChanged(it) })
                                AnimatedVisibility(visible = umbrellaType == "Other") {
                                    Column {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(value = otherUmbrellaType, onValueChange = { viewModel.onOtherUmbrellaTypeChanged(it) }, label = { Text("Specify Other Umbrella Type", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }

                if (category != null) {
                    val isForex = category == BusinessCategory.FOREX_BUREAU

                    if (isForex) {
                        item {
                            FormSectionCard(title = "Forex Bureau Details", icon = Icons.Default.Business) {
                                OutlinedTextField(value = businessName, onValueChange = { viewModel.onBusinessNameChanged(it) }, label = { Text("Bureau Name", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = personCount, onValueChange = { viewModel.onPersonCountChanged(it) }, label = { Text("Number of Persons Inside", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                        }

                        itemsIndexed(persons, key = { index, _ -> "person_$index" }) { index, person ->
                            PersonProfileCard(
                                index = index,
                                person = person,
                                counties = counties,
                                idTypes = idTypes,
                                countries = viewModel.countries,
                                educationStatuses = educationStatuses,
                                maritalStatuses = maritalStatuses,
                                ownerOperatorOptions = ownerOperatorOptions,
                                onUpdate = { updated -> viewModel.onPersonProfileChanged(index, updated) },
                                onCaptureIdCard = {
                                    currentTarget = PhotoTarget.PersonIdCard(index)
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                    } else {
                        // Profile Information for non-Forex
                        item {
                            FormSectionCard(title = "Person Profile", icon = Icons.Default.Person) {
                                OutlinedTextField(value = personName, onValueChange = { viewModel.onPersonNameChanged(it) }, label = { Text("Full Name", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                                DropdownSelector(label = "Nationality", options = viewModel.countries, selectedOption = nationality, onOptionSelected = { viewModel.onNationalityChanged(it) })
                                Spacer(modifier = Modifier.height(12.dp))
                                DropdownSelector(label = "Marital Status", options = maritalStatuses, selectedOption = maritalStatus, onOptionSelected = { viewModel.onMaritalStatusChanged(it) })
                                Spacer(modifier = Modifier.height(12.dp))
                                DropdownSelector(label = "Educational Status", options = educationStatuses, selectedOption = educationStatus, onOptionSelected = { viewModel.onEducationStatusChanged(it) })
                                Spacer(modifier = Modifier.height(12.dp))
                                DropdownSelector(label = "County of Origin", options = counties, selectedOption = originCounty, onOptionSelected = { viewModel.onOriginCountyChanged(it) })
                                Spacer(modifier = Modifier.height(12.dp))
                                DropdownSelector(label = "Gender", options = genders, selectedOption = gender, onOptionSelected = { viewModel.onGenderChanged(it) })
                            }
                        }

                        item {
                            FormSectionCard(title = "Business & Role", icon = Icons.Default.Work) {
                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { viewModel.onBusinessNameChanged(it) },
                                    label = { Text("Business Name", style = TextStyle(fontSize = 18.sp)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DropdownSelector(label = "Role", options = ownerOperatorOptions, selectedOption = ownerOperatorStatus, onOptionSelected = { viewModel.onOwnerOperatorStatusChanged(it) })
                                Spacer(modifier = Modifier.height(16.dp))
                                YesNoSelector(label = "Money Remittance Available?", selected = moneyRemittance, onSelected = { viewModel.onMoneyRemittanceChanged(it) })
                                Spacer(modifier = Modifier.height(16.dp))
                                YesNoSelector(label = "Has Active Bank Account?", selected = hasBankAccount, onSelected = { viewModel.onHasBankAccountChanged(it) })
                            }
                        }

                        item {
                            FormSectionCard(title = "Emergency Contact", icon = Icons.Default.ContactEmergency) {
                                OutlinedTextField(value = emergencyContactName, onValueChange = { viewModel.onEmergencyContactNameChanged(it) }, label = { Text("Full Name", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = emergencyContactNumber, onValueChange = { viewModel.onEmergencyContactNumberChanged(it) }, label = { Text("Contact Number", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = emergencyContactAddress, onValueChange = { viewModel.onEmergencyContactAddressChanged(it) }, label = { Text("Physical Address", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
                            }
                        }

                        item {
                            FormSectionCard(title = "Identity Verification", icon = Icons.Default.Badge) {
                                DropdownSelector(label = "ID Type", options = idTypes, selectedOption = idType, onOptionSelected = { viewModel.onIdTypeChanged(it) })
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = idNumber, onValueChange = { viewModel.onIdNumberChanged(it) }, label = { Text("ID Number", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                                PhotoCaptureSlot(label = "ID Card Photo", photoUri = idCardPhoto, onCaptureClick = {
                                    currentTarget = PhotoTarget.GlobalIdCard
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                })
                            }
                        }
                    }

                    item {
                        FormSectionCard(title = "Location & Final Photo", icon = Icons.Default.LocationOn) {
                            OutlinedTextField(value = physicalAddress, onValueChange = { viewModel.onPhysicalAddressChanged(it) }, label = { Text("Actual Physical Address", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = contactNumber, onValueChange = { viewModel.onContactNumberChanged(it) }, label = { Text("Direct Contact Number", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            Spacer(modifier = Modifier.height(12.dp))
                            PhotoCaptureSlot(label = getCategoryPhotoLabel(category), photoUri = individualPhoto, onCaptureClick = {
                                currentTarget = PhotoTarget.GlobalIndividual
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            })
                            Spacer(modifier = Modifier.height(16.dp))
                            GPSDataSurface(latitude = latitude, longitude = longitude) { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
                        }
                    }

                    item {
                        if (error != null) {
                            Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp))
                        }

                        Button(onClick = { viewModel.saveCollection() }, modifier = Modifier.fillMaxWidth().height(64.dp), enabled = !isLoading && county.isNotBlank(), shape = MaterialTheme.shapes.large) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("FINALIZE COLLECTION", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonProfileCard(
    index: Int,
    person: CollectionViewModel.PersonInput,
    counties: List<String>,
    idTypes: List<String>,
    countries: List<String>,
    educationStatuses: List<String>,
    maritalStatuses: List<String>,
    ownerOperatorOptions: List<String>,
    onUpdate: (CollectionViewModel.PersonInput) -> Unit,
    onCaptureIdCard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Person ${index + 1} Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(value = person.name, onValueChange = { onUpdate(person.copy(name = it)) }, label = { Text("Full Name", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(label = "Nationality", options = countries, selectedOption = person.nationality, onOptionSelected = { onUpdate(person.copy(nationality = it)) })

            Spacer(modifier = Modifier.height(20.dp))
            Text("Identity Verification", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            DropdownSelector(label = "ID Type", options = idTypes, selectedOption = person.idType, onOptionSelected = { onUpdate(person.copy(idType = it)) })
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = person.idNumber, onValueChange = { onUpdate(person.copy(idNumber = it)) }, label = { Text("ID Number", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            PhotoCaptureSlot(label = "Capture ID Card Image", photoUri = person.idCardPhoto, onCaptureClick = onCaptureIdCard)

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(value = person.contact, onValueChange = { onUpdate(person.copy(contact = it)) }, label = { Text("Contact Number", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(label = "Marital Status", options = maritalStatuses, selectedOption = person.maritalStatus, onOptionSelected = { onUpdate(person.copy(maritalStatus = it)) })
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(label = "Educational Status", options = educationStatuses, selectedOption = person.educationStatus, onOptionSelected = { onUpdate(person.copy(educationStatus = it)) })
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(label = "County of Origin", options = counties, selectedOption = person.originCounty, onOptionSelected = { onUpdate(person.copy(originCounty = it)) })
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(label = "Role", options = ownerOperatorOptions, selectedOption = person.ownerOperatorStatus, onOptionSelected = { onUpdate(person.copy(ownerOperatorStatus = it)) })
            Spacer(modifier = Modifier.height(16.dp))
            YesNoSelector(label = "Money Remittance Available?", selected = person.moneyRemittance, onSelected = { onUpdate(person.copy(moneyRemittance = it)) })
            Spacer(modifier = Modifier.height(16.dp))
            YesNoSelector(label = "Has Active Bank Account?", selected = person.hasBankAccount, onSelected = { onUpdate(person.copy(hasBankAccount = it)) })
            Spacer(modifier = Modifier.height(16.dp))
            Text("Emergency Contact", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(value = person.emergencyContactName, onValueChange = { onUpdate(person.copy(emergencyContactName = it)) }, label = { Text("Name", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = person.emergencyContactNumber, onValueChange = { onUpdate(person.copy(emergencyContactNumber = it)) }, label = { Text("Phone", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = person.emergencyContactAddress, onValueChange = { onUpdate(person.copy(emergencyContactAddress = it)) }, label = { Text("Physical Address", style = TextStyle(fontSize = 18.sp)) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun YesNoSelector(label: String, selected: Boolean?, onSelected: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            YesNoButton(label = "YES", selected = selected == true, onClick = { onSelected(true) }, modifier = Modifier.weight(1f))
            YesNoButton(label = "NO", selected = selected == false, onClick = { onSelected(false) }, modifier = Modifier.weight(1f))
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
    val latText = if (latitude == 0.0) "Not set" else "%.6f".format(latitude)
    val lonText = if (longitude == 0.0) "Not set" else "%.6f".format(longitude)
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("GPS Data", fontWeight = FontWeight.Bold)
            Text("Lat: $latText", fontSize = 12.sp)
            Text("Lon: $lonText", fontSize = 12.sp)
            Button(onClick = onUpdate, modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) { Text("UPDATE GPS COORDINATES") }
        }
    }
}

private fun getCategoryPhotoLabel(category: BusinessCategory?): String {
    return when (category) {
        BusinessCategory.FOREX_BUREAU -> "Bureau Photo"
        BusinessCategory.STORE_SHOP -> "Store/Shop Photo"
        BusinessCategory.UMBRELLA -> "Umbrella Photo"
        BusinessCategory.WITHOUT_UMBRELLA -> "Business Site Photo"
        BusinessCategory.KIOSK_BOOTH -> "Kiosk/Booth Photo"
        else -> "Owner/Site Photo"
    }
}

@Composable
fun FormSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun PhotoCaptureSlot(label: String, photoUri: Uri?, onCaptureClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (photoUri != null) {
            Card(modifier = Modifier.fillMaxWidth().height(180.dp), shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Image(painter = rememberAsyncImagePainter(photoUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        val btnText = if (photoUri == null) "CAPTURE IMAGE" else "RETAKE IMAGE"
        OutlinedButton(onClick = onCaptureClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(btnText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val filteredOptions = options.filter { it.contains(searchText, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { isExpanded ->
            expanded = isExpanded
            if (!isExpanded) {
                searchText = ""
            }
        }
    ) {
        OutlinedTextField(
            value = if (expanded) searchText else selectedOption,
            onValueChange = {
                searchText = it
                expanded = true
            },
            label = { Text(label, style = TextStyle(fontSize = 18.sp)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )

        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onOptionSelected(selectionOption)
                            searchText = ""
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
