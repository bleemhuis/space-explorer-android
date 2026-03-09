# Architecture Review — Space Explorer Android

**Reviewed:** 2026-03-09
**Reviewer Role:** Senior Android Architect (android-architect-reviewer)
**Files analysed:** 28 Kotlin-Dateien über alle Schichten

---

## 1. Clean Architecture — Note: 6 / 10

### Positiv
Die Paketstruktur bildet die drei Schichten korrekt ab. DTOs erreichen nie die Presentation-Schicht. Mapper sind in `data/mapper/` isoliert und korrekt als `internal` deklariert. Repository-Interface in `domain/`, Implementierung in `data/` — korrekte Inversion.

### Verletzungen

**Verletzung 1 — `javax.inject.Inject` in der Domain-Schicht**

`GetApodUseCase.kt` Z. 5+7, `ToggleFavoriteUseCase.kt` Z. 6+8:
```kotlin
import javax.inject.Inject

class GetApodUseCase @Inject constructor(
```
`javax.inject` koppelt die Domain an einen DI-Container. Die CLAUDE.md-Regel „domain hat **keine** Android-Abhängigkeiten" ist im strengen Sinne verletzt. In der Praxis weitgehend akzeptiert, aber dokumentierungswürdig.

**Verletzung 2 — `ApodViewModel` greift direkt auf `ApodRepository` zu**

`ApodViewModel.kt` Z. 6, 26, 35:
```kotlin
import com.spaceexplorer.domain.repository.ApodRepository  // Z. 6

class ApodViewModel @Inject constructor(
    private val getApodUseCase: GetApodUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val repository: ApodRepository          // Z. 26 — direkter Repository-Zugriff
) : ViewModel() {
    ...
    repository.isFavorite(state.apod.date)          // Z. 35
```
`isFavorite()` wird direkt vom ViewModel aufgerufen, nicht via Use Case. Ein `ObserveIsFavoriteUseCase` fehlt.

**Verletzung 3 — `FavoritesViewModel` hat null Use Cases**

`FavoritesViewModel.kt` Z. 6, 16, 19, 28:
```kotlin
import com.spaceexplorer.domain.repository.ApodRepository

class FavoritesViewModel @Inject constructor(
    private val repository: ApodRepository    // Z. 16
) : ViewModel() {
    val favorites = repository.getFavorites() // Z. 19
    fun removeFavorite(apod: Apod) {
        repository.removeFavorite(apod.date)  // Z. 28
    }
}
```
Beide Business-Operationen (`getFavorites`, `removeFavorite`) werden direkt auf dem Repository aufgerufen. Fehlende Use Cases: `GetFavoritesUseCase`, `RemoveFavoriteUseCase`.

**Verletzung 4 — `thumbnailUrl` wird beim DB-Round-Trip verloren**

`ApodMapper.kt` Z. 26:
```kotlin
internal fun ApodEntity.toDomain(): Apod = Apod(
    ...
    thumbnailUrl = null  // not persisted in Room schema
)
```
`ApodEntity` hat kein `thumbnailUrl`-Feld. Bei Video-APODs wird `displayUrl` aus der DB immer auf den rohen YouTube-URL fallen, der in einem `Image`-Composable nicht darstellbar ist. Die Business-Regel `isVideo` / `displayUrl` bricht lautlos nach einem DB-Round-Trip.

**Verletzung 5 — `exportSchema = false`**

`AppDatabase.kt` Z. 11:
```kotlin
@Database(entities = [ApodEntity::class], version = 1, exportSchema = false)
```
Die CLAUDE.md-Spec sieht automatisch generierte Schema-Export-Dateien vor. Mit `exportSchema = false` wird kein Schema exportiert — Migration-Safety-Net ist deaktiviert.

| Regel | Status |
|---|---|
| Abhängigkeitsrichtung | Teilweise verletzt (2 ViewModels halten Repositories) |
| Domain ohne Android-Imports | Grenzwertig (`javax.inject`) |
| Use Cases als einziger Einstiegspunkt aus Presentation | Verletzt |
| DTOs bleiben in der Data-Schicht | Korrekt |
| Entity-Mapping vollständig | Verletzt (`thumbnailUrl` geht verloren) |
| Schema-Export | Verletzt |

---

## 2. SOLID-Prinzipien — Note: 5 / 10

### Single Responsibility

**SRP-Verletzung 1 — `ApodViewModel` hat drei Aufgaben**

`ApodViewModel.kt`:
1. Tagesaktuelle APOD laden (Z. 53–63 — Netzwerk)
2. Favoriten-Toggle-Zustand verwalten (Z. 65–73 — DB-Schreiben)
3. Favoriten-Status reaktiv beobachten (Z. 32–44 — DB-Lesen-Stream)

Aufgabe 3 ruft `repository.isFavorite()` direkt auf, was Use Cases umgeht und das Testen erschwert.

**SRP-Verletzung 2 — `ApodUiState.kt` enthält zwei Sealed Classes**

`ApodUiState.kt`:
```kotlin
sealed class ApodUiState { ... }  // Z. 5–9
sealed class UiEvent { ... }      // Z. 11–13
```
Zwei konzeptionell verschiedene Typen in einer Datei. `UiEvent` ist ein Einweg-Seiteneffekt-Kanal; `ApodUiState` ist beobachtbarer Zustand — gehören in getrennte Dateien.

**SRP-Verletzung 3 — Vollqualifizierter Type-Name in Composable-Body**

`HomeScreen.kt` Z. 111:
```kotlin
androidx.compose.foundation.layout.Row(
    modifier = Modifier.fillMaxWidth(),
```
`Row` wurde nicht in den Import-Block aufgenommen und stattdessen vollqualifiziert verwendet — Java-Style-Workaround.

### Dependency Inversion

**DIP-Verletzung — ViewModels hängen an `ApodRepository` statt an Use Cases**

Bereits oben detailliert. Auch wenn `ApodRepository` ein Interface ist, kennt die Presentation-Schicht Repository-Level-Operationen — verletzt DIP im Sinne geschichteter Architektur.

**DIP korrekt — `RepositoryModule` nutzt `@Binds`**

`RepositoryModule.kt`:
```kotlin
@Binds
@Singleton
abstract fun bindApodRepository(impl: ApodRepositoryImpl): ApodRepository
```
Idiomatisch korrekt.

### Interface Segregation

`ApodRepository` hat 5 Methoden. `FavoritesViewModel` nutzt nur 2 davon. Test-Doubles müssen alle 5 Methoden stubben — ISP-Geruch. Mögliche Aufteilung: `ApodRemoteRepository` + `FavoritesRepository`.

### Open/Closed

`NasaApiService.getApodRange()` (Z. 15–21) ist deklariert, aber nirgends aufgerufen — totes Interface ohne Produktionsnutzen.

---

## 3. Kotlin-Qualität — Note: 7 / 10

### Positiv

**`runCatching` idiomatisch**

`ApodRepositoryImpl.kt` Z. 22:
```kotlin
runCatching { apiService.getApod(date).toDomain() }
```

**`data object` für singleton sealed class member**

`ApodUiState.kt` Z. 6:
```kotlin
data object Loading : ApodUiState()
```
Korrektes Kotlin 1.9+ Idiom.

**`apply` korrekt in `NetworkModule`**

`NetworkModule.kt` Z. 41–48:
```kotlin
HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) { ... } else { ... }
}
```

**`EXISTS`-Subquery im DAO**

`ApodDao.kt` Z. 20–21:
```kotlin
@Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE date = :date)")
fun isFavorite(date: String): Flow<Boolean>
```

### Schwächen

**Problem 1 — Vollqualifizierter `Row`-Import**

`HomeScreen.kt` Z. 111:
```kotlin
androidx.compose.foundation.layout.Row(
```
Import fehlt. Trivial zu beheben.

**Problem 2 — Race Condition in `ToggleFavoriteUseCase`**

`ToggleFavoriteUseCase.kt` Z. 12–16:
```kotlin
if (repository.isFavorite(apod.date).first()) {
    repository.removeFavorite(apod.date)
} else {
    repository.addFavorite(apod)
}
```
`.first()` auf einem `Flow<Boolean>` terminiert nach einer Emission. Zwischen Read und Write kann der DB-Zustand ändern (Race Condition). Idiomatischere Alternative: atomare DB-Operation (`UPSERT` / `DELETE WHERE EXISTS`) oder `suspend fun isFavorite()` das einen einzelnen Wert zurückgibt.

**Problem 3 — Hardcodierte deutsche UI-Strings**

`ApodViewModel.kt` Z. 59–60:
```kotlin
ApodUiState.Error(error.message ?: "Unbekannter Fehler")
UiEvent.ShowSnackbar(error.message ?: "Fehler beim Laden")
```
Alle User-Facing Strings gehören in `res/values/strings.xml`. Verhindert Lokalisierung und Lint-Checks.

**Problem 4 — `provideApodDao` ohne `@Singleton`**

`DatabaseModule.kt` Z. 27:
```kotlin
@Provides
fun provideApodDao(database: AppDatabase): ApodDao = database.apodDao()
```
Kein Scope-Annotation. Hilt erzeugt bei jeder Injektion eine neue `ApodDao`-Instanz — irreführend, auch wenn Room intern dieselbe Instanz zurückgibt.

---

## 4. Fehlerbehandlung — Note: 5 / 10

### Positiv

`runCatching` in `ApodRepositoryImpl` fängt alle `Throwable` korrekt ab. `ApodViewModel` reportet Fehler sowohl als persistenter `Error`-State als auch als Snackbar-Event.

### Kritische Schwächen

**Kritisch 1 — NASA API JSON-Fehlerbodies werden nie geparst**

`NasaApiService.kt` Z. 9–13:
```kotlin
suspend fun getApod(...): ApodDto  // nicht Response<ApodDto>
```
Retrofit wirft bei Non-2xx Responses eine `HttpException`. Der JSON-Fehlerbody der NASA API:
```json
{"error":{"code":"API_KEY_MISSING","message":"No api_key was supplied..."}}
```
wird **nie geparst**. Der User sieht `"HTTP 403 "` statt einer sinnvollen Fehlermeldung. Lösung: `Response<ApodDto>` + dedizierter Error-Parser.

**Kritisch 2 — `FavoritesViewModel.removeFavorite()` hat null Error Handling**

`FavoritesViewModel.kt` Z. 26–29:
```kotlin
fun removeFavorite(apod: Apod) {
    viewModelScope.launch {
        repository.removeFavorite(apod.date)  // kein try/catch, kein runCatching
    }
}
```
Eine Room-Exception (Disk voll, DB-Korruption) lässt die Coroutine lautlos crashen. Kein Feedback an den User, kein State-Update — UI und DB können inkonsistent werden.

**Kritisch 3 — Exception in `toggleFavorite` wird vollständig verworfen**

`ApodViewModel.kt` Z. 68–71:
```kotlin
runCatching { toggleFavoriteUseCase(state.apod) }
    .onFailure {
        _uiEvent.emit(UiEvent.ShowSnackbar("Favorit konnte nicht gespeichert werden"))
    }
```
`it` (der Throwable) wird ignoriert. Kein Logging, keine Unterscheidung zwischen Fehlertypen.

**Kritisch 4 — `ToggleFavoriteUseCase` gibt `Unit` statt `Result<Unit>` zurück**

`ToggleFavoriteUseCase.kt` Z. 11:
```kotlin
suspend operator fun invoke(apod: Apod)  // Unit — kein Error Contract
```
Inkonsistent zu `GetApodUseCase`, das `Result<Apod>` zurückgibt. Der Aufrufer muss `runCatching` selbst hinzufügen — der Fehlervertrag ist nicht im Typsystem ausgedrückt.

**Kritisch 5 — Kein Timeout auf `OkHttpClient`**

`NetworkModule.kt` Z. 33–49:
```kotlin
OkHttpClient.Builder()
    .addInterceptor { ... }
    .addInterceptor(HttpLoggingInterceptor()...)
    .build()
    // kein connectTimeout(), readTimeout(), writeTimeout(), kein Retry
```
OkHttp-Defaults (10 s) sind nicht deklariert. Kein Retry bei NASA-API 503-Antworten.

---

## Gesamtbewertung

| Kategorie | Note | Kritischste Schwäche | Quick Fix möglich? |
|---|---|---|---|
| Clean Architecture | **6 / 10** | Beide ViewModels injizieren `ApodRepository` direkt — Use Case Layer wird umgangen | Ja — `GetFavoritesUseCase` + `RemoveFavoriteUseCase` anlegen, Repository aus ViewModels entfernen |
| SOLID-Prinzipien | **5 / 10** | `ApodRepository`-Interface hat 5 Methoden, ISP verletzt; `FavoritesViewModel` ohne SRP-Abgrenzung | Teilweise — Interface-Split erfordert Refactoring in mehreren Dateien |
| Kotlin-Qualität | **7 / 10** | `flow.first()` Race Condition in `ToggleFavoriteUseCase`; vollqualifizierter `Row`-Import | Ja — `Row`-Import trivial; Race Condition erfordert atomare DB-Operation |
| Fehlerbehandlung | **5 / 10** | NASA API JSON-Fehlerbodies werden nie geparst; `FavoritesViewModel.removeFavorite()` hat null Error Handling | Nein — erfordert `Response<T>`-Wrapper und dedizierte Error-Parsing-Logik |

---

## Priorisierte Maßnahmen (vor Phase 2)

1. **`GetFavoritesUseCase` + `RemoveFavoriteUseCase` anlegen** — `ApodRepository` aus beiden ViewModels entfernen
2. **`ToggleFavoriteUseCase` → `Result<Unit>`** — konsistenter Error-Contract
3. **`FavoritesViewModel.removeFavorite()` absichern** — `runCatching` + Snackbar-Event
4. **`thumbnailUrl` in `ApodEntity` ergänzen** — Room-Migration Version 1→2 anlegen
5. **`NasaApiService` auf `Response<ApodDto>`** — NASA-Fehlerbody parsen
6. **Strings in `strings.xml`** auslagern
7. **`exportSchema = true`** + Schema-Pfad konfigurieren
