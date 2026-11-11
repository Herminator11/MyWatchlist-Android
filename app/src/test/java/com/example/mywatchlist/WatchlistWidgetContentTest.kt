package com.example.mywatchlist

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import com.example.mywatchlist.data.AppearancePrefs
import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.widget.WidgetContent
import com.example.mywatchlist.widget.WidgetList
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WatchlistWidgetContentTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val entries = listOf(
        MovieEntity(1, "Severance", "2022-02-18", null, "tv", "CURRENTLY_WATCHING", "Season 2"),
        MovieEntity(2, "The Bear", "2022-06-23", null, "tv", "CURRENTLY_WATCHING", "Season 3"),
    )

    @Test fun `medium widget lists entries with count and header`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 110.dp))
        provideComposable { WidgetContent(context, WidgetList.WATCHING, entries, AppearancePrefs()) }
        onNode(hasText("CURRENTLY WATCHING")).assertExists()
        onNode(hasTextEqualTo("2")).assertExists()
        onNode(hasText("Severance · Season 2")).assertExists()
        onNode(hasText("The Bear · Season 3")).assertExists()
    }

    @Test fun `small widget shows only the next title`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(110.dp, 110.dp))
        provideComposable { WidgetContent(context, WidgetList.WATCHING, entries, AppearancePrefs()) }
        onNode(hasText("Severance · Season 2")).assertExists()
        onNode(hasText("The Bear · Season 3")).assertDoesNotExist()
    }

    @Test fun `empty list shows a hint`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 260.dp))
        provideComposable { WidgetContent(context, WidgetList.FAVORITES, emptyList(), AppearancePrefs()) }
        onNode(hasText("Nothing here yet")).assertExists()
        onNode(hasText("FAVORITES")).assertExists()
    }
}
