package com.example.mywatchlist.utils

import com.example.mywatchlist.MovieEntity
import com.example.mywatchlist.TmdbMovie

fun MovieEntity.toTmdbMovie() = TmdbMovie(
    id = id,
    title = title,
    name = title,
    releaseDate = date,
    firstAirDate = date,
    posterPath = posterPath,
    mediaType = mediaType,
    overview = ""
)