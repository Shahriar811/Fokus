# Fokus

Fokus is a native Android Pomodoro timer for reading, studying, working, and exercising. A new install starts with 25 minutes of focus and a 5-minute break.

## Using Fokus

- **Timer** shows the countdown and local clock. Pick an activity, then start, pause, resume, or end a block. Completed focus blocks are logged automatically. You can also save a shorter block when you end it early.
- **Full-screen clock** opens from the clock icon. Switch between analog and digital on a black background. Use the rotation button to lock the clock in landscape on supported phones.
- **Activities** holds your books, courses, tasks, workouts, and other activities. Items can be edited, archived, and restored. Archiving keeps their session history available.
- **Tree** grows from saved focus time, including manually logged sessions. Longer sessions contribute more, and new stages also require focus on different days. The seven-day view shows recent activity and your current streak.
- **Insights** compares today, the last 7 days, or the last 30 days with the preceding period of the same length. Filter by activity, inspect a session’s start and end times, or log a completed session manually with optional notes.
- **Settings** lets you customize both durations, auto-start breaks, and choose a light, dark, or system appearance.

## Android permissions

Focus mode uses an app-owned Do Not Disturb rule. Grant Do Not Disturb access when enabling it. The rule runs during active focus blocks and releases on pause, stop, or break. Android manages notification exceptions.

Enable timer alerts to get completion notifications. On Android 12 and later, grant **Precise background alerts** if you want Android to finish timers reliably while the phone is idle.

**Lock-screen progress** posts a live countdown. Your phone’s lock-screen and always-on display settings determine what it can show. **Keep screen on** is available for the in-app timer and full-screen clock.

## Build

Use JDK 17 or later and Android SDK 36. Fokus supports Android 8.0 and later.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Fokus keeps the former app’s Android package ID so an in-place update can import SmartRead study history. Installation over a previous build requires the same signing key.

Fokus bundles Manrope and Inter for offline use. Their SIL Open Font Licenses are packaged with the app.
