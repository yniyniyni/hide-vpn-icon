# hide VPN icon (Pixel SystemUI / LSPosed)

this LSPosed module hides only the VPN icon from the pixel status bar while leaving
VPN connectivity and other vpn-related crap.

# why

because google fucked up AOSP default icon_blacklist after the september pixel update, and hiding status bar icons now impossible using adb / SystemUITuner, e.g. "adb shell settings put secure icon_blacklist vpn" now useless. fuck google.

## what the module actually does

the supplied `SystemUIGoogle` decompilation is android API 37 / version 17 and
contains both:

- the legacy `StatusBarSignalPolicy$$ExternalSyntheticLambda0` path, which calls
  `StatusBarIconControllerImpl.setIconVisibility(mSlotVpn, true)` when a VPN is
  active; and
- the newer `VpnIconViewModel` path under `statusbar.systemstatusicons`.

the old AOSP `icon_blacklist` route does not apply the VPN slot in this build, so
the module forces the legacy slot back to invisible and returns `false`/`null`
from the modern view model.

the project uses the modern libXposed api and declares its entry point, metadata,
and fixed `com.android.systemui` scope under `META-INF/xposed/`.

## build

nothing special.

```sh
./gradlew test
./gradlew assembleRelease
```

the APK is written to:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```
as always.

install it, enable it for `SystemUI` in LSPosed, then restart SystemUI or reboot.

## Logs

if a target moved in a future pixel update (because google sucks), the module logs a non-fatal message to the xposed log and continues. search for `HideVpnIcon` in the lsposed log.

# license

[WTFPL.](LICENSE)
