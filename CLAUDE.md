# Space Explorer — Android

Kotlin-App zur Anzeige von NASA APOD-Bildern (Astronomy Picture of the Day).

## Tech Stack

- **Sprache**: Kotlin
- **UI**: Jetpack Compose
- **DI**: Hilt
- **Datenbank**: Room
- **Netzwerk**: Retrofit + OkHttp
- **Architektur**: Clean Architecture + MVVM

---

## Build-Befehle

```bash
# Debug-Build
./gradlew assembleDebug

# Release-Build
./gradlew assembleRelease

# Unit-Tests
./gradlew test

# Instrumented Tests (Emulator/Gerät erforderlich)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Alle Checks (vor einem PR)
./gradlew check

# App installieren (Gerät/Emulator verbunden)
./gradlew installDebug
```

---

## Projektstruktur (Clean Architecture)

```
app/src/main/java/com/spaceexplorer/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── entity/       # Room Entities
│   │   └── database/     # AppDatabase.kt
│   ├── remote/
│   │   ├── api/          # Retrofit Interfaces (NasaApiService.kt)
│   │   └── dto/          # API Response-DTOs
│   └── repository/       # Repository-Implementierungen
├── domain/
│   ├── model/            # Business-Modelle (reine Kotlin-Klassen)
│   ├── repository/       # Repository-Interfaces
│   └── usecase/          # Use Cases (ein Use Case pro Datei)
├── presentation/
│   ├── ui/
│   │   ├── screens/      # Composable Screens
│   │   ├── components/   # Wiederverwendbare Composables
│   │   └── theme/        # MaterialTheme, Farben, Typografie
│   └── viewmodel/        # ViewModels (HiltViewModel)
└── di/                   # Hilt-Module
```

---

## Architektur-Regeln

### Schichtenabhängigkeiten
- `presentation` darf nur `domain` importieren — niemals `data`
- `domain` hat **keine** Android-Abhängigkeiten (reines Kotlin)
- `data` implementiert `domain`-Interfaces, kennt `domain`-Modelle
- Abhängigkeiten zeigen immer nach innen: `data` → `domain` ← `presentation`

### Domain-Schicht
- Use Cases sind Klassen mit einem einzigen `operator fun invoke()`
- Business-Modelle (`domain/model/`) sind `data class` ohne Framework-Annotationen
- Repository-Interfaces liegen in `domain/repository/`, Implementierungen in `data/repository/`

### Data-Schicht
- DTOs (`data/remote/dto/`) werden **nicht** direkt an die Presentation-Schicht übergeben
- Jeder DAO gibt `Flow<T>` oder `suspend fun` zurück — kein blockierender Code
- Room-Entities (`data/local/entity/`) bleiben in der Data-Schicht

### Presentation-Schicht
- ViewModels halten `UiState` als `StateFlow` oder `MutableStateFlow`
- Composables erhalten ausschließlich primitive Typen oder `UiState`-Objekte — keine ViewModels direkt übergeben, außer auf Screen-Ebene
- Seiteneffekte (Navigation, Snackbar) über `SharedFlow`/`Channel` im ViewModel

### NASA APOD API
- Base URL: `https://api.nasa.gov/`
- Endpunkt: `GET /planetary/apod`
- API-Key in `local.properties` als `NASA_API_KEY` — niemals im Code hartkodieren
- Im BuildConfig als `NASA_API_KEY` verfügbar machen

---

## Was der Agent NICHT ändern darf

### Gesperrte Dateien — keine Änderungen ohne explizite Anweisung

| Datei/Verzeichnis | Grund |
|---|---|
| `app/build.gradle.kts` | SDK-Versionen, Abhängigkeiten, Signing-Konfiguration |
| `build.gradle.kts` (Root) | Plugin-Versionen, Classpath |
| `gradle/libs.versions.toml` | Zentrale Versionsverwaltung — Upgrades erfordern Kompatibilitätsprüfung |
| `app/src/main/assets/migrations/` | Room-SQL-Migrationsskripte |
| `app/src/main/java/**/database/migrations/` | Room-`Migration`-Objekte in Kotlin |
| `gradle.properties` | Build-Flags, JVM-Argumente |
| `local.properties` | API-Keys, lokale Pfade (nicht im VCS) |
| `.github/workflows/` | CI/CD-Pipelines |
| `keystore/` | Signing-Zertifikate |

### Room-Datenbank-Migrationen
- Bestehende Migrations **niemals löschen oder modifizieren**
- Neue Migrations **nur anlegen**, wenn eine Room-Entity geändert oder erstellt wird
- Schema-Export-Dateien (`schemas/`) sind schreibgeschützt — werden automatisch generiert
- `@Database(version = X)` nur erhöhen, wenn eine passende Migration existiert

---

## Code-Konventionen

- Kotlin-Dateien: `PascalCase` für Klassen, `camelCase` für Funktionen/Variablen
- Composables: `PascalCase`, immer `@Composable` + `@Preview` hinzufügen
- Hilt-Module: Dateiname endet auf `Module` (z. B. `NetworkModule.kt`, `DatabaseModule.kt`)
- Coroutines: `viewModelScope` in ViewModels, `Dispatchers.IO` für Netzwerk/DB-Operationen in Repositories
- Fehlerbehandlung: `Result<T>` oder `sealed class` für UI-States (`Loading`, `Success`, `Error`)

## Tests

- Unit-Tests für alle Use Cases und ViewModels
- Repository-Tests mit gefakten DAOs/API-Services
- UI-Tests mit Compose Testing API für kritische Flows
- Test-Dateien spiegeln die Produktionsstruktur unter `src/test/` bzw. `src/androidTest/` wider
