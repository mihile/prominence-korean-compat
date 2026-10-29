# Nightly distribution

`main` remains the stable default. Work and cumulative releases are prepared on `nightly`; merge reviewed, game-validated changes into `main` explicitly. A unique `nightly-YYYYMMDD.N` prerelease stores the signed immutable asset. The nightly branch `feed/update.properties` is a signed pointer to that asset. It is committed only after a successful upload and never changes the stable `latest` release. Signed tag identity and JAR track must agree.

Build with `tools/build.py --track nightly` and a globally increasing build number; preserve and validate the existing cumulative artifact. Push the reviewed source commit on `nightly`, then run `tools/publish_nightly.py` with the same arguments as `publish.py`. Verify the downloaded prerelease signature, hash and the public raw feed after it is pushed. The signer stays outside the repository. Do not publish logs, heap dumps, classpaths or secrets. If feed publication fails after release creation, recover the existing exact signed feed after verifying assets; do not overwrite the immutable release.

The default selection is main. Only a user-initiated main selection and explicit update button can install an older stable build over an installed nightly. The helper checks the signed release track, installed track, signature, hash, backup and approved pending plan. Automatic checks never downgrade. Initial access from a legacy updater requires installing a channel-capable JAR once. An old stable build may not contain the selector; promoting this bootstrap to main requires real game validation first.


## Startup popup (nightly-20261029.6)

Automatic offers appear only at the title menu after the loading overlay has closed and one menu frame has rendered (the next ready tick, rather than the previous one-second delay). The world-join check/fallback is removed: a late network result waits for the title menu and never interrupts a singleplayer or multiplayer session. The menu's explicit update/channel controls remain.

The modal is capped at308×210 GUI units,70% of the previous440×300. A shared GPU-only copy of the menu frame remains behind a translucent dim layer. No parent Screen.render call or CPU screenshot/file write is used. Channel switching reuses the same snapshot; closing the final modal unregisters and deletes its texture. The captured background is static while the popup is open. Changelog scrolling, saved channel selection and signed post-exit installation remain unchanged.

Offline validation covers startup/world/overlay gates, layout boundaries, compiled capture/dim/release routing, snapshot ownership across channel changes, and the cumulative updater suite. Actual GPU appearance and startup timing still require in-game validation.

## Stable promotion: 2026-10-30

The user confirmed nightly20261029.16 in-game. The main promotion retains its cumulative functionality and removes the unsupported creative offload/fixed sleep and external Xaero throttle after native-bytecode review. Stable build2026103001 uses track=main; future work continues on nightly. The nightly branch feed remains its own signed prerelease pointer. Promotion details and gameplay-vs-offline verification limits are in MAIN_PROMOTION_REVIEW.md.

When stable promotion and cleanup change the released functionality, also publish the equivalent nightly artifact so users staying on nightly receive the same reviewed code. Use a new globally increasing build and a signed nightly tag; compare all classes and resource contents with stable, normalizing JSON/source formatting and allowing only channel/build metadata to differ. A source merge alone is not binary distribution synchronization.
