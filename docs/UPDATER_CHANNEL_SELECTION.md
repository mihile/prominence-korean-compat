# Remember the selected updater channel

The previous updater already saved `track=nightly` and reloaded it on startup, but its menu reopened a generic chooser and waited for another click without indicating the selected channel. This made a saved choice appear lost. The observed master preference contained `track=nightly`; no preference file was changed by this fix.

Opening Patch updates now automatically checks the saved channel. The update detail screen shows a channel button; it opens a chooser showing the saved selection with a check mark. Choosing a channel persists it and checks that feed. The button is disabled during download or pending replacement. Automatic checks, signed channel verification, the user-approved update action, stable-return restrictions and after-exit replacement remain unchanged.

Tests inspect the compiled screen/control paths and run the signed updater and cumulative regression suites. They do not start Minecraft or prove actual screen layout or interactive use. The client fix is released on nightly, not main, until gameplay verification.
