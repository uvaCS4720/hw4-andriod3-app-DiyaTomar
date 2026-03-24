package edu.nd.pmcburne.hello

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.nd.pmcburne.hello.data.CampusLocation
import edu.nd.pmcburne.hello.data.CampusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DefaultTag = "core"

data class MainUiState(
    val isLoading: Boolean = true,
    val selectedTag: String = DefaultTag,
    val availableTags: List<String> = listOf(DefaultTag),
    val locations: List<CampusLocation> = emptyList(),
    val errorMessage: String? = null
) {
    val filteredLocations: List<CampusLocation>
        get() = locations.filter { selectedTag in it.tags }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CampusRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshLocations()
    }

    fun onTagSelected(tag: String) {
        _uiState.update { currentState ->
            currentState.copy(selectedTag = tag)
        }
    }

    fun refreshLocations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val syncError = try {
                repository.synchronizeLocations()
                null
            } catch (exception: Exception) {
                Log.e("CampusMaps", "Failed to synchronize campus locations", exception)
                exception.localizedMessage ?: "Unable to update campus locations."
            }

            val storedLocations = repository.getStoredLocations()
            val tags = storedLocations
                .flatMap(CampusLocation::tags)
                .distinct()
                .sortedBy(String::lowercase)

            _uiState.update { currentState ->
                val nextSelectedTag = when {
                    currentState.selectedTag in tags -> currentState.selectedTag
                    DefaultTag in tags -> DefaultTag
                    tags.isNotEmpty() -> tags.first()
                    else -> DefaultTag
                }
                currentState.copy(
                    isLoading = false,
                    selectedTag = nextSelectedTag,
                    availableTags = if (tags.isEmpty()) listOf(DefaultTag) else tags,
                    locations = storedLocations,
                    errorMessage = syncError
                )
            }
        }
    }
}
