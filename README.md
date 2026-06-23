# react-native-android-floating-bubble

A draggable **floating bubble overlay** for React Native on **Android** — like the
Messenger chat heads. The bubble shows a numeric badge, can be dragged, snaps to the
nearest screen edge, persists its position, and opens the host app when tapped. It runs
on top of other apps via a foreground service.

On **iOS the API is a no-op** (`isSupported()` returns `false`) — iOS does not allow
windows on top of other apps — so cross-platform code does not break.

## Demo

https://github.com/user-attachments/assets/2ff07e33-c024-4e50-9764-11d50ae80412

## Requirements

- React Native **0.71+** (New Architecture / TurboModules; tested with RN 0.85).
- **Android**:
  - `SYSTEM_ALERT_WINDOW` (overlay) permission — granted by the user at runtime.
  - Foreground service of type **special use** → **Android 14+ (API 34)** is the most
    fragile target; the library handles the `startForeground` contract for you.
  - The permissions and the `<service>` are declared in the library's manifest and merged
    into your app automatically (manifest merge). No manual manifest edits required.

## Installation

### React Native CLI

```sh
npm install react-native-android-floating-bubble
# or
yarn add react-native-android-floating-bubble
```

Autolinking (RN ≥ 0.60) compiles the native code and registers the module — no manual
steps in `MainApplication`.

### Expo

This library contains native code, so it **does not run in Expo Go**. Use a
[development build](https://docs.expo.dev/develop/development-builds/introduction/):

```sh
npx expo install react-native-android-floating-bubble
npx expo prebuild      # generates the native android/ project and autolinks the lib
# then build & run a dev client:
npx expo run:android
```

## Configuration

### Icons (optional)

The bubble icon and the foreground-service notification icon default to icons bundled
with the library. To use your own, pass the **drawable resource names** from your host
app — they are resolved against your app's resources at runtime (the library never
references your app's `R`):

```ts
import { FloatingBubble } from 'react-native-android-floating-bubble';

FloatingBubble.setIcons({
  bubbleIcon: 'bubble_icon', // res/drawable/bubble_icon.* in your app
  notificationIcon: 'ic_notification', // res/drawable/ic_notification.* in your app
});
```

If a name is empty or cannot be resolved, the library default is used.

### What the library injects (manifest merge)

You do **not** need to add these — they come from the library's manifest:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<service
    android:name="com.androidfloatingbubble.FloatingBubbleService"
    android:exported="false"
    android:foregroundServiceType="specialUse" />
```

## Usage

```ts
import { FloatingBubble } from 'react-native-android-floating-bubble';

// 1. Make sure the overlay permission is granted
if (!FloatingBubble.hasOverlayPermission()) {
  FloatingBubble.requestOverlayPermission(); // opens the system settings screen
}

// 2. Show the bubble
FloatingBubble.show(3); // with badge "3"
FloatingBubble.show();  // keep the current badge count (KEEP_BADGE_COUNT)

// 3. Hide it
FloatingBubble.hide();
```

The bubble is shown only while your app is in the **background** — it hides automatically
when an Activity of your app is visible.

## API reference

| Method | Parameters | Returns | Behavior |
| --- | --- | --- | --- |
| `isSupported()` | — | `boolean` | `true` on Android, `false` on iOS. |
| `hasOverlayPermission()` | — | `boolean` | Whether `SYSTEM_ALERT_WINDOW` is granted. `false` on iOS. |
| `requestOverlayPermission()` | — | `void` | Opens the system overlay-permission screen. No-op if already granted / on iOS. |
| `show(badgeCount?)` | `badgeCount?: number` | `void` | Shows the bubble. `>= 0` sets the badge; omitted (or `KEEP_BADGE_COUNT`) keeps the current count. No-op on iOS / without overlay permission. |
| `hide()` | — | `void` | Hides and stops the bubble. No-op on iOS. |
| `setIcons({ bubbleIcon?, notificationIcon? })` | drawable names | `void` | Configures icons resolved from the host app's resources. Empty/unknown → library default. |

### `KEEP_BADGE_COUNT`

```ts
import { KEEP_BADGE_COUNT } from 'react-native-android-floating-bubble';
// KEEP_BADGE_COUNT === -1
```

A sentinel (`-1`) kept in sync between JS and native. Calling `show()` with no argument
sends `-1`, which tells the native side to **keep the current badge count** instead of
resetting it — useful for toggling/bootstrapping the bubble without losing the count.

## iOS behavior

All methods are no-ops; `isSupported()` returns `false` and `hasOverlayPermission()`
returns `false`. Safe to call from shared code without platform checks.

## Troubleshooting

- **Bubble never appears** → overlay permission not granted. Call
  `requestOverlayPermission()` and verify with `hasOverlayPermission()`. The bubble also
  only shows while the app is in the background.
- **`RemoteServiceException: did not then call Service.startForeground()`** → an FGS
  special-use issue on Android 14+. The library calls `startForeground` on every start to
  prevent it; if you see it, confirm the merged manifest still contains the `<service>`
  with `foregroundServiceType="specialUse"`.
- **Nothing happens in Expo Go** → expected. Expo Go cannot load native modules; use
  `expo prebuild` + a dev client.

## Testing / running the example

```sh
yarn
yarn example android   # build & run the example app on Android (use Android 14+)
```

The example app exercises `show`/`hide`, badge values, `KEEP_BADGE_COUNT`, and the
overlay-permission flow.

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT
