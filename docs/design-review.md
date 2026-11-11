# Design Review – MyWatchlist (Stand `main` @ 90b06ce, 2026-11-11)

Grundlage: Code-Analyse aller Screens und Komponenten unter `app/src/main/java/com/example/mywatchlist/ui/`. Es gab keine Screenshots, weil `@Preview`s und Screenshot-Tooling fehlen; Roborazzi kommt in Phase 5 dazu. Als Prüfraster dienten die Regeln der Skills `compose-agent` und `jetpack-compose-audit` (unter `.claude/skills/`) sowie die 9 Punkte aus dem Overhaul-Prompt.

**Gesamteindruck:** Die App hat bereits eine starke, eigenständige Identität: Cinema Noir/Gold, Bebas Neue + Barlow + Space Mono, Glas-Navigation mit Haze, ein orchestrierter Header-Auftritt. Ein Standard-Look ist **kein** Problem. Die Schwächen liegen woanders: Tokens laufen am `MaterialTheme` vorbei, die Accessibility bei großer Schrift und bei Touch-Zielen ist lückenhaft, Motion ist uneinheitlich, und mehrere Bausteine sind dupliziert.

---

## 1. Selbstgebaute oder zweckentfremdete Komponenten

| Ort | Befund | Empfehlung |
|---|---|---|
| `ui/screens/SettingsScreen.kt:61,63,71` | `Toast.makeText` für Export-Feedback, im System-Look und nicht im Theme. | **compose-sonner** (`io.github.dokar3:sonner` 0.4.0) mit Theme-Farben, oder die vorhandene App-Snackbar. |
| `CurrentlyWatchingScreen.kt:119`, `WantToWatchScreen.kt:711`, `AddToListSheet.kt:305`, `GlobalEditSheet.kt:344` | Vier fast identische `DatePickerDialog`-Kopien, jedes Mal mit eigenen `DatePickerDefaults.colors(...)`. | Eine gemeinsame `CinemaDatePickerDialog` in `ui/components/common/`. |
| `UniversalMediaCard.kt:185–219`, `SearchScreen.kt:~480`, `WantToWatchScreen.kt:~515` | Handgebaute Aktions-Buttons (`Box` + `clip` + `background` + `clickable`) ohne Rolle, Ripple oder Disabled-Zustand. | Material 3 `FilledIconButton` mit Cinema-Farben (Rolle, 48-dp-Ziel und Ripple inklusive). |
| `MainScreen.kt` `FloatingBottomNavigation` | Eigene Nav-Leiste ist **legitim** (das ist der Eye-Catcher), aber ohne Tab-Semantik (`selected`, `Role.Tab`) und mit Tooltip als einzigem Label. | Behalten, aber `Modifier.selectable(selected, role = Role.Tab)` nutzen, optional Labels (Setting). |
| `MovieTrackerTopBar` (`MainScreen.kt:85`) | Settings-Icon ist ein `Icon` mit `.clickable` auf 26 dp. | `IconButton` (48 dp). |
| `ScreenHeader.kt` | Eigene Eintritts-Animation mit drei getrennten `animateFloatAsState`. | Okay als Signatur-Moment. Auf `MotionTokens` umstellen und reduzierte Animation respektieren. |

Gut gelöst: `ModalBottomSheet` (Material 3) für die Sheets, Material-`Snackbar` mit Undo, Coil für Bilder.

## 2. Standard-Look-Check
- Theme ist **kein** Baseline-Material: eigene Palette, drei gebündelte Fonts, Dynamic Color bewusst aus. ✔
- `Typography` in `ui/theme/Type.kt` wird kaum genutzt. Screens setzen 88-mal `fontSize = …sp` direkt. Die Typo-Hierarchie existiert nur implizit.
- Shapes sind nicht zentral definiert (`RoundedCornerShape(16/12/6/20/32.dp)` verstreut), es gibt kein `Shapes`-Objekt.
- Elevation ist überall 0 oder 12 dp (Nav). Das ist konsistent, aber Tiefe entsteht nur über Flächenfarben. Für Noir ist das passend.

## 3. Theme-Konsistenz
- **`AppTheme` ist ein statisches `object`.** 280 Verweise, 0 Nutzungen von `MaterialTheme.colorScheme/typography/shapes`. Folge: keine Laufzeit-Themes, also auch keine Akzentfarben-Einstellung (geplantes Feature), und Material-Komponenten müssen jedes Mal manuell eingefärbt werden.
- 23× `Color.Black`/`Color.White` als „auf Akzent“-Farbe. Gemeint ist `onPrimary` (= `DarkBg`).
- Keine `Color(0xFF…)`-Literale außerhalb von `ui/theme`. ✔
- Hartcodierter String: `UniversalMediaCard.kt:193` `"Mark as finished"` (englisch, nicht in `strings.xml`).

## 4. Accessibility
- **Feste Kartenhöhe `110.dp`** (`UniversalMediaCard.kt:65`, `SearchScreen.kt:428`): Bei Schriftgröße 1.5 werden Titel und Metazeile abgeschnitten. → `heightIn(min = 110.dp)`.
- **Zu kleine Schrift:** 9 sp (Typ-Tag, Favoriten-Badge, Header-Subline) und 10 sp an vier Stellen. Bei dunklem Grund und Sekundärtext (`#A89F8E`) liegt das unter der empfohlenen Mindestlesbarkeit. → mindestens 11 sp, Labels über einen Typo-Token.
- **Touch-Ziele:** Settings-Icon 26 dp; Nav-Items etwa 52×40 dp (Höhe < 48); Aktions-Buttons in Karten 38 dp. → 48 dp über `minimumInteractiveComponentSize()`/`IconButton`.
- Kontrast: `TextTertiary #4A4640` auf `#161519` liegt bei etwa 1.9:1. Wird teils für Inhalte genutzt (Chevrons, Trennpunkte); bei Info-Text ist das zu schwach.
- `contentDescription = null` 13× – meist dekorativ und korrekt; die Poster in Karten haben den Titel als Beschreibung, das ist doppelt mit dem Titeltext (TalkBack liest den Titel zweimal). → Poster `null`, Karte per `semantics(mergeDescendants = true)`.
- Long-Press ist die einzige Bearbeiten-Geste. Ohne `onLongClickLabel` erfährt TalkBack nichts davon.

## 5. Systemintegration
- **Edge-to-Edge:** Kein `enableEdgeToEdge()` in `MainActivity`. Ab Android 15 (targetSdk 36) wird es erzwungen, darunter nicht → inkonsistente Systemleisten. → `enableEdgeToEdge()` mit dunklen, transparenten Bars.
- **Predictive Back:** `android:enableOnBackInvokedCallback` fehlt im Manifest.
- Dark-only ist eine bewusste Markenentscheidung → **bleibt** (kein Light Theme im Overhaul; als spätere Option gelistet).
- Adaptive Layouts: Listen sind auf Telefonbreite ausgelegt; auf Tablet und im Querformat werden Karten nur gestreckt. Niedrige Priorität.
- `.blur()` hinter dem Detail-Dialog wirkt erst ab Android 12. Darunter gibt es keinen Ersatz-Scrim (minSdk 24).

## 6. Motion
- Mischung aus `tween(250/420/480/560)` mit unterschiedlichen Easings und `spring(MediumBouncy, Low)`. Es gibt keine zentralen Motion-Tokens.
- **Reduzierte Animation** (Systemeinstellung „Animationen entfernen“) wird nirgends berücksichtigt.
- Listen-Einträge erscheinen und verschwinden ohne `Modifier.animateItem()`. Löschen und Verschieben springen hart.
- Positiv: Header-Auftritt, Nav-Bounce und animierter Blur sind gut gemachte Einzelmomente. Ziel: **ein** einheitliches Spring-Schema.

## 7. Zustände
- Leer-Zustände sind auf allen Listen-Screens nur ein zentrierter Grautext, ohne Icon, Hinweis oder Aktion (z. B. „Zur Suche“).
- Suche hat Loading/Error/Empty. ✔ Die Fehlermeldung wird aber im ViewModel auf Deutsch zusammengebaut (`SearchViewModel.kt:78`) und nicht lokalisiert. Das ist UI-Text in der Logikschicht und wird hier nur notiert.
- Poster ohne Lade-Platzhalter (Coil-Crossfade ins Leere). Kein Fehler-Bild.
- Pressed-Zustände: Nav ✔ (Scale), Karten nur Ripple, Aktions-Buttons gar keine.

## 8. Compose-Qualität mit UI-Bezug
- `CurrentlyWatchingScreen.kt:34`: `allSavedMovies.filter { … }` bei jeder Recomposition ohne `remember`. Ähnliche Muster gibt es in `FavoritesScreen`.
- `collectAsState()` statt `collectAsStateWithLifecycle()` (9×).
- Poster werden als `w500` geladen, auch für 75-dp-Thumbnails. → `w185` für Karten, `w500` nur im Detail (spart RAM, Cache und Datenvolumen).
- `FavoritesScreen.kt:101` `items(categories)` ohne `key`.
- `SettingsViewModel` wird in `MainScreenApp` und `MainContent` je einmal instanziiert (verschiedene Back-Stack-Scopes). Funktional okay, weil beide den DataStore lesen.
- Build: `material3:1.3.0` ist explizit neben BOM 2024.09 gepinnt, dazu `ui-test-junit4:1.6.0` doppelt und `animation-core-lint` als `implementation`. Haze 0.6.2 ist mehrere Major-Versionen alt (aktuell 2.1.0).

## 9. Priorisierte Liste

| Prio | Punkt | Aufwand |
|---|---|---|
| **Hoch** | Theme-Fundament: `AppTheme` reaktiv über `CompositionLocal`, `ColorScheme`/`Typography`/`Shapes`/Motion-Tokens zentral, `Color.Black` → `onAccent` | M |
| **Hoch** | Große Schrift: `heightIn` statt fester Kartenhöhe, Mindestschrift 11 sp | S |
| **Hoch** | Touch-Ziele 48 dp (Settings, Nav, Karten-Aktionen) + Tab-Semantik der Nav | S |
| **Mittel** | `Toast` → compose-sonner im Cinema-Stil | S |
| **Mittel** | Gemeinsamer `CinemaDatePickerDialog` (4 Kopien → 1) | S |
| **Mittel** | Leer-Zustände mit Icon + Aktion, Poster-Platzhalter | S–M |
| **Mittel** | Einheitliche Spring-Motion, `animateItem()` in Listen, reduzierte Animation respektieren | M |
| **Mittel** | Edge-to-Edge + Predictive Back | S |
| **Mittel** | Poster-Größe `w185` für Karten, `remember` für Filter, `collectAsStateWithLifecycle` | S |
| Niedrig | Haze auf 2.x (API-Änderungen), Build-Abhängigkeiten aufräumen | M |
| Niedrig | TalkBack-Feinschliff (Merge-Semantik, Long-Press-Label) | S |
| Niedrig | Adaptive Layouts (Tablet/Querformat), optionales Light Theme | L |
