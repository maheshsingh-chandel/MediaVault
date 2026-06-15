# MediaVault

MediaVault is a Kotlin Compose Desktop application for managing a local media library.

The project currently includes a desktop UI, SQLite persistence, recursive scanning, lazy thumbnails, and metadata extraction. Playback is intentionally not implemented yet.

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
|-- app/        # Application entry point, Compose window, DI startup
|-- core/       # Domain models and repository interfaces
|-- database/   # SQLite initialization, Exposed tables, repository implementations
|-- metadata/   # Image, video, and audio metadata extraction
|-- scanner/    # Mounted-drive discovery and recursive media indexing
|-- thumbnail/  # Async thumbnail generation and cache management
|-- ui/         # Compose UI screens and navigation
`-- AGENTS.md   # Architecture and contribution rules for coding agents
```

## Current Features

- Compose Desktop application shell
- Dashboard screen with media statistics
- Library screen with search, sorting, pagination, thumbnails, actions, and details
- Settings screen placeholder
- SQLite database initialization on startup
- `media_files` table with metadata JSON storage
- Repository pattern for media files
- Koin-based dependency injection
- Automatic mounted-drive detection
- Recursive media scanning with `Files.walkFileTree`
- Graceful handling for inaccessible folders
- Live dashboard scan progress
- Lazy thumbnail generation for Library rows
- Metadata extraction stored as JSON on media records

## Not Implemented Yet

- Image preview
- Video playback
- Audio playback
- Metadata refresh jobs

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

## Metadata

MediaVault stores extracted metadata as JSON in SQLite.

Images include:

- Width and height
- EXIF fields
- Camera make and model
- GPS latitude and longitude, when available

Videos include:

- Duration
- Resolution
- Codec
- Bitrate

Audio includes:

- Artist
- Album
- Genre
- Duration

Video metadata uses `ffprobe` when available on the system path. If probing fails, the metadata JSON records a failed status instead of crashing the app.

## Architecture

MediaVault follows a Clean Architecture-style module boundary:

- `core` contains domain models and repository contracts.
- `database` implements persistence using SQLite and Exposed.
- `metadata` implements JSON metadata extraction.
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

## Development Notes

- Do not implement playback until it is explicitly scoped.
- Keep domain code framework-free.
- Register dependencies through Koin modules.
- Keep database schema changes in the `database` module.
