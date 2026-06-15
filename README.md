# MediaVault

MediaVault is a Kotlin Compose Desktop application for managing a local media library.

This initial version sets up the application foundation: Clean Architecture modules, SQLite persistence, Exposed ORM, Koin dependency injection, and a basic desktop UI with Dashboard, Library, and Settings screens.

Scanning, thumbnails, and playback are intentionally not implemented yet.

## Tech Stack

- Kotlin
- JDK 21
- Compose Desktop
- SQLite
- Exposed ORM
- Koin
- Gradle Kotlin DSL

## Project Structure

```text
MediaVault/
├── app/        # Application entry point, Compose window, DI startup
├── core/       # Domain models and repository interfaces
├── database/   # SQLite initialization, Exposed tables, repository implementations
├── scanner/    # Mounted-drive discovery and recursive media indexing
├── thumbnail/  # Async thumbnail generation and cache management
├── ui/         # Compose UI screens and navigation
└── AGENTS.md   # Architecture and contribution rules for coding agents
```

## Current Features

- Compose Desktop application shell
- Dashboard screen with media statistics:
  - Total files
  - Images
  - Videos
  - Audio
- Library screen placeholder
- Settings screen placeholder
- SQLite database initialization on startup
- `media_files` table with:
  - `id`
  - `path`
  - `filename`
  - `extension`
  - `mediaType`
  - `size`
  - `createdDate`
  - `modifiedDate`
  - `indexedAt`
- Database indexes:
  - Unique index on `path`
  - Index on `filename`
  - Index on `mediaType`
- Repository pattern for media files
- Koin-based dependency injection
- Automatic mounted-drive detection
- Recursive media scanning with `Files.walkFileTree`
- Graceful handling for inaccessible folders
- Live dashboard scan progress
- Coroutine-backed scanning so the UI remains responsive
- Lazy thumbnail generation for Library rows
- Image thumbnails generated with `ImageIO`
- Video thumbnails generated from the 10-second frame through `ffmpeg`
- Thumbnail cache at `%APPDATA%\MediaVault\thumbnails`

## Not Implemented Yet

- Image preview
- Video playback
- Audio playback
- File metadata refresh jobs

## Requirements

- JDK 21
- Gradle, or a Gradle wrapper if one is added later

Check your Java version:

```powershell
java -version
```

## Build

```powershell
gradle build
```

If you are using the local Gradle distribution that was downloaded during setup:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat build
```

## Run

```powershell
gradle :app:run
```

Or with the local Gradle distribution:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat :app:run
```

## Database

MediaVault initializes a SQLite database on startup at:

```text
%USERPROFILE%\.mediavault\mediavault.db
```

The database schema is created automatically by Exposed.

## Thumbnails

Thumbnails are generated lazily when rows appear in the Library screen.

Cached thumbnails are stored at:

```text
%APPDATA%\MediaVault\thumbnails
```

Thumbnail filenames use:

```text
SHA256(path).jpg
```

Existing thumbnails are reused and are never regenerated. Video thumbnails require `ffmpeg` to be available on the system path; if it is missing or cannot read a file, MediaVault records the thumbnail as failed and keeps the app running.

## Architecture

MediaVault follows a Clean Architecture-style module boundary:

- `core` contains domain models and repository contracts.
- `database` implements persistence using SQLite and Exposed.
- `scanner` implements mounted-drive discovery and recursive indexing.
- `thumbnail` implements asynchronous thumbnail generation and cache lookup.
- `ui` contains Compose UI code.
- `app` wires everything together and starts the desktop application.

Inner layers do not depend on outer layers. UI and database framework types should not leak into `core`.

## Testing

Run all available checks:

```powershell
gradle build
```

Current tests are intentionally small because this version only establishes the project foundation.

## Development Notes

- Do not implement scanning, thumbnails, or playback until those features are explicitly scoped.
- Keep domain code framework-free.
- Register dependencies through Koin modules.
- Keep database schema changes in the `database` module.
