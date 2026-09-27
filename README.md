# FocusFloat

**English** | [Русский](README.ru.md)

A minimalist Android launcher. It replaces the stock home screen with a calm, near-monochrome one: a clock, the date and plain-text app lists instead of bright icons. Built for the Google Pixel 7 and fully local.

A personal side project for my own phone, where I explore native Android development with Kotlin and Jetpack Compose.

[Features](#features) · [Privacy](#privacy) · [Build](#build-from-source) · [Structure](#project-structure) · [Design](#design)

**Kotlin · Jetpack Compose · Room · DataStore · WorkManager · Android 8.0+**

## Features

- Home screen with a clock, the date and a battery indicator. Below them are plain-text links: all apps, favorites, distracting apps and Pixel Focus Mode.
- AMOLED-black theme by default. Color is used only for errors.
- A searchable list of all apps. Any app can be added to favorites, renamed, hidden from the list, marked as distracting, or opened in its system settings page.
- Distracting apps: you have to type the word `confirmed` before one opens. It is friction, not a block — a deliberate pause before opening.
- External launch guard: an optional accessibility service shows the same confirmation screen when a distracting app is opened outside the launcher, for example from a notification.
- A shortcut to Pixel Focus Mode (Digital Wellbeing).
- A setup guide that helps make FocusFloat the default home app.

## Privacy

FocusFloat never goes online: the manifest has no `INTERNET` permission, and `ACCESS_NETWORK_STATE`, which dependencies add, is explicitly removed. There are no accounts, no analytics and no backend. Favorites, renamed apps, lists and settings are stored on the device with Room and DataStore.

Both accessibility services are off by default and have to be turned on manually in Android settings. The external launch guard does not read window contents: it only learns which app was opened. The experimental Focus Mode automation service is meant for the Digital Wellbeing screens.

## Requirements

- Android 8.0 (API 26) or newer. Focus Mode relies on Google's Digital Wellbeing, so that part targets Pixel phones.
- JDK 17 and the Android SDK with platform 36, for example from Android Studio. The Gradle wrapper downloads the right Gradle version.

## Build from source

From the repository root:

```sh
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. To install it on a connected phone:

```sh
./gradlew installDebug
```

After installing, press Home and choose FocusFloat, or go through the setup guide inside the app.

Unit tests:

```sh
./gradlew testDebugUnitTest
```

### Signed `internal` build

Copy `keystore.properties.example` to `keystore.properties` and fill in the keystore path, alias and passwords. You can set the `FOCUSFLOAT_INTERNAL_*` environment variables instead. Then run `./gradlew assembleInternal`. `keystore.properties` and `keystores/` are never committed.

## Project structure

```text
FocusFloat/
├── app/src/main/java/com/focusfloat/app/
│   ├── ui/                Compose screens and MainViewModel
│   ├── design/            Theme and base components (tokens from docs/design)
│   ├── launcher/          App list via LauncherApps, favorites, renaming, hiding
│   ├── distracting/       Launch confirmation and the external launch guard
│   ├── digitalwellbeing/  Pixel Focus Mode shortcut and its automation
│   ├── pause/             Early mechanism for pausing apps until midnight via Shizuku
│   ├── settings/, data/   DataStore and the Room database
│   └── core/              Models and time handling
├── app/src/test/          Unit tests
└── docs/                  Design and on-device check notes
```

## Design

`docs/design/` holds the design tokens (`TOKENS.md`), the component spec (`COMPONENTS.md`) and an interactive mockup of about 45 screens (`FocusFloat.html`). **[Open the interactive prototype](https://zireael-web.github.io/focus-float/design/FocusFloat.html)**. The mockup reflects the original concept: pausing app categories through Shizuku has since been replaced by the Pixel Focus Mode shortcut. Locally, serve the folder with any static server, for example `python3 -m http.server` in `docs/design`.

Things worth re-checking on a real device are listed in [`docs/spikes.md`](docs/spikes.md).

## License

[MIT](LICENSE)
