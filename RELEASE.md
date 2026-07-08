# MediaVault Release Packaging

MediaVault is packaged with Compose Desktop native distributions, which use `jpackage` and a bundled JRE. End users do not need Java, Gradle, Kotlin, or developer tools installed.

## Requirements

- Windows
- JDK 21 available to Gradle during packaging
- WiX Toolset installed if `jpackage` requires it for MSI/EXE generation on your machine
- VLC installed on target machines for VLCJ video/audio playback
- `ffmpeg` and `ffprobe` on target machines for video thumbnails and video metadata

MediaVault detects missing VLC, `ffmpeg`, and `ffprobe` in Settings and shows user-friendly status messages.

## Commands

Build and test:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat build
```

Clean release artifacts:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat cleanRelease
```

Create installers:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat createInstaller
```

Equivalent release command:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat packageRelease
```

Direct installer tasks:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat :app:packageExe
.\.tools\gradle-8.14.3\bin\gradle.bat :app:packageMsi
```

## Output Locations

Generated installers are written under:

```text
app\build\compose\binaries\main\exe
app\build\compose\binaries\main\msi
```

## Runtime Data

MediaVault does not store runtime data in the installation directory.

Persistent user data:

```text
%APPDATA%\MediaVault
```

Includes:

- SQLite database
- Thumbnails
- Logs
- Configuration

Temporary cache:

```text
%LOCALAPPDATA%\MediaVault\cache
```

Uninstalling the app removes the application installation, but must not delete user media files. User data in `%APPDATA%\MediaVault` is intentionally separate so upgrades preserve the database.

## Fresh User Test Checklist

1. Install MediaVault with the generated installer.
2. Launch from Start Menu shortcut.
3. Confirm Settings shows database, thumbnails, logs, config, and cache paths.
4. Confirm Settings reports VLC, `ffmpeg`, and `ffprobe` status.
5. Scan a small folder or drive.
6. Confirm media appears in Library.
7. Confirm thumbnails generate.
8. Open image viewer.
9. Play video/audio when VLC is installed.
10. Restart MediaVault.
11. Confirm database contents persist.
12. Uninstall from Windows Apps & Features.
