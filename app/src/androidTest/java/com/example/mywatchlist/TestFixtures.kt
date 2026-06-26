package com.example.mywatchlist

object TestFixtures {

    fun tvEntity(
        id: Int = 1,
        title: String = "Breaking Bad",
        listType: WatchListType = WatchListType.CURRENTLY_WATCHING,
        seasonNumber: String = "Staffel 1",
        isFavorite: Boolean = false,
        finishedDate: String? = null,
        notes: String? = null,
        favoriteCategory: String? = null
    ) = MovieEntity(
        id = id,
        title = title,
        date = "2008-01-20",
        posterPath = null,
        mediaType = "tv",
        listType = listType.name,
        seasonNumber = seasonNumber,
        isFavorite = isFavorite,
        finishedDate = finishedDate,
        notes = notes,
        favoriteCategory = favoriteCategory
    )

    fun movieEntity(
        id: Int = 100,
        title: String = "Inception",
        listType: WatchListType = WatchListType.RECENTLY_WATCHED_MOVIES,
        isFavorite: Boolean = false,
        finishedDate: String? = "watched: 01.06.2026",
        notes: String? = null
    ) = MovieEntity(
        id = id,
        title = title,
        date = "2010-07-16",
        posterPath = null,
        mediaType = "movie",
        listType = listType.name,
        seasonNumber = "",
        isFavorite = isFavorite,
        finishedDate = finishedDate,
        notes = notes
    )
}