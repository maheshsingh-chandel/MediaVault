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
|-- monitor/    # WatchService-based real-time filesystem monitoring
|-- player/     # Media viewing, VLCJ playback adapters, playlist/slideshow logic
|-- scanner/    # Mounted-drive discovery and recursive media indexing
|-- thumbnail/  # Async thumbnail generation and cache management
|-- ui/         # Compose UI screens and navigation
`-- AGENTS.md   # Architecture and contribution rules for coding agents
```

## Current Features

- Compose Desktop application shell
- Dashboard screen with media statistics
- Library screen with search, sorting, pagination, thumbnails, actions, and details
- Media viewer for images, video, and audio
- Settings screen placeholder
- SQLite database initialization on startup
- `media_files` table with metadata JSON storage
- Repository pattern for media files
- Koin-based dependency injection
- Automatic mounted-drive detection
- Recursive media scanning with `Files.walkFileTree`
- Real-time filesystem monitoring with `WatchService`
- Graceful handling for inaccessible folders
- Live dashboard scan progress
- Lazy thumbnail generation for Library rows
- Metadata extraction stored as JSON on media records
- Image fullscreen viewing, zoom controls, and slideshow navigation
- VLCJ-backed video playback with play, pause, seek, and fullscreen controls
- Audio playlist controls with shuffle and repeat modes

## Not Implemented Yet

- Metadata refresh jobs

## Requirements

- JDK 21
- Gradle, or a Gradle wrapper if one is added later
- VLC installed locally for VLCJ video/audio playback

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

## Media Viewing

Library rows open media inside MediaVault.

Images support fullscreen viewing, zoom controls, previous/next navigation, and slideshow mode.

Videos use VLCJ and support play, pause, seek, and fullscreen controls. Audio uses VLCJ playback with playlist navigation, shuffle, and repeat modes.

VLCJ requires VLC to be installed on the machine and discoverable by the native runtime.

## Filesystem Monitoring

MediaVault uses `WatchService` to monitor directories that already contain indexed media. It handles:

- Created media files
- Modified media files
- Deleted media files
- Newly created child directories

The monitor updates SQLite directly and increments a UI refresh version so Dashboard and Library data reload automatically. It does not perform a full rescan. If a watched directory or drive becomes unavailable, the monitor drops that watch and keeps running.

## Architecture

MediaVault follows a Clean Architecture-style module boundary:

- `core` contains domain models and repository contracts.
- `database` implements persistence using SQLite and Exposed.
- `metadata` implements JSON metadata extraction.
- `monitor` implements real-time filesystem monitoring.
- `player` implements viewing and playback state.
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
