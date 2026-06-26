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

    // Erstabfrage (zeigt den großen Lade-Screen)
    var isLoading by mutableStateOf(false)
        private set

    // Nachladen weiterer Seiten (zeigt nur den Fußzeilen-Spinner)
    var isLoadingMore by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var currentQuery = ""
    private var currentPage = 0
    private var totalPages = 1

    /** Es gibt noch weitere Ergebnisseiten zum Nachladen. */
    val canLoadMore: Boolean
        get() = searchResults.isNotEmpty() && currentPage in 1 until totalPages

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            currentQuery = ""
            currentPage = 0
            totalPages = 1
            return
        }
        currentQuery = query
        currentPage = 0
        totalPages = 1
        loadPage(reset = true)
    }

    /** Nächste Seite anhängen (für Infinite-Scroll). */
    fun loadNextPage() {
        if (isLoading || isLoadingMore) return
        if (currentQuery.isBlank() || currentPage >= totalPages) return
        loadPage(reset = false)
    }

    private fun loadPage(reset: Boolean) {
        viewModelScope.launch {
            if (reset) isLoading = true else isLoadingMore = true
            errorMessage = null
            try {
                val response = MovieApi.service.searchMulti(
                    query = currentQuery,
                    page = currentPage + 1
                )
                currentPage = response.page
                totalPages = response.totalPages

                val filtered = response.results.filter {
                    it.mediaType != "person" &&
                            (it.posterPath != null || it.displayTitle != "Unknown title")
                }
                searchResults = if (reset) {
                    filtered
                } else {
                    // Anhängen + Duplikate vermeiden (gleicher key wie in der UI)
                    (searchResults + filtered).distinctBy { it.id.toString() + (it.mediaType ?: "") }
                }
            } catch (e: Exception) {
                errorMessage = "Fehler bei der Suche: ${e.localizedMessage}"
            } finally {
                if (reset) isLoading = false else isLoadingMore = false
            }
        }
    }
}
