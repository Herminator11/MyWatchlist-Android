package com.example.mywatchlist

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

class MyWatchlistApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        // 20% des verfügbaren RAM – bei 4GB = ~80MB
                        // Reicht für ~400 Poster gleichzeitig im Speicher
                        .maxSizePercent(0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        // Persistenter Cache im App-Cache-Verzeichnis
                        // Bleibt über App-Neustarts erhalten, wird vom OS
                        // bei Speichermangel automatisch geleert
                        .directory(cacheDir.resolve("poster_cache"))
                        .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                        .build()
                }
                // Weicher Übergang beim Laden – verhindert Bildflackern
                .crossfade(true)
                .crossfade(300)
                // Netzwerk nur anfragen wenn kein Cache-Treffer
                .networkCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        )
    }
}