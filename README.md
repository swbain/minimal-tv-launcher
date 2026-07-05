# Nocturne — Minimal TV Launcher

A dark, cinematic Android TV launcher: bundled wallpaper under layered scrims, a large
Newsreader-serif clock with live weather, and a 4-column grid of app cards with an amber
focus ring. Built with Compose for TV in an MVI-lite MVVM shape (single `StateFlow` state,
actions in, one-shot events out).

## Build

```sh
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintDebug            # lint
./gradlew :app:connectedDebugAndroidTest  # UI smoke test (needs a TV device/emulator)
```

Set as the device's home screen when prompted (the manifest declares the `HOME` category).

## Data & licenses

- **Weather** by [Open-Meteo](https://open-meteo.com/) (free non-commercial tier, no API key),
  licensed [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- **IP geolocation** by [GeoJS](https://www.geojs.io/) (free, keyless, HTTPS).
- **Newsreader** typeface bundled under the
  [SIL Open Font License 1.1](https://openfontlicense.org/).
