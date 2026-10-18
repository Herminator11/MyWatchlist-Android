package com.example.mywatchlist.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName
import com.example.mywatchlist.BuildConfig

const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
// Key kommt aus local.properties → BuildConfig (nicht im Repo).
val TMDB_API_KEY: String = BuildConfig.TMDB_API_KEY

// ── Detail-Antworten (für das Detail-Popup) ───────────────────
// search/multi liefert KEINE Staffel-/Folgenzahl – dafür braucht
// es die dedizierten Detail-Endpunkte /tv/{id} bzw. /movie/{id}.
data class Genre(
    val id: Int,
    val name: String
)

data class SeasonInfo(
    @SerializedName("season_number")
    val seasonNumber: Int,

    @SerializedName("episode_count")
    val episodeCount: Int?,

    val name: String?
)

data class CastMember(
    val name: String?,
    val character: String?,
    val order: Int?
)

data class CrewMember(
    val name: String?,
    val job: String?
)

// Kommt per append_to_response=credits im selben Request.
data class Credits(
    val cast: List<CastMember>?,
    val crew: List<CrewMember>?
)

data class CreatedBy(
    val name: String?
)

data class TvDetails(
    @SerializedName("number_of_seasons")
    val numberOfSeasons: Int?,

    @SerializedName("number_of_episodes")
    val numberOfEpisodes: Int?,

    @SerializedName("first_air_date")
    val firstAirDate: String?,

    val status: String?,

    val genres: List<Genre>? = null,

    @SerializedName("original_name")
    val originalName: String? = null,

    val seasons: List<SeasonInfo>? = null,

    @SerializedName("created_by")
    val createdBy: List<CreatedBy>? = null,

    val credits: Credits? = null
)

data class MovieDetails(
    val runtime: Int?, // Laufzeit in Minuten

    @SerializedName("release_date")
    val releaseDate: String?,

    val status: String?,

    val genres: List<Genre>? = null,

    @SerializedName("original_title")
    val originalTitle: String? = null,

    val credits: Credits? = null
)

data class MovieSearchResponse(
    val results: List<TmdbMovie>,
    val page: Int = 1,
    @SerializedName("total_pages")
    val totalPages: Int = 1
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
        get() = title ?: name ?: "Unknown title"

    val displayDate: String
        get() = releaseDate ?: firstAirDate ?: ""

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
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String = TMDB_API_KEY,
        @Query("language") language: String = "en-US"
    ): MovieSearchResponse

    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String = TMDB_API_KEY,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "credits"
    ): TvDetails

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String = TMDB_API_KEY,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "credits"
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