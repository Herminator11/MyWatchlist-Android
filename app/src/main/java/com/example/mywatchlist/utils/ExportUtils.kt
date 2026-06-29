package com.example.mywatchlist.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val YEAR_REGEX = Regex("""\d{4}""")

/**
 * Minimaler Datensatz für den History-Export – enthält nur die Felder,
 * die buildExportContent() tatsächlich braucht. Entkoppelt von MovieEntity,
 * damit Tests ohne Room/Android-Abhängigkeit laufen (TP-2).
 */
data class ExportEntry(
    val title: String,
    val seasonNumber: String = "",
    val finishedDate: String?,
    val date: String
)

/**
 * Baut den Textinhalt für den History-Export – reine Funktion ohne
 * Context/File-I/O.
 *
 * @param tvList Serien-Einträge (aus recentlyWatchedTv)
 * @param movieList Film-Einträge (aus recentlyWatchedMovies)
 * @param exportDate vorformatierter Zeitstempel (siehe currentExportTimestamp())
 *
 * TP-2: Extrahiert aus LibraryViewModel.buildExportFile(), damit die
 * Formatierungslogik unabhängig vom Android-Context per JVM-Unit-Test
 * geprüft werden kann.
 */
internal fun buildExportContent(
    tvList: List<ExportEntry>,
    movieList: List<ExportEntry>,
    exportDate: String
): String {
    val sb = StringBuilder()

    sb.appendLine("MyWatchlist – History Export")
    sb.appendLine("Exportiert am: $exportDate")
    sb.appendLine("═".repeat(40))

    // ── Serien ────────────────────────────────────────────
    if (tvList.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("SERIEN (${tvList.size})")
        sb.appendLine("─".repeat(40))

        val grouped = tvList.groupBy { entry ->
            val raw = entry.finishedDate ?: entry.date
            YEAR_REGEX.find(raw)?.value ?: "Unbekannt"
        }
        grouped.forEach { (year, entries) ->
            sb.appendLine()
            sb.appendLine("  $year")
            entries.forEach { entry ->
                val season = if (entry.seasonNumber.isNotBlank())
                    " – ${entry.seasonNumber}" else ""
                val date = entry.finishedDate
                    ?.replace("finished: ", "")
                    ?.replace("watched: ", "")
                    ?: ""
                val dateStr = if (date.isNotBlank()) "  [$date]" else ""
                sb.appendLine("  • ${entry.title}$season$dateStr")
            }
        }
    }

    // ── Filme ─────────────────────────────────────────────
    if (movieList.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("FILME (${movieList.size})")
        sb.appendLine("─".repeat(40))

        val grouped = movieList.groupBy { entry ->
            val raw = entry.finishedDate ?: entry.date
            YEAR_REGEX.find(raw)?.value ?: "Unbekannt"
        }
        grouped.forEach { (year, entries) ->
            sb.appendLine()
            sb.appendLine("  $year")
            entries.forEach { entry ->
                val date = entry.finishedDate
                    ?.replace("finished: ", "")
                    ?.replace("watched: ", "")
                    ?: ""
                val dateStr = if (date.isNotBlank()) "  [$date]" else ""
                sb.appendLine("  • ${entry.title}$dateStr")
            }
        }
    }

    return sb.toString()
}

/**
 * Ein benannter Abschnitt im flexiblen Listen-Export (z.B. "Want to Watch").
 */
data class ExportSection(val name: String, val entries: List<ExportEntry>)

/**
 * Generischer .txt-Export für beliebige Listen (Want to Watch, Favoriten …).
 * Reine Funktion ohne Context/File-I/O – die eigentliche Datei wird über SAF
 * geschrieben (siehe SettingsScreen). Jeder Abschnitt wird alphabetisch
 * sortiert, mit Jahr (aus dem Erscheinungsdatum), optionaler Staffel und
 * – falls vorhanden – persönlichem Seh-Datum aufgelistet.
 *
 * @param documentTitle Kopfzeile der Datei.
 * @param sections Liste der zu exportierenden Abschnitte (Name + Einträge).
 * @param exportDate vorformatierter Zeitstempel (siehe currentExportTimestamp()).
 */
internal fun buildListExportContent(
    documentTitle: String,
    sections: List<ExportSection>,
    exportDate: String
): String {
    val sb = StringBuilder()

    sb.appendLine(documentTitle)
    sb.appendLine("Exported: $exportDate")
    sb.appendLine("═".repeat(40))

    sections.forEach { section ->
        sb.appendLine()
        sb.appendLine("${section.name.uppercase()} (${section.entries.size})")
        sb.appendLine("─".repeat(40))

        if (section.entries.isEmpty()) {
            sb.appendLine("  (empty)")
        } else {
            section.entries
                .sortedBy { it.title.lowercase() }
                .forEach { entry ->
                    val year = YEAR_REGEX.find(entry.date)?.value?.let { " ($it)" } ?: ""
                    val season = if (entry.seasonNumber.isNotBlank())
                        " – ${entry.seasonNumber}" else ""
                    val watched = entry.finishedDate
                        ?.replace("finished: ", "")
                        ?.replace("watched: ", "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "  [$it]" } ?: ""
                    sb.appendLine("  • ${entry.title}$year$season$watched")
                }
        }
    }

    return sb.toString()
}

/**
 * Aktueller Zeitstempel für den Export-Header, Format dd.MM.yyyy HH:mm.
 */
internal fun currentExportTimestamp(): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())