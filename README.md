# Comet Slicer Bot

Android screen-capture automation demo with:
- Floating overlay START / STOP button
- MediaProjection screen capture
- AccessibilityService swipe gestures
- Pure Java template matching using PNG files in `assets/templates`
- GitHub Actions debug APK build

Use only on your own apps, test builds, or games where automation is allowed.

## Setup on phone

1. Install the debug APK.
2. Open **Comet Slicer**.
3. Enable overlay permission.
4. Enable accessibility service for gestures.
5. Allow screen capture.
6. Tap **Show floating start/stop button**.
7. Open the game and tap START.

## Templates

Current templates:
- `app/src/main/assets/templates/comet_white_template.png`
- `app/src/main/assets/templates/comet_pink_template.png`
- `app/src/main/assets/templates/skull_template.png`

For better accuracy, replace these PNGs with clean crops from your own device screenshots.

## GitHub Actions

Push this project to GitHub. The workflow builds:

`app/build/outputs/apk/debug/app-debug.apk`

The APK is uploaded as artifact name:

`comet-slicer-debug-apk`
