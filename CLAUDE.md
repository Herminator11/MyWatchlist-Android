<frontend_aesthetics>
You tend to converge toward generic, "on distribution" outputs. In frontend design, this creates what users call the "AI slop" aesthetic. Avoid this: make creative, distinctive frontends that surprise and delight. Focus on:

Typography: Choose fonts that are beautiful, unique, and interesting. Avoid generic fonts like Arial and Inter; opt instead for distinctive choices that elevate the frontend's aesthetics.

Color & Theme: Commit to a cohesive aesthetic. Use CSS variables for consistency. Dominant colors with sharp accents outperform timid, evenly-distributed palettes. Draw from IDE themes and cultural aesthetics for inspiration.

Motion: Use animations for effects and micro-interactions. Prioritize CSS-only solutions for HTML. Use Motion library for React when available. Focus on high-impact moments: one well-orchestrated page load with staggered reveals (animation-delay) creates more delight than scattered micro-interactions.

Backgrounds: Create atmosphere and depth rather than defaulting to solid colors. Layer CSS gradients, use geometric patterns, or add contextual effects that match the overall aesthetic.

Avoid generic AI-generated aesthetics:
- Overused font families (Inter, Roboto, Arial, system fonts)
- Clichéd color schemes (particularly purple gradients on white backgrounds)
- Predictable layouts and component patterns
- Cookie-cutter design that lacks context-specific character

Interpret creatively and make unexpected choices that feel genuinely designed for the context. Vary between light and dark themes, different fonts, different aesthetics. You still tend to converge on common choices (Space Grotesk, for example) across generations. Avoid this: it is critical that you think outside the box!
</frontend_aesthetics>

## Datensicherheit (höchste Priorität)

Die installierte Original-App (`com.example.mywatchlist`) enthält echte Nutzerdaten und darf in keiner Weise verändert werden.

- Auf einem angeschlossenen Gerät **nur** den Build-Typ `v2` verwenden („MyWatchList 2“, Paket `com.example.mywatchlist.v2`, eigene DB/DataStore): `./gradlew installV2`, `./gradlew connectedV2AndroidTest`.
- **Niemals** auf einem echten Gerät: `installDebug`, `installRelease`, `connectedDebugAndroidTest`, „Run“ der Debug-Variante aus Android Studio, `adb uninstall com.example.mywatchlist`, `adb shell pm clear com.example.mywatchlist`, Schreibzugriffe per `run-as com.example.mywatchlist`.
- Vor jeder Gerätesitzung zuerst ein Backup, das nur liest (TXT-Export in der App, DB-Kopie per `adb exec-out run-as … cat`).
- Room: `fallbackToDestructiveMigration(false)` bleibt. Jede Schemaänderung braucht eine echte `Migration` und einen Migrationstest.

## Android-UI-Regeln

**Stack:** 1 Modul, Kotlin 2.3, AGP 9.1, Jetpack Compose + Material 3, Navigation Compose, Coil 2, Haze, `sh.calvin.reorderable`, DataStore, Room. minSdk 24 / targetSdk 36. Kein Compose Multiplatform.

**Grundprinzip:** Komponenten mit komplexem Verhalten (Toast-Stapel, Bottom Sheet mit Drag, Dropdown, Tooltip, Skeleton-Loading, animierte Zahlen) nicht von Grund auf selbst bauen. Zuerst Material 3 oder eine der unten genannten Bibliotheken verwenden. Vor jeder neuen Abhängigkeit Wartungsstand und Kompatibilität prüfen (Maven Central / Google Maven, nie Versionen raten).

**1. Foundations**
- **Material 3** (`androidx.compose.material3`) als Basis: `MaterialTheme` mit eigenem `ColorScheme`, `Typography`, `Shapes`. Material 3 Expressive (`MaterialExpressiveTheme`, `MotionScheme`) nur nach Versionsprüfung, mit `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`; maßgeblich ist die offizielle Doku auf developer.android.com.
- **Compose Unstyled** (`com.composables:composeunstyled`): renderlose, barrierefreie Bausteine, wenn Material 3 Aussehen oder Verhalten zu stark vorgibt.
- **Composables UI** (`com.composables:ui`): gestylte Komponenten auf Compose-Unstyled-Basis (frühe Version, mit Vorsicht). Bei Copy-and-Paste-Übernahme nach `ui/components/`.
- **Kein Standard-Look:** Das Material-Baseline-Theme (Violett, Roboto, Standard-Rundungen) ist tabu. Dynamic Color ist bewusst aus. Die App hat eine feste Markenpalette (siehe Tokens).

**2. Specialists (eine Bibliothek pro Aufgabe)**
- Toasts: **compose-sonner** (`io.github.dokar3:sonner`) statt `Toast.makeText`. Undo-Aktionen bleiben bei der Material-`Snackbar`.
- Bottom Sheet mit Drag: Material 3 `ModalBottomSheet` (oder Compose Unstyled).
- Command-Menü: kein Pendant; ggf. Material 3 `SearchBar`, nur mit Tastatur- und TalkBack-Support.
- Animierte Zahlen: `AnimatedContent` pro Ziffer (kleine Libs wie compose-rolling-text nur nach Wartungscheck).
- Skeletons: `com.revenuecat.purchases:placeholder` oder ein eigener Shimmer, der auf Theme-Tokens basiert.
- Blur/Glas: **Haze** (`dev.chrisbanes.haze`), nur gezielt einsetzen (Frame-Zeit), alle Haze-Module auf derselben Version.

**3. Effekte (sparsam):** Höchstens **ein** großer Eye-Catcher pro Screen (z. B. die Haze-Glas-Navigation oder eine Shape-Morph-Animation). Spring-Motion statt gemischter Tween/Spring-Stile.

**Installation:** Abhängigkeiten über `gradle/libs.versions.toml`, keine harten Versionen in `build.gradle.kts`.

**Design-Tokens des Projekts („Cinema Noir / Gold“, Quelle: `ui/theme/AppTheme.kt`):**
- Farben (nur dunkel): Grund `#0B0B0D`, Fläche `#161519`, Fläche erhöht `#242228`, Akzent Bernsteingold `#E9B949`, Gold hell `#FFD874`, Ember `#7A4A12`, Text `#F4ECDD` / `#A89F8E` / `#4A4640`, Hairline `#E9B949` @ 20 %. Hintergrund als vertikaler Charcoal-Gradient (`cinemaBackground()`).
- Schriften: Display **Bebas Neue** (Titel), Body **Barlow** (Fließtext, app-weiter Default), Mono **Space Mono** (Daten, Labels).
- Shapes: Karten/Sheets 12–16 dp, Nav-Kapsel 32 dp, Chips 20 dp.
- Motion: Spring (MediumBouncy/Low) für Icons, 250 ms Tween für Farbwechsel.
- Farben und Schriften immer über `AppTheme`/`MaterialTheme` beziehen, nie neue `Color(0xFF…)`-Literale in Screens.
