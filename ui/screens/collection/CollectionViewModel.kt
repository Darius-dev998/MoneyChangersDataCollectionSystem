package com.example.moneychangersdatacollectionsystem.ui.screens.collection

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.moneychangersdatacollectionsystem.data.worker.SyncWorker
import com.example.moneychangersdatacollectionsystem.domain.model.BusinessCategory
import com.example.moneychangersdatacollectionsystem.domain.model.CollectionRecord
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import com.example.moneychangersdatacollectionsystem.domain.repository.AuthRepository
import com.example.moneychangersdatacollectionsystem.domain.repository.CollectionRepository
import com.example.moneychangersdatacollectionsystem.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val authRepository: AuthRepository,
    private val locationHelper: LocationHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Advanced Search Console State - ALL 14 FIELDS
    private val _searchId = MutableStateFlow("")
    val searchId = _searchId.asStateFlow()
    private val _searchName = MutableStateFlow("")
    val searchName = _searchName.asStateFlow()
    private val _searchCounty = MutableStateFlow("")
    val searchCounty = _searchCounty.asStateFlow()
    private val _searchCategory = MutableStateFlow("")
    val searchCategory = _searchCategory.asStateFlow()
    private val _searchAgentId = MutableStateFlow("")
    val searchAgentId = _searchAgentId.asStateFlow()
    private val _searchMarital = MutableStateFlow("")
    val searchMarital = _searchMarital.asStateFlow()
    private val _searchEdu = MutableStateFlow("")
    val searchEdu = _searchEdu.asStateFlow()
    private val _searchOrigin = MutableStateFlow("")
    val searchOrigin = _searchOrigin.asStateFlow()
    private val _searchGender = MutableStateFlow("")
    val searchGender = _searchGender.asStateFlow()
    private val _searchIdType = MutableStateFlow("")
    val searchIdType = _searchIdType.asStateFlow()
    private val _searchRole = MutableStateFlow("")
    val searchRole = _searchRole.asStateFlow()
    private val _searchHasBank = MutableStateFlow<Boolean?>(null)
    val searchHasBank = _searchHasBank.asStateFlow()
    private val _searchRemittance = MutableStateFlow<Boolean?>(null)
    val searchRemittance = _searchRemittance.asStateFlow()
    private val _searchAddress = MutableStateFlow("")
    val searchAddress = _searchAddress.asStateFlow()

    private val _triggerSearch = MutableSharedFlow<Unit>(replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val collectionsPaged: Flow<PagingData<CollectionRecord>> = _triggerSearch
        .onStart { emit(Unit) }
        .flatMapLatest {
            collectionRepository.searchCollectionsPaged(
                id = _searchId.value,
                name = _searchName.value,
                county = _searchCounty.value,
                category = _searchCategory.value,
                agentId = _searchAgentId.value,
                marital = _searchMarital.value,
                edu = _searchEdu.value,
                origin = _searchOrigin.value,
                gender = _searchGender.value,
                idType = _searchIdType.value,
                role = _searchRole.value,
                hasBank = _searchHasBank.value,
                remittance = _searchRemittance.value,
                address = _searchAddress.value
            )
        }
        .cachedIn(viewModelScope)

    // Form Field State
    private val _county = MutableStateFlow("")
    val county: StateFlow<String> = _county.asStateFlow()
    private val _category = MutableStateFlow<BusinessCategory?>(null)
    val category: StateFlow<BusinessCategory?> = _category.asStateFlow()
    private val _umbrellaType = MutableStateFlow("")
    val umbrellaType: StateFlow<String> = _umbrellaType.asStateFlow()
    private val _otherUmbrellaType = MutableStateFlow("")
    val otherUmbrellaType: StateFlow<String> = _otherUmbrellaType.asStateFlow()
    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName.asStateFlow()
    private val _personCount = MutableStateFlow("")
    val personCount: StateFlow<String> = _personCount.asStateFlow()
    private val _persons = MutableStateFlow<List<PersonInput>>(emptyList())
    val persons: StateFlow<List<PersonInput>> = _persons.asStateFlow()
    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()
    private val _idType = MutableStateFlow("")
    val idType: StateFlow<String> = _idType.asStateFlow()
    private val _idNumber = MutableStateFlow("")
    val idNumber: StateFlow<String> = _idNumber.asStateFlow()
    private val _contactNumber = MutableStateFlow("")
    val contactNumber: StateFlow<String> = _contactNumber.asStateFlow()
    private val _originCounty = MutableStateFlow("")
    val originCounty: StateFlow<String> = _originCounty.asStateFlow()
    private val _moneyRemittance = MutableStateFlow<Boolean?>(null)
    val moneyRemittance: StateFlow<Boolean?> = _moneyRemittance.asStateFlow()
    private val _educationStatus = MutableStateFlow("")
    val educationStatus: StateFlow<String> = _educationStatus.asStateFlow()
    private val _emergencyContactName = MutableStateFlow("")
    val emergencyContactName: StateFlow<String> = _emergencyContactName.asStateFlow()
    private val _emergencyContactNumber = MutableStateFlow("")
    val emergencyContactNumber: StateFlow<String> = _emergencyContactNumber.asStateFlow()
    private val _hasBankAccount = MutableStateFlow<Boolean?>(null)
    val hasBankAccount: StateFlow<Boolean?> = _hasBankAccount.asStateFlow()
    private val _ownerOperatorStatus = MutableStateFlow("")
    val ownerOperatorStatus: StateFlow<String> = _ownerOperatorStatus.asStateFlow()
    private val _maritalStatus = MutableStateFlow("")
    val maritalStatus: StateFlow<String> = _maritalStatus.asStateFlow()
    private val _physicalAddress = MutableStateFlow("")
    val physicalAddress: StateFlow<String> = _physicalAddress.asStateFlow()
    private val _personName = MutableStateFlow("")
    val personName: StateFlow<String> = _personName.asStateFlow()
    private val _nationality = MutableStateFlow("")
    val nationality: StateFlow<String> = _nationality.asStateFlow()

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()
    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()
    private val _individualPhoto = MutableStateFlow<Uri?>(null)
    val individualPhoto: StateFlow<Uri?> = _individualPhoto.asStateFlow()
    private val _idCardPhoto = MutableStateFlow<Uri?>(null)
    val idCardPhoto: StateFlow<Uri?> = _idCardPhoto.asStateFlow()
    private val _currentPhotoToCrop = MutableStateFlow<Uri?>(null)
    val currentPhotoToCrop: StateFlow<Uri?> = _currentPhotoToCrop.asStateFlow()
    private val _navigateToCropper = MutableStateFlow(false)
    val navigateToCropper: StateFlow<Boolean> = _navigateToCropper.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    enum class PhotoType { INDIVIDUAL, ID_CARD }
    data class PersonInput(
        val name: String = "",
        val contact: String = "",
        val originCounty: String = "",
        val moneyRemittance: Boolean? = null,
        val educationStatus: String = "",
        val emergencyContactName: String = "",
        val emergencyContactNumber: String = "",
        val hasBankAccount: Boolean? = null,
        val ownerOperatorStatus: String = "",
        val maritalStatus: String = "",
        val physicalAddress: String = "",
        val nationality: String = "",
        val idType: String = "",
        val idNumber: String = ""
    )

    // Search Actions
    fun onSearchIdChanged(v: String) { _searchId.value = v }
    fun onSearchNameChanged(v: String) { _searchName.value = v }
    fun onSearchCountyChanged(v: String) { _searchCounty.value = v }
    fun onSearchCategoryChanged(v: String) { _searchCategory.value = v }
    fun onSearchAgentIdChanged(v: String) { _searchAgentId.value = v }
    fun onSearchMaritalChanged(v: String) { _searchMarital.value = v }
    fun onSearchEduChanged(v: String) { _searchEdu.value = v }
    fun onSearchOriginChanged(v: String) { _searchOrigin.value = v }
    fun onSearchGenderChanged(v: String) { _searchGender.value = v }
    fun onSearchIdTypeChanged(v: String) { _searchIdType.value = v }
    fun onSearchRoleChanged(v: String) { _searchRole.value = v }
    fun onSearchHasBankChanged(v: Boolean?) { _searchHasBank.value = v }
    fun onSearchRemittanceChanged(v: Boolean?) { _searchRemittance.value = v }
    fun onSearchAddressChanged(v: String) { _searchAddress.value = v }
    fun executeSearch() { viewModelScope.launch { _triggerSearch.emit(Unit) } }
    fun clearFilters() {
        _searchId.value = ""; _searchName.value = ""; _searchCounty.value = ""; _searchCategory.value = ""
        _searchAgentId.value = ""; _searchMarital.value = ""; _searchEdu.value = ""; _searchOrigin.value = ""
        _searchGender.value = ""; _searchIdType.value = ""; _searchRole.value = ""; _searchHasBank.value = null
        _searchRemittance.value = null; _searchAddress.value = ""; executeSearch()
    }

    // Form Handlers - Restored missing handlers
    fun onCountyChanged(value: String) { _county.value = value }
    fun onCategoryChanged(value: BusinessCategory) { _category.value = value; resetFormFields() }
    fun onUmbrellaTypeChanged(value: String) { _umbrellaType.value = value }
    fun onOtherUmbrellaTypeChanged(value: String) { _otherUmbrellaType.value = value }
    fun onBusinessNameChanged(value: String) { _businessName.value = value }
    fun onPersonCountChanged(value: String) { 
        _personCount.value = value 
        val count = value.toIntOrNull() ?: 0
        val currentList = _persons.value.toMutableList()
        if (count > currentList.size) repeat(count - currentList.size) { currentList.add(PersonInput()) }
        else if (count < currentList.size) { _persons.value = currentList.take(count); return }
        _persons.value = currentList
    }
    fun onPersonProfileChanged(index: Int, updated: PersonInput) {
        val currentList = _persons.value.toMutableList()
        if (index in currentList.indices) { currentList[index] = updated; _persons.value = currentList }
    }
    fun onGenderChanged(value: String) { _gender.value = value }
    fun onIdTypeChanged(value: String) { _idType.value = value }
    fun onIdNumberChanged(value: String) { _idNumber.value = value }
    fun onContactNumberChanged(value: String) { _contactNumber.value = value }
    fun onOriginCountyChanged(value: String) { _originCounty.value = value }
    fun onMoneyRemittanceChanged(value: Boolean?) { _moneyRemittance.value = value }
    fun onEducationStatusChanged(value: String) { _educationStatus.value = value }
    fun onEmergencyContactNameChanged(value: String) { _emergencyContactName.value = value }
    fun onEmergencyContactNumberChanged(value: String) { _emergencyContactNumber.value = value }
    fun onHasBankAccountChanged(value: Boolean?) { _hasBankAccount.value = value }
    fun onOwnerOperatorStatusChanged(value: String) { _ownerOperatorStatus.value = value }
    fun onMaritalStatusChanged(value: String) { _maritalStatus.value = value }
    fun onPhysicalAddressChanged(value: String) { _physicalAddress.value = value }
    fun onPersonNameChanged(value: String) { _personName.value = value }
    fun onNationalityChanged(value: String) { _nationality.value = value }

    fun onPhotoCaptured(uri: Uri, type: PhotoType) {
        _currentCaptureTypeState.value = type; _currentPhotoToCrop.value = uri; _navigateToCropper.value = true
    }
    private val _currentCaptureTypeState = MutableStateFlow<PhotoType?>(null)
    fun onPhotoCropped(uri: Uri) {
        when (_currentCaptureTypeState.value) { PhotoType.INDIVIDUAL -> _individualPhoto.value = uri; PhotoType.ID_CARD -> _idCardPhoto.value = uri; null -> {} }
        _currentPhotoToCrop.value = null; _navigateToCropper.value = false
    }
    fun resetCropperNavigation() { _navigateToCropper.value = false }
    fun refreshFromRemote() {
        viewModelScope.launch { _isLoading.value = true; try { withContext(Dispatchers.IO) { collectionRepository.syncFromRemote() } } catch (e: Exception) { _error.value = "Refresh Error: ${e.message}" } finally { _isLoading.value = false } }
    }
    fun collectGps() {
        viewModelScope.launch { _isLoading.value = true; _error.value = null; try { val location = withContext(Dispatchers.IO) { locationHelper.getCurrentLocation() }; if (location != null) { _latitude.value = location.latitude; _longitude.value = location.longitude } else { _error.value = "Failed GPS" } } catch (e: Exception) { _error.value = e.message } finally { _isLoading.value = false } }
    }
    private suspend fun getNextSerial(prefix: String, startAt: Int): Int = withContext(Dispatchers.IO) {
        val all = collectionRepository.getCollections().first()
        val existingSerials = all.filter { it.id.startsWith(prefix) }.mapNotNull { it.id.substringAfter("-").toIntOrNull() }
        val max = existingSerials.maxOrNull() ?: (startAt - 1); max + 1
    }
    fun saveCollection() {
        viewModelScope.launch {
            val isForex = _category.value == BusinessCategory.FOREX_BUREAU
            if (_county.value.isBlank() || (isForex && _businessName.value.isBlank()) || _category.value == null || _individualPhoto.value == null || _idCardPhoto.value == null) { _error.value = "Fields required"; return@launch }
            _isLoading.value = true; try {
                val agent = withContext(Dispatchers.IO) { authRepository.currentUser.first() } ?: throw Exception("Auth error")
                val categoryPrefix = _category.value?.code ?: "REC"; val nextBusinessSerial = getNextSerial(categoryPrefix, 20002); val recordId = "$categoryPrefix-$nextBusinessSerial"
                var nextPersonSerial = getNextSerial("PRS", 202601)
                val personDetails = _persons.value.map { input -> PersonDetail(id = "PRS-${nextPersonSerial++}", name = input.name, contact = input.contact, originCounty = input.originCounty, moneyRemittance = input.moneyRemittance, educationStatus = input.educationStatus, emergencyContactName = input.emergencyContactName, emergencyContactNumber = input.emergencyContactNumber, hasBankAccount = input.hasBankAccount, ownerOperatorStatus = input.ownerOperatorStatus, maritalStatus = input.maritalStatus, physicalAddress = input.physicalAddress, nationality = input.nationality, idType = input.idType, idNumber = input.idNumber) }
                val finalUmbrellaType = if (_category.value == BusinessCategory.UMBRELLA) { if (_umbrellaType.value == "Other") _otherUmbrellaType.value else _umbrellaType.value } else null
                val record = CollectionRecord(
                    id = recordId, 
                    agentId = agent.id, 
                    county = _county.value, 
                    category = _category.value!!.displayName, 
                    umbrellaType = finalUmbrellaType, 
                    businessName = if (isForex) _businessName.value else _category.value!!.displayName, 
                    personCount = if (isForex) _personCount.value.toIntOrNull() else null, 
                    gender = if (isForex) null else _gender.value, 
                    idType = if (isForex) "" else _idType.value, 
                    idNumber = if (isForex) "" else _idNumber.value, 
                    contactNumber = if (isForex) "" else _contactNumber.value, 
                    latitude = _latitude.value, 
                    longitude = _longitude.value, 
                    photoUrl = _individualPhoto.value.toString(), 
                    idCardPhotoUrl = _idCardPhoto.value.toString(), 
                    persons = personDetails, 
                    originCounty = if (!isForex) _originCounty.value else "", 
                    moneyRemittance = if (!isForex) _moneyRemittance.value else null, 
                    educationStatus = if (!isForex) _educationStatus.value else "", 
                    emergencyContactName = if (!isForex) _emergencyContactName.value else "", 
                    emergencyContactNumber = if (!isForex) _emergencyContactNumber.value else "", 
                    hasBankAccount = if (!isForex) _hasBankAccount.value else null, 
                    ownerOperatorStatus = if (!isForex) _ownerOperatorStatus.value else "", 
                    maritalStatus = if (!isForex) _maritalStatus.value else "", 
                    physicalAddress = if (!isForex) _physicalAddress.value else "",
                    personName = if (!isForex) _personName.value else "",
                    nationality = if (!isForex) _nationality.value else ""
                )
                withContext(Dispatchers.IO) { collectionRepository.saveCollection(record) }.onSuccess { _saveSuccess.value = true; resetForm() }.onFailure { _error.value = "Save Failed" }
            } catch (e: Exception) { _error.value = e.message } finally { _isLoading.value = false }
        }
    }
    fun exportToCSV() {
        viewModelScope.launch {
            _isLoading.value = true; try {
                val list = collectionRepository.getCollections().first(); if (list.isEmpty()) { _exportMessage.value = "Empty"; return@launch }
                val csvContent = StringBuilder(); csvContent.append("ID,Agent,County,Category,Name,Timestamp\n"); list.forEach { csvContent.append("${it.id},${it.agentId},${it.county},${it.category},${it.businessName},${it.timestamp}\n") }
                val fileName = "MCDCS_${System.currentTimeMillis()}.csv"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver; val vals = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); put(MediaStore.MediaColumns.MIME_TYPE, "text/csv"); put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, vals); uri?.let { resolver.openOutputStream(it)?.use { out -> out.write(csvContent.toString().toByteArray()) }; _exportMessage.value = "Exported" }
                } else { val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); val file = File(downloadsDir, fileName); file.writeText(csvContent.toString()); _exportMessage.value = "Exported" }
            } catch (e: Exception) { _error.value = "Export Failed" } finally { _isLoading.value = false }
        }
    }
    fun clearExportMessage() { _exportMessage.value = null }
    private fun triggerSync() { val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(); val req = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build(); WorkManager.getInstance(context).enqueue(req) }
    private fun resetFormFields() { _umbrellaType.value = ""; _otherUmbrellaType.value = ""; _businessName.value = ""; _personCount.value = ""; _persons.value = emptyList(); _gender.value = ""; _idType.value = ""; _idNumber.value = ""; _contactNumber.value = ""; _originCounty.value = ""; _moneyRemittance.value = null; _educationStatus.value = ""; _emergencyContactName.value = ""; _emergencyContactNumber.value = ""; _hasBankAccount.value = null; _ownerOperatorStatus.value = ""; _maritalStatus.value = ""; _physicalAddress.value = ""; _personName.value = ""; _nationality.value = ""; _error.value = null }
    private fun resetForm() { _county.value = ""; _category.value = null; resetFormFields(); _latitude.value = 0.0; _longitude.value = 0.0 }
    fun logout() { viewModelScope.launch { authRepository.logout() } }
}
