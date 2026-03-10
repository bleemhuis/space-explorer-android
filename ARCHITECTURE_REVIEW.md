# Architektur-Review: Space Explorer Android

> Analysiert: 42 Kotlin-Dateien (Produktion + Tests)
> Datum: 2026-03-09
> Gesamtnote: **7/10**

---

## 1. Clean Architecture — Note: 8/10

### Stärken
- Die Dependency Rule wird durchgehend eingehalten. Keine `import com.spaceexplorer.data.*`-Zeile in `domain/` oder `presentation/`.
- Die Domain-Schicht hat keinerlei Android-Imports (`android.*`). Nur `javax.inject.Inject` (JSR-330) wird verwendet — als Framework-unabhängige DI-Annotation akzeptabel.
- Repository-Interface in `domain/repository/ApodRepository.kt`, Implementierung in `data/repository/ApodRepositoryImpl.kt` — korrekte Inversion.
- Mapper (`data/mapper/ApodMapper.kt`) wandeln DTOs und Entities sauber in Domain-Modelle um. DTOs werden nie an die Presentation-Schicht weitergegeben.

### Schwächen

**S1: Use Cases sind teilweise reine Durchreich-Delegationen ohne Business-Logik**

Die folgenden Use Cases sind 1:1-Weiterleitungen an das Repository ohne Transformation oder Validierung:

- `domain/usecase/GetApodUseCase.kt`, Zeile 10–11
- `domain/usecase/GetApodRangeUseCase.kt`, Zeile 10–11
- `domain/usecase/GetFavoritesUseCase.kt`, Zeile 11
- `domain/usecase/ObserveIsFavoriteUseCase.kt`, Zeile 10

`GetApodRangeUseCase` könnte z.B. die Datumsvalidierung kapseln (kein Datum in der Zukunft, Startdatum vor Enddatum) — tut es aber nicht.

Positiv: `ToggleFavoriteUseCase` und `RemoveFavoriteUseCase` enthalten tatsächlich eigene Logik.

**S2: `ApodUiState` verwendet `Apod` (Domain-Modell) direkt**

`presentation/viewmodel/ApodUiState.kt`, Zeile 3 und 7:
```kotlin
import com.spaceexplorer.domain.model.Apod
data class Success(val apod: Apod) : ApodUiState()
```

Architektonisch erlaubt (Presentation darf Domain kennen), aber in strikter Clean Architecture würden dedizierte UI-Models verwendet. Bei diesem Projektumfang vertretbar.

**S3: Kein separates Domain-Modul**

Alles liegt in einem einzigen `app`-Modul. Der Compiler erzwingt die Schichtentrennung nicht — sie basiert rein auf Konvention. Bei Wachstum werden versehentliche Schichtverletzungen wahrscheinlicher.

---

## 2. SOLID-Prinzipien — Note: 7/10

### Stärken
- Use Cases haben je eine `invoke()`-Methode (Single Responsibility).
- ViewModels sind klar getrennt (`ApodViewModel`, `FavoritesViewModel`, `HistoryViewModel`).
- Repository wird über Interface (`ApodRepository`) injiziert — Dependency Inversion korrekt.
- Sealed Classes (`ApodUiState`, `HistoryUiState`, `UiEvent`) ermöglichen typsichere Erweiterung (Open/Closed).

### Verletzungen

**V1: `ApodRepository`-Interface ist zu breit — Interface Segregation Principle verletzt**

`domain/repository/ApodRepository.kt`, Zeile 6–13:
```kotlin
interface ApodRepository {
    suspend fun getApod(date: String? = null): Result<Apod>
    suspend fun getApodRange(startDate: String, endDate: String): Result<List<Apod>>
    fun getFavorites(): Flow<List<Apod>>
    fun isFavorite(date: String): Flow<Boolean>
    suspend fun addFavorite(apod: Apod)
    suspend fun removeFavorite(date: String)
}
```

Dieses Interface mischt zwei völlig verschiedene Verantwortlichkeiten: APOD-Abruf (API) und Favoriten-Verwaltung (lokal). Ein Client, der nur Favoriten braucht (`FavoritesViewModel`), muss das gesamte Interface kennen. Besser wären `ApodFetchRepository` und `FavoriteRepository`.

**V2: `ApodRepositoryImpl` hat zu viele Verantwortlichkeiten — Single Responsibility verletzt**

`data/repository/ApodRepositoryImpl.kt` verwaltet gleichzeitig:
1. API-Aufrufe (Zeile 27)
2. Cache-Logik mit Eviction (Zeile 30–34)
3. Offline-Fallback (Zeile 36–43)
4. Favoriten-CRUD (Zeile 56–66)

Die Cache-Eviction-Logik gehört in eine eigene Klasse oder zumindest in den `CacheDao`.

**V3: `FavoritesViewModel` verwendet `List<Apod>` statt eines dedizierten UiState**

`presentation/viewmodel/FavoritesViewModel.kt`, Zeile 23:
```kotlin
val favorites: StateFlow<List<Apod>>
```

Es fehlt ein dedizierter `FavoritesUiState` (sealed class mit Loading/Success/Error). Der initiale Zustand `emptyList()` ist nicht von "Daten werden geladen" unterscheidbar. Fehlerbehandlung fehlt komplett.

**V4: Kein Timeout im `NetworkModule`**

`di/NetworkModule.kt`, Zeile 32–49: Der OkHttpClient hat keine expliziten Timeouts. Die OkHttp-Defaults (10s) können bei der NASA-API zu knapp sein.

---

## 3. Kotlin-Qualität — Note: 7/10

### Stärken
- Korrekte Nutzung von `data class`, `sealed class`, `data object`.
- `operator fun invoke()` für Use Cases — idiomatisch.
- Extension Functions im Mapper — sauber.
- `mutableFloatStateOf` statt `mutableStateOf<Float>` in `ZoomableImage.kt` (Zeile 31) — Performance-bewusst.
- `?.let { }` für Nullable-Handling korrekt (`HomeScreen.kt`, Zeile 160).
- `coerceIn` in `ZoomableImage.kt` Zeile 35 — idiomatisch.

### Schwächen

**K1: Voll qualifizierter Klassenname statt Import**

`presentation/ui/screens/HomeScreen.kt`, Zeile 134:
```kotlin
androidx.compose.foundation.layout.Row(
```

Statt eines regulären Imports wird der FQN inline verwendet. Deutet auf Copy-Paste oder vergessenes Auto-Import hin.

**K2: Hardcodierte Strings überall in der UI**

Alle UI-Texte sind direkt in Composables hartcodiert statt `stringResource(R.string.*)` zu verwenden. Internationalisierung ist so unmöglich.

Betroffene Dateien (Auswahl):
- `HomeScreen.kt` (Zeile 72, 78, 84, 177)
- `DetailScreen.kt` (Zeile 82, 93)
- `FavoritesScreen.kt` (Zeile 69, 90, 159)
- `HistoryScreen.kt` (Zeile 70, 83, 99, 114)
- `ErrorContent.kt` (Zeile 35)
- `FullScreenImageViewer.kt` (Zeile 52)
- `ApodDateRangePicker.kt` (Zeile 44, 48)

**K3: Fehlende `Modifier`-Parameter in Top-Level-Composables**

`FullScreenImageViewer` (Zeile 21) und `ApodDateRangePicker` (Zeile 21) akzeptieren keinen `modifier`-Parameter. Das verletzt die Compose-API-Richtlinien für öffentliche Composables.

**K4: Duplizierter Code zwischen `HomeScreen` und `DetailScreen`**

Die Bild-Anzeige mit FullScreenViewer, Favoriten-Toggle und APOD-Detail-Anzeige ist in beiden Screens nahezu identisch (`HomeScreen.kt` Zeile 105–181, `DetailScreen.kt` Zeile 118–165). Könnte in ein gemeinsames `ApodContent`-Composable extrahiert werden.

**K5: `thumbnailUrl = null` im Mapper — funktionaler Bug**

`data/mapper/ApodMapper.kt`, Zeile 27:
```kotlin
thumbnailUrl = null  // not persisted in Room schema
```

Die `ApodEntity` speichert keine `thumbnailUrl`. Ein Video, das als Favorit gespeichert wird, verliert seinen Thumbnail beim Laden aus der Datenbank. Das ist ein funktionaler Datenverlust-Bug.

---

## 4. Fehlerbehandlung — Note: 6/10

### Stärken
- `Result<T>` wird konsequent als Return-Typ für API-Aufrufe verwendet.
- `runCatching` in `ApodRepositoryImpl.getApod()` (Zeile 27) und `getApodRange()` (Zeile 49).
- `onSuccess`/`onFailure` in allen ViewModels — sauberes Pattern.
- Fallback auf Cache bei Netzwerkfehler (`ApodRepositoryImpl.kt`, Zeile 36–43).
- `UiEvent.ShowSnackbar` für transiente Fehlermeldungen.

### Schwächen

**F1: `getApodRange` hat keinen Offline-Cache-Fallback**

`data/repository/ApodRepositoryImpl.kt`, Zeile 47–54:
```kotlin
override suspend fun getApodRange(startDate: String, endDate: String): Result<List<Apod>> =
    withContext(Dispatchers.IO) {
        runCatching {
            apiService.getApodRange(startDate, endDate)
                .map { it.toDomain() }
                .sortedByDescending { it.date }
        }
    }
```

Im Gegensatz zu `getApod()` wird hier bei Netzwerkfehler kein Cache-Fallback versucht. Die History-Funktion ist offline nicht nutzbar.

**F2: `FavoritesViewModel` hat keine Fehlerbehandlung — Crash-Risiko**

`presentation/viewmodel/FavoritesViewModel.kt`, Zeile 23–28:
```kotlin
val favorites: StateFlow<List<Apod>> = getFavoritesUseCase()
    .stateIn(...)
```

Wenn der Room-Flow eine Exception wirft (z.B. bei korrupter Datenbank), gibt es keinen `.catch`-Operator. Die App würde crashen. Fix: `.catch { emit(emptyList()) }` hinzufügen.

**F3: Keine Datumsvalidierung bei API-Aufrufen**

NASA APOD existiert seit dem 16.06.1995. Weder `GetApodUseCase` noch `GetApodRangeUseCase` validieren ob:
- das Datum in der Zukunft liegt
- das Startdatum vor dem Enddatum liegt
- der Zeitraum die API-Limits überschreitet

Bei ungültigen Daten zeigt die App nur "Unbekannter Fehler" — keine hilfreiche Nutzermeldung.

**F4: `addFavorite` und `removeFavorite` geben kein `Result<Unit>` zurück**

`data/repository/ApodRepositoryImpl.kt`, Zeile 62–66:
```kotlin
override suspend fun addFavorite(apod: Apod) =
    withContext(Dispatchers.IO) { dao.insertFavorite(apod.toEntity()) }

override suspend fun removeFavorite(date: String) =
    withContext(Dispatchers.IO) { dao.deleteFavoriteByDate(date) }
```

Bei Room-Fehler würde eine unbehandelte Exception propagiert. `ToggleFavoriteUseCase.addFavorite` hat kein `runCatching`-Wrapping.

**F5: Bild-Fehlerfall zeigt nur eine leere Box ohne Feedback**

`presentation/ui/components/ApodImage.kt`, Zeile 40–46:
```kotlin
error = {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.errorContainer)
    )
}
```

Kein Fehlertext, kein Icon, kein Retry-Button. Der Nutzer sieht nur eine farbige Fläche.

**F6: `DetailScreen` lädt APOD doppelt**

`presentation/ui/screens/DetailScreen.kt`, Zeile 59–61:
```kotlin
LaunchedEffect(date) {
    viewModel.loadApod(date)
}
```

Da `ApodViewModel` im `init`-Block bereits `loadApod()` aufruft (Zeile 50–51), wird beim Navigieren zweimal geladen: einmal ohne Datum (= heute) und einmal mit dem übergebenen Datum. Führt zu kurzem Flackern und einer unnötigen API-Anfrage.

---

## 5. Zusätzliche Befunde

**Z1: Fehlende Tests für `ApodViewModel` und `FavoritesViewModel`**

Vorhandene Tests:
- `GetApodUseCaseTest` ✓
- `ToggleFavoriteUseCaseTest` ✓
- `GetApodRangeUseCaseTest` ✓
- `ApodRepositoryImplTest` ✓
- `HistoryViewModelTest` ✓

Nicht getestet:
- `ApodViewModel` (komplexester ViewModel)
- `FavoritesViewModel`
- `ObserveIsFavoriteUseCase`
- `GetFavoritesUseCase`
- `RemoveFavoriteUseCase`

ViewModel-Testabdeckung: **33% (1 von 3)**

**Z2: Keine Instrumented Tests vorhanden**

`app/src/androidTest/` ist komplett leer. Die CLAUDE.md fordert "UI-Tests mit Compose Testing API für kritische Flows" — nicht umgesetzt.

**Z3: `exportSchema = false` widerspricht CLAUDE.md**

`data/local/database/AppDatabase.kt`, Zeile 13:
```kotlin
exportSchema = false
```

Die CLAUDE.md erwähnt "Schema-Export-Dateien (`schemas/`) sind schreibgeschützt" — das setzt voraus, dass Schema-Export aktiviert ist. Sollte auf `true` geändert und der Export-Pfad in `build.gradle.kts` konfiguriert werden.

---

## Zusammenfassung

| Kategorie | Note | Kritischste Schwäche | Quick Fix möglich? |
|---|---|---|---|
| Clean Architecture | **8/10** | Use Cases ohne Business-Logik (reine Delegation) | Nein — erfordert Architektur-Entscheidung |
| SOLID-Prinzipien | **7/10** | `ApodRepository` mischt API- und Favoriten-Verantwortung (ISP) | Ja — Interface aufteilen |
| Kotlin-Qualität | **7/10** | Hardcodierte UI-Strings statt String-Ressourcen | Ja — systematische Extraktion |
| Fehlerbehandlung | **6/10** | `FavoritesViewModel` crasht bei Room-Exception (kein `.catch`) | Ja — `.catch { }` hinzufügen |

**Gesamtnote: 7/10**

Die Codebasis ist für ein Projekt dieser Größe gut strukturiert. Schichtentrennung und grundlegende Patterns (MVVM, sealed UiState, Result-Wrapping) sind korrekt implementiert. Die größten Risiken liegen in der unvollständigen Fehlerbehandlung (F2, F6), dem funktionalen Thumbnail-Bug bei Favoriten (K5), und der lückenhaften Testabdeckung (Z1, Z2).

### Empfohlene Prioritäten

| Priorität | Maßnahme | Betrifft |
|---|---|---|
| 1 — Sofort (Crash-Risiko) | `.catch { }` in `FavoritesViewModel` | F2 |
| 2 — Kurzfristig (Bug) | `thumbnailUrl` in `ApodEntity` persistieren | K5 |
| 3 — Kurzfristig (UX) | Doppeltes Laden in `DetailScreen` beheben | F6 |
| 4 — Mittelfristig | String-Ressourcen extrahieren | K2 |
| 5 — Mittelfristig | Datumsvalidierung in Use Cases | F3 |
| 6 — Langfristig | `ApodRepository`-Interface aufteilen | V1 |
| 7 — Langfristig | ViewModel-Tests schreiben (ApodViewModel, FavoritesViewModel) | Z1 |
