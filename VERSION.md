# CometSlicerBot v1.1

Fix build focused on your reported issue:

- START changed back to START immediately
- App asked for screen capture again without showing why
- No visible runtime errors
- No visible slice/tap logs

## Added

- Floating log panel under START / STOP button
- Logs for:
  - START pressed
  - capture service created
  - capture size
  - templates loaded
  - comet match score
  - no comet match
  - slice/tap coordinates
  - gesture completed/cancelled
  - capture errors
  - MediaProjection stopped
- Better state handling:
  - STARTING state
  - RUNNING state only after capture is actually active
  - STOPPED state after service closes
- Clear message that Android may require screen-capture permission again after STOP/crash.

## Good commit message

fix: add floating runtime logs and stable capture state
