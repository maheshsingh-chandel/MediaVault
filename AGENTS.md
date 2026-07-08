# MediaVault Agent Guidelines

## Clean Architecture Rules

- Keep domain models and repository interfaces in `core`.
- Keep SQLite, Exposed tables, and repository implementations in `database`.
- Keep duplicate hashing and duplicate grouping in `duplicate`.
- Keep media metadata extraction in `metadata`.
- Keep real-time filesystem monitoring in `monitor`.
- Keep media viewing and playback state in `player`.
- Keep mounted-drive detection and recursive indexing in `scanner`.
- Keep thumbnail generation and cache management in `thumbnail`.
- Keep Compose screens and UI state mapping in `ui`.
- Keep application startup, dependency wiring, and the Compose window entry point in `app`.
- Inner layers must not depend on outer layers.
- Do not put database or UI framework types in `core`.

## Kotlin Coding Standards

- Use Kotlin idioms such as immutable data classes, expression bodies when clear, and nullable types deliberately.
- Prefer explicit package names that match the module boundary.
- Keep functions small and name them by behavior.
- Avoid broad refactors unless they directly support the requested change.
- Do not add scanning, thumbnails, or playback until explicitly requested.

## Dependency Injection Rules

- Use Koin modules for construction.
- Register interfaces in `core` with implementations from outer modules.
- Avoid service locators in domain code.
- Keep database initialization as an application startup dependency.

## Testing Requirements

- Add focused tests for domain logic and repository behavior when it changes.
- Prefer deterministic tests that do not depend on user files or machine media folders.
- Run the most relevant Gradle checks before delivery.
- If a check cannot be run, report the reason and remaining risk.
