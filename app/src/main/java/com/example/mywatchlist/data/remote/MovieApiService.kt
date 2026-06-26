package com.example.mywatchlist.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
const val TMDB_API_KEY = "REDACTED"

// ── Detail-Antworten (für das Detail-Popup) ───────────────────
// search/multi liefert KEINE Staffel-/Folgenzahl – dafür braucht
// es die dedizierten Detail-Endpunkte /tv/{id} bzw. /movie/{id}.
data class TvDetails(
    @SerializedName("number_of_seasons")
    val numberOfSeasons: Int?,

    @SerializedName("number_of_episodes")
    val numberOfEpisodes: Int?,

    @SerializedName("first_air_date")
    val firstAirDate: String?,

    val status: String?
)

data class MovieDetails(
    val runtime: Int?, // Laufzeit in Minuten

    @SerializedName("release_date")
    val releaseDate: String?,

    val status: String?
)

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
        @Query("api_key") apiKey: String = TMDB_API_KEY,
        @Query("language") language: String = "de-DE"
    ): MovieSearchResponse

    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String = TMDB_API_KEY,
        @Query("language") language: String = "de-DE"
    ): TvDetails

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String = TMDB_API_KEY,
        @Query("language") language: String = "de-DE"
    ): MovieDetails
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