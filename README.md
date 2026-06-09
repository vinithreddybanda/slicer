# CometSlicerBot v3 no-autoclose

This version specifically fixes app auto-closing immediately on open.

## What was removed/changed

- Removed custom `Application` from manifest.
- Removed all dynamic broadcast receivers from MainActivity and overlay.
- Replaced broadcast log updates with safe Handler refresh loops.
- Wrapped startup permission calls with try/catch.
- Kept floating logs, capture logs, match logs, and slice logs.
- Kept GitHub Actions debug APK build.

## Use

1. Open app.
2. Enable overlay permission.
3. Enable accessibility.
4. Allow screen capture.
5. Show floating start/logs.
6. Open your test game.
7. Press START.

## Good commit

`fix: remove startup crash sources and stabilize main screen`
