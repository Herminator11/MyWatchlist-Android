package com.example.mywatchlist.utils
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*


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