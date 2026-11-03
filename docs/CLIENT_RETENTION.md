# Client retained-memory cleanup

Build 2026110401 / nightly-20261104.1, cumulative over published nightly-20261103.9.

A long-running client capture retained four disconnected ClientPlayNetworkHandlers with about 1.47 GiB of recipe/item data. Strong GC-root paths identified BadPackets handlers, AE2WTLib weak-key/value cycles, Zenith anvil scratch, tooltip item keys, FTB previous screens and Azure render/model contexts. The capture's GC pause total was only 404 ms over one minute; allocation samples and retained memory were assessed separately.

## Changes

- Prune only closed, non-current client handlers from BadPackets S2C. Active/login connections survive, and delayed socket closure is retried once per second.
- Remove AE2WTLib entries owned by old client players/worlds. Preserve current and server players; skip shared scratch cleanup while an integrated server is active.
- Release Zenith anvil scratch only when its player is obsolete, after event stacks have returned. The native active repair path is retained.
- Clear recomputable Legendary Tooltips frame lookups on transitions and bound memoization at 4,096 entries (checked once per second). Keep frame/color configuration.
- Weakly track Azure rewrite contexts and both Azure armor renderer families. Release only obsolete client-world entities and associated transient render references between frames. Native populate/prepForRender restores state before rendering.
- Detach a stale inventory screen from FTB Library previous-screen navigation while keeping live screens, quest progress and other back destinations.
- Release obsolete AzureLibArmor GeoModel-owned query captures without replacing parsed variable identities or other mods' providers.
- Match SophisticatedCore ItemStackKey's native tick lifetime on multiplayer client ticks. This closes the missing client counterpart of the server tick clear; active integrated-server shared scratch is left to its native server lifecycle.
- Reuse equipped-spell lookup within one synchronous HUD render. Defensive SpellContainer copies prevent mutable-result aliasing; nested renders, nulls, exceptions and per-frame release are handled. No cross-frame inventory/player cache.

Every optional module is independently guarded by exact version/raw-class hashes. Cleanup occurs on actual transitions with two delayed retries; steady work is limited to the native tick cache and small once-per-second checks. No periodic heap walk, forced GC, recipe deletion or world-data mutation is added.

Additional large owners were reviewed: ResourcefulLib's roughly 95 MiB is resource-defined block outline geometry, not a lazy disposable map; deleting it would remove outlines. Current Voxy world/model data and shader transformation caches are retained. Raw captures and private classpaths stay local.

## Verification

- Native Mixin transformation: 6 targets, 237 ASM-verified methods, 5 constructor paths. Includes both armor implementations, previous-screen final-field transformation, and coexistence of existing HUD equipment/weapon hooks.
- Production lifecycle/data fixtures: 30 checks, including a reproduced weak-key/value cycle collected after cleanup, active/login/server preservation, late socket closure, screen/render ownership, exception/nested HUD scope, independent result copies and next-frame refresh.
- Production guard checks: 40; reject changed versions, altered bytes and missing classes. 24 exact installed fingerprints verified.
- Existing world-cache fixture: 16 checks. Existing weapon input/HUD transforms: 7 targets/135 methods, including native cooldown lifecycle.
- All 668 prior classes retain identical instructions; 667 are byte-identical, one differs only in debug metadata after recompilation. Integrated Korean resources are unchanged.

Status: offline validation complete; signed client nightly publication. Actual retained-heap reduction and gameplay behavior require an updated client capture. The 1.47 GiB baseline is a target, not a promised saving: multiple retaining paths may overlap and additional roots may remain. Server JAR and operational files are unchanged.

Startup evidence: `Client retained-owner cleanup:` and `Client frame-scoped spell container reads:`. The client profiler records these for this build. Disable an individual optional module using the existing `prominence.worldCacheCleanup.retention<Module>` system property when diagnosing a version mismatch; no launcher settings were changed.
