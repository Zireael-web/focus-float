# Pixel 7 / Shizuku Spikes

These need verification on the target Pixel 7. They are intentionally not hidden
behind guesses in the code.

1. Confirm that `cmd package suspend --user 0 --dialogMessage "Paused until midnight" <package>`
   suppresses notifications in the expected way on the installed Android version.
2. Check which system dialog appears when a suspended app is launched from
   FocusFloat, recents, Settings, or an external notification.
3. Confirm `ApplicationInfo.FLAG_SUSPENDED` reflects packages suspended by the
   shell caller on the target OS.
4. Check whether suspension survives reboot on the target OS.
5. Collect packages that Pixel 7 refuses to suspend even from shell, then add
   any safety-critical packages to `ProtectedPackages`.
6. `Shizuku.newProcess()` is private in Shizuku API 13.1.5. The MVP shell uses
   a reflective call so the Pixel 7 path can be tested quickly. Replace it with
   a Shizuku UserService if reflection is blocked or process handling is flaky.
7. Verify exact alarm permission behavior on the target Android version. If the
   user does not grant it, WorkManager + launch reconcile remains the fallback.
