# Space Explorer — Feature Backlog

## High Priority

- **Search / Random APOD** — "Surprise me" button that loads a random past APOD using a random date generator against the known APOD history (starts June 16, 1995).

## Medium Priority

- **Share APOD** — Share button on Home/Detail screen using Android's `ShareCompat` to share image + title + NASA link.

- **Full-Screen Image Viewer** — Tap image to open full-screen with pinch-to-zoom support.

- **Video Support** — The `isVideo` / `thumbnailUrl` properties exist in the domain model but the UI only handles images. Embed a YouTube/WebView player for video APODs.

## Lower Priority

- **Notifications / Daily Reminder** — WorkManager job that fetches today's APOD and posts a notification with the image and title.

- **Widget** — Android App Widget showing today's APOD on the home screen.

- **Dark/Light Theme Toggle** — Manual theme switcher stored in DataStore preferences.

- **Favorites Search / Filter** — Search bar and sort options (by date, by title) on the Favorites screen.

## Done

- ~~**Date Picker / APOD History Browser**~~ — Browse past APODs by picking a date range. Implemented in commit `ad8de82`.
- ~~**Offline Mode / Caching**~~ — Network-first cache with Room fallback (50-entry LRU, thumbnailUrl persisted, DB migration 1→2). Implemented in commit `a6ffd37`.
- ~~**Full-Screen Image Viewer**~~ — Tap any APOD image to open a full-screen Dialog with pinch-to-zoom (1×–5×) and pan. No new dependencies.
