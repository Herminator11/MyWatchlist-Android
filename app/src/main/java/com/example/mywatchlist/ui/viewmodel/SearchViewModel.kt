package com.example.mywatchlist.ui.viewmodel
import com.example.mywatchlist.data.remote.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    var searchResults by mutableStateOf<List<TmdbMovie>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val response = MovieApi.service.searchMulti(query = query)
                searchResults = response.results.filter {
                    it.mediaType != "person" && (it.posterPath != null || it.displayTitle != "Unbekannter Titel")
                }
            } catch (e: Exception) {
                errorMessage = "Fehler bei der Suche: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}