# CLAUDE.md

Nocturne — a minimal Android TV launcher (Compose for TV, MVI-lite MVVM). See README.md
for the build commands and data/license notes.

## Testing conventions

- **Do not write Compose/Espresso UI tests.** No new tests in `app/src/androidTest`.
- **All UI testing is done with Android journeys**: author a journey XML (see the
  android-cli skill's journeys reference) and evaluate it against the app on a TV
  emulator, driving the device with `android layout`, `android screen capture`, and
  adb input.
- JVM unit tests in `app/src/test` (ViewModel, formatters, repositories, weather
  parsing) are the right place for logic coverage and remain the standard.
