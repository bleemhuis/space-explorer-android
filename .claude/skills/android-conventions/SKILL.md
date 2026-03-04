---
name: android-conventions
description: Kotlin/Android Coding-Konventionen für dieses Projekt. 
             Aktiviere bei jeder Code-Generierung für Android.
---
## Pflicht-Patterns
- Sealed classes für API-Results: `Result<T>` mit Success/Error/Loading
- StateFlow in ViewModels, nie LiveData
- suspend functions in Use Cases, Flow in Repositories
- @HiltViewModel für alle ViewModels
- @Inject constructor für alle injizierten Klassen