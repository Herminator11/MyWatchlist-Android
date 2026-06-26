package com.example.mywatchlist

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "movies_table",
    primaryKeys = ["id", "listType", "seasonNumber"],
    indices = [
        // Jeder Screen filtert nach listType – dieser Index macht das O(log n) statt O(n)
        Index(value = ["listType"]),
        // WantToWatch-Tabs filtern nach listType + mediaType kombiniert
        Index(value = ["listType", "mediaType"]),
        // Favoriten-Screens filtern nach listType + sortOrder
        Index(value = ["listType", "sortOrder"])
    ]
)
data class MovieEntity(
    val id: Int,
    val title: String,
    val date: String,
    val posterPath: String?,
    val mediaType: String,
    val listType: String,

    // Teil des Primary Keys – darf NICHT nullable sein.
    val seasonNumber: String = "",

    val finishedDate: String? = null,
    val favoriteCategory: String? = null,
    val isFavorite: Boolean = false,
    val notes: String? = null,

    // NEU: Reihenfolge für Drag & Drop im FavoritesScreen
    // Niedrigerer Wert = weiter oben. Default Int.MAX_VALUE = ans Ende anhängen
    val sortOrder: Int = Int.MAX_VALUE
)