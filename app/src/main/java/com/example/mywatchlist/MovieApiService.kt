package com.example.mywatchlist

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

data class MovieSearchResponse(
    val results: List<TmdbMovie>
)

data class TmdbMovie(
    val id: Int,

    @SerializedName("media_type")
    val mediaType: String?,

    // --- Titel-Felder ---
    val title: String?, // Für Filme
    val name: String?,  // Für Serien

    // --- Datum-Felder ---
    @SerializedName("release_date")
    val releaseDate: String?, // Für Filme

    @SerializedName("first_air_date")
    val firstAirDate: String?, // Für Serien

    @SerializedName("poster_path")
    val posterPath: String?,

    val overview: String?
) {
    val displayTitle: String
        get() = title ?: name ?: "Unbekannter Titel"

    val displayDate: String
        get() = releaseDate ?: firstAirDate ?: "Kein Datum"

    val displayMediaType: String
        get() = when (mediaType) {
            "tv" -> "TV Series"
            "movie" -> "Movie"
            else -> "Media"
        }

    val fullPosterPath: String?
        get() = posterPath?.let { "$TMDB_IMAGE_BASE_URL$it" }
}


interface MovieApiService {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("api_key") apiKey: String = "REDACTED",
        @Query("language") language: String = "de-DE"
    ): MovieSearchResponse
}

object MovieApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: MovieApiService by lazy {
        retrofit.create(MovieApiService::class.java)
    }
}