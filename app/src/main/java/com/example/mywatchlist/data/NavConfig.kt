package com.example.mywatchlist.data

// ─────────────────────────────────────────────────────────────
// NAVIGATIONS-KONFIGURATION · Reihenfolge, ausgeblendete Tabs, Start-Tab
//
// Reine Kotlin-Logik (kein Android) → per Unit-Test prüfbar.
// Gespeichert wird als CSV in DataStore; beim Lesen wird IMMER
// normalisiert, damit kaputte/alte Werte nie zu einer leeren oder
// unbedienbaren Navigation führen können.
// ─────────────────────────────────────────────────────────────
object NavRoutes {
    const val WANT_TO_WATCH = "want_to_watch"
    const val CURRENTLY_WATCHING = "currently_watching"
    const val RECENTLY_WATCHED = "recently_watched"
    const val FAVORITES = "favorites"
    const val SEARCH = "search"

    /** Bisherige, fest eingebaute Reihenfolge. */
    val DEFAULT_ORDER = listOf(WANT_TO_WATCH, CURRENTLY_WATCHING, RECENTLY_WATCHED, FAVORITES, SEARCH)

    /** Die Suche bleibt immer sichtbar – sonst kann man nichts mehr hinzufügen. */
    val ALWAYS_VISIBLE = setOf(SEARCH)

    const val MIN_VISIBLE = 2
}

data class NavConfig(
    val order: List<String>,
    val hidden: Set<String>,
    val start: String,
) {
    /** Sichtbare Tabs in Nutzer-Reihenfolge. */
    val visible: List<String> get() = order.filter { it !in hidden }

    fun orderCsv(): String = order.joinToString(",")
    fun hiddenCsv(): String = hidden.sorted().joinToString(",")

    fun move(from: Int, to: Int): NavConfig {
        if (from !in order.indices || to !in order.indices) return this
        val list = order.toMutableList().apply { add(to, removeAt(from)) }
        return normalize(list, hidden, start)
    }

    fun setHidden(route: String, hide: Boolean): NavConfig =
        normalize(order, if (hide) hidden + route else hidden - route, start)

    fun withStart(route: String): NavConfig = normalize(order, hidden, route)

    /** Darf dieser Tab gerade ausgeblendet werden? */
    fun canHide(route: String): Boolean =
        route !in NavRoutes.ALWAYS_VISIBLE && (route in hidden || visible.size > NavRoutes.MIN_VISIBLE)

    companion object {
        val Default = NavConfig(
            order = NavRoutes.DEFAULT_ORDER,
            hidden = emptySet(),
            start = NavRoutes.CURRENTLY_WATCHING, // bisheriger Start-Screen
        )

        fun parse(orderCsv: String?, hiddenCsv: String?, start: String?): NavConfig =
            normalize(
                order = orderCsv.splitCsv(),
                hidden = hiddenCsv.splitCsv().toSet(),
                start = start ?: Default.start,
            )

        fun normalize(order: List<String>, hidden: Set<String>, start: String): NavConfig {
            val known = NavRoutes.DEFAULT_ORDER
            // 1. nur bekannte Routen, keine Duplikate, fehlende hinten anhängen
            val cleanOrder = order.filter { it in known }.distinct()
                .let { it + known.filter { r -> r !in it } }
            // 2. nur bekannte, ausblendbare Tabs; Mindestanzahl sichtbar halten
            var cleanHidden = hidden.filter { it in known && it !in NavRoutes.ALWAYS_VISIBLE }.toSet()
            if (known.size - cleanHidden.size < NavRoutes.MIN_VISIBLE) cleanHidden = emptySet()
            // 3. Start-Tab muss sichtbar sein, sonst erster sichtbarer
            val visible = cleanOrder.filter { it !in cleanHidden }
            val cleanStart = if (start in visible) start else visible.first()
            return NavConfig(cleanOrder, cleanHidden, cleanStart)
        }

        private fun String?.splitCsv(): List<String> =
            this?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
}
