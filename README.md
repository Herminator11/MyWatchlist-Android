# MyWatchlist

An Android app to keep track of films and series: want to watch, currently watching, favourites and recently watched. Titles and posters come from [TMDB](https://www.themoviedb.org/); everything is stored on the device. Personal project.

This product uses the TMDB API but is not endorsed or certified by TMDB.

## Legal notice

This is a **personal, non-commercial** project and is not published in any app store. Film and series titles, posters and other images shown by the app or in screenshots belong to their respective rights holders (studios, distributors, artists), not to me and not to TMDB. They are loaded from TMDB at runtime and are not part of this repository. If you are a rights holder and want something changed or removed, please open an issue or write to hermann11.ad@gmail.com.

## Stack

Kotlin, Jetpack Compose, Room, Retrofit.

## Build

1. Get a free API key from TMDB.
2. Add it to `local.properties` in the project root (this file is not committed):

   ```properties
   TMDB_API_KEY=your_key_here
   ```

3. Open the project in Android Studio and run the `app` configuration.

## Licence

MIT, see [LICENSE](LICENSE).
