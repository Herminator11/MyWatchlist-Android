package com.example.mywatchlist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Nur für Tests: Erlaubt das Injizieren eines Test-DAOs in den LibraryViewModel.
 * In der App selbst wird LibraryViewModel weiterhin über ViewModelProvider
 * ohne Factory instanziiert – der Default-Parameter greift automatisch.
 */
class LibraryViewModelFactory(
    private val application: Application,
    private val movieDao: MovieDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LibraryViewModel(application, movieDao) as T
    }
}