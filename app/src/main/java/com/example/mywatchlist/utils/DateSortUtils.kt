package com.example.mywatchlist.utils

private val SORT_DATE_REGEX = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")

/**
 * Wandelt ein Anzeigedatum (z.B. "watched: 01.06.2026", "finished: 14.06.2026")
 * in ein lexikografisch sortierbares Format (yyyy-MM-dd) um.
 *
 * @param finishedDate bevorzugtes Anzeigedatum, ggf. mit "watched: "/"finished: "-Präfix
 * @param date Fallback-Datum im TMDB-Format (yyyy-MM-dd), falls finishedDate null ist
 *
 * Fällt auf den Rohwert zurück, wenn kein dd.MM.yyyy-Muster gefunden wird
 * (z.B. wenn date bereits im Format yyyy-MM-dd vorliegt – dieses Format
 * ist bereits lexikografisch korrekt sortierbar).git
 *
 * TP-3: Extrahiert aus LibraryViewModel.parseSortDate(), damit reine
 * JVM-Unit-Tests ohne Emulator möglich sind (kein MovieEntity/Room nötig).
 */
internal fun parseSortDate(finishedDate: String?, date: String): String {
    val raw = finishedDate ?: date
    return SORT_DATE_REGEX.find(raw)
        ?.destructured?.let { (d, m, y) -> "$y-$m-$d" } ?: raw
}