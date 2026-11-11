package com.example.mywatchlist

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mywatchlist.data.backup.BackupCodec
import com.example.mywatchlist.data.local.AppDatabase
import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Restore darf vorhandene Einträge nie verändern – geprüft gegen echtes Room (in-memory). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRestoreDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var vm: LibraryViewModel

    private val local = MovieEntity(1, "Severance", "2022", null, "tv", "CURRENTLY_WATCHING", "Season 2", notes = "my local note")
    private val other = MovieEntity(2, "Dune", "2021", null, "movie", "WANT_TO_WATCH")

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        runBlocking { db.movieDao().insertOrUpdateAll(listOf(local)) }
        vm = LibraryViewModel(app, db.movieDao())
    }

    @After fun tearDown() = db.close()

    @Test fun `restore adds missing entries and never overwrites existing ones`() = runBlocking {
        val backup = listOf(local.copy(notes = "old note from backup", title = "Old title"), other)
        assertEquals(1, vm.countMissingFromBackup(backup))
        assertEquals(1, vm.restoreMissingFromBackup(backup))

        val all = db.movieDao().getAllOnce().sortedBy { it.id }
        assertEquals(listOf(local, other), all)              // lokale Notiz/Titel unverändert
        assertEquals(0, vm.restoreMissingFromBackup(backup)) // zweiter Lauf: nichts mehr zu tun
    }

    @Test fun `backup json round-trips the whole database`() = runBlocking {
        db.movieDao().insertOrUpdateAll(listOf(other))
        val decoded = BackupCodec.decode(vm.buildBackupJson())
        assertEquals(db.movieDao().getAllOnce().toSet(), decoded.entries.toSet())
    }
}
