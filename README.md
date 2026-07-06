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

## Release

CI runs the build/test/lint gates above on every PR (`.github/workflows/ci.yml`).
Signed release APKs come from `.github/workflows/release.yml`:

```sh
# bump versionCode/versionName in app/build.gradle.kts first, then:
git tag v1.1
git push origin v1.1
```

The tag run builds a signed APK, verifies its signature, and attaches it to a GitHub
Release as `nocturne-<tag>.apk`. Manual runs (`gh workflow run release.yml`) build and
upload the APK as a workflow artifact without publishing a release. Android requires a
strictly increasing `versionCode` for updates, so bump it for every release.

The workflow needs four repository secrets: `RELEASE_KEYSTORE_B64` (the keystore,
base64), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`.

To build a signed APK locally, create a gitignored `keystore.properties` at the repo
root pointing at your keystore (absolute path):

```properties
storeFile=/path/to/nocturne-release.jks
storePassword=...
keyAlias=nocturne
keyPassword=...
```

then `./gradlew :app:assembleRelease`. Without that file the release build still works
but emits an unsigned (non-installable) APK.

## Data & licenses

- **Weather** by [Open-Meteo](https://open-meteo.com/) (free non-commercial tier, no API key),
  licensed [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- **IP geolocation** by [GeoJS](https://www.geojs.io/) (free, keyless, HTTPS).
- **Newsreader** typeface bundled under the
  [SIL Open Font License 1.1](https://openfontlicense.org/).
