---
name: android-data-layer
description: Guidance on implementing the Data Layer using Repository pattern, Room (Local), and Retrofit (Remote) with offline-first synchronization.
---

# Android Data Layer & Offline-First

> **Source:** [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills) — Eclipse Public License 2.0

## Instructions

The Data Layer coordinates data from multiple sources.

### 1. Repository Pattern
*   **Role**: Single Source of Truth (SSOT).
*   **Logic**: The repository decides whether to return cached data or fetch fresh data.
*   **Implementation**:
    ```kotlin
    class NewsRepository @Inject constructor(
        private val newsDao: NewsDao,
        private val newsApi: NewsApi
    ) {
        // Expose data from Local DB as the source of truth
        val newsStream: Flow<List<News>> = newsDao.getAllNews()

        // Sync operation
        suspend fun refreshNews() {
            val remoteNews = newsApi.fetchLatest()
            newsDao.insertAll(remoteNews)
        }
    }
    ```

### 2. Local Persistence (Room)
*   **Usage**: Primary cache and offline storage.
*   **Entities**: Define `@Entity` data classes.
*   **DAOs**: Return `Flow<T>` for observable data.

### 3. Remote Data (Retrofit)
*   **Usage**: Fetching data from backend.
*   **Response**: Use `suspend` functions in interfaces.
*   **Error Handling**: Wrap network calls in `try-catch` blocks or a `Result` wrapper to handle exceptions (NoInternet, 404, etc.) gracefully.

### 4. Synchronization
*   **Read**: "Stale-While-Revalidate". Show local data immediately, trigger a background refresh.
*   **Write**: "Outbox Pattern" (Advanced). Save local change immediately, mark as "unsynced", use `WorkManager` to push changes to server.

### 5. Dependency Injection
*   Bind Repository interfaces to implementations in a Hilt Module.
    ```kotlin
    @Binds
    abstract fun bindNewsRepository(impl: OfflineFirstNewsRepository): NewsRepository
    ```

### 6. GymBro-Specific Notes

GymBro's iOS app is offline-first with SwiftData as source of truth and CloudKit for sync. The Android data layer should follow the same philosophy:
- **Local-first**: Room is the source of truth. All writes persist locally immediately.
- **Sync is opportunistic**: Background sync via WorkManager (mirrors iOS CloudKit opportunistic sync).
- **Conflict resolution**: Last-Writer-Wins with property-level merging for critical fields (workout data is sacred).
- **Performance**: Target < 100ms per set logging operation, < 5s sync for 1000 workouts.
- The Outbox Pattern is the right fit for workout data writes — never lose a user's PR.
