# CometSlicerBot v2

Re-coded version to avoid the app closing automatically and to show useful floating logs.

## Fixed / inspected problems

- Replaced unsafe `registerReceiver(receiver, filter)` with Android 13+ safe `Context.RECEIVER_NOT_EXPORTED`.
- Added app-level crash logger.
- Re-coded capture service lifecycle.
- Added clear STARTING/RUNNING/STOPPED state.
- Added foreground service startup error logs.
- Added MediaProjection startup, stop, and frame-processing logs.
- Added floating logs/errors under the overlay button.
- Added slice/tap coordinate logs.
- Set `targetSdk 33` for this debug tool to reduce Android 14 MediaProjection strict one-shot crashes.
- Capture permission is cleared after stop because Android may not allow token reuse.

## How to use

1. Open app.
2. Enable overlay permission.
3. Enable accessibility permission.
4. Allow screen capture.
5. Tap `Show floating start/logs`.
6. Open your own game/test app.
7. Tap START.

## APK build

Push to GitHub. Actions will build debug APK and upload artifact:

`comet-slicer-debug-apk`

## Good commit message

`fix: recode capture lifecycle and floating debug logs`
