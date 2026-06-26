package com.example.mywatchlist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MovieEntity::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies_table ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE movies_table ADD COLUMN notes TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS movies_table_new (
                        id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        date TEXT NOT NULL,
                        posterPath TEXT,
                        mediaType TEXT NOT NULL,
                        listType TEXT NOT NULL,
                        seasonNumber TEXT NOT NULL DEFAULT '',
                        finishedDate TEXT,
                        favoriteCategory TEXT,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        notes TEXT,
                        PRIMARY KEY (id, listType, seasonNumber)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO movies_table_new
                        (id, title, date, posterPath, mediaType, listType,
                         seasonNumber, finishedDate, favoriteCategory, isFavorite, notes)
                    SELECT
                        id, title, date, posterPath, mediaType, listType,
                        COALESCE(seasonNumber, ''), finishedDate, favoriteCategory,
                        isFavorite, notes
                    FROM movies_table
                """.trimIndent())
                db.execSQL("DROP TABLE movies_table")
                db.execSQL("ALTER TABLE movies_table_new RENAME TO movies_table")
            }
        }

        // Migration 4 → 5: sortOrder für Drag & Drop hinzufügen
        // Bestehende Favoriten bekommen aufsteigend 0,1,2,... als Initialreihenfolge
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Spalte hinzufügen mit Default 2147483647 (Int.MAX_VALUE)
                db.execSQL(
                    "ALTER TABLE movies_table ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 2147483647"
                )
                // Bestehenden Favoriten eine sinnvolle Startreihenfolge geben
                // (nach rowid = Einfüge-Reihenfolge)
                db.execSQL("""
                    UPDATE movies_table
                    SET sortOrder = (
                        SELECT COUNT(*)
                        FROM movies_table AS t2
                        WHERE t2.listType = movies_table.listType
                          AND t2.rowid < movies_table.rowid
                    )
                    WHERE listType IN ('FAVORITE_SERIES', 'FAVORITE_MOVIES')
                """.trimIndent())
            }
        }
        // 2. Migration 5 → 6 definieren (kein SQL nötig, nur Versions-Bump):
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Kein Schemaänderung – WANT_TO_WATCH ist nur ein neuer listType-String
            }
        }

        // Migration 6 → 7: DB-Indizes für häufig gefilterte Spalten
        // Macht Screen-Queries von O(n) Full Table Scan → O(log n) Index Lookup.
        // Kein Datenverlust, kein Schema-Umbau – nur neue Indizes anlegen.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_movies_table_listType " +
                            "ON movies_table (listType)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_movies_table_listType_mediaType " +
                            "ON movies_table (listType, mediaType)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_movies_table_listType_sortOrder " +
                            "ON movies_table (listType, sortOrder)"
                )
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "movie_tracker_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}