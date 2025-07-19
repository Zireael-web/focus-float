# FocusFloat

Personal minimalist Android launcher for Google Pixel 7.

FocusFloat is local-only. It has no server backend, no accounts, no analytics, no
network permission, no VPN, and no Accessibility-based app interception.

Core MVP:

- default Home launcher intent;
- text-first Home screen;
- launchable app list via `LauncherApps`;
- favorites, rename, hide;
- default Focus Pause category for apps you want to pause;
- package pause/unpause through Shizuku shell:
  - `cmd package suspend --user 0 --dialogMessage "Paused until midnight" <package>`;
  - `cmd package unsuspend --user 0 <package>`;
- local Room/DataStore persistence;
- AlarmManager + WorkManager reconcile path for midnight unpause.

Design source:

- `docs/design/TOKENS.md`
- `docs/design/COMPONENTS.md`
- `docs/design/FocusFloat.html`

Open `FocusFloat.html` through a local static server if the CDN-backed React
preview is needed.

## Build notes

The project is configured as a standard single-module Android app. Open it in
Android Studio or build it with `./gradlew assembleDebug`.

## Privacy constraints

Do not add:

- `android.permission.INTERNET`
- `android.permission.QUERY_ALL_PACKAGES`
- VPN services
- Accessibility services
- Firebase / analytics / crash SaaS
- REST / GraphQL / remote config clients
