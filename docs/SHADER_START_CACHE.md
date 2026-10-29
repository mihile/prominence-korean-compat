# Shader preprocessing cache retention and visibility

## Retired in nightly-20261029.8

The 170748 capture showed zero source-memo reuse and almost no shader-text disk hits during dimension travel, with 186–408 ms spent preparing cache input per pipeline. The runtime JCPP interception is now unregistered. Native preprocessing runs directly; pipeline timing remains, without opening the old cache or starting its writer. Existing disk files are left untouched. The earlier implementation and checks remain as historical source, not active optimization. Startup did have cache hits, so restoring native preprocessing may increase menu preparation; no overall speedup is claimed without a gameplay capture. See JOIN_TRANSITION_REVIEW.md for the other retired hooks and lifecycle tradeoffs.

## Historical implementation

The 21:34 capture recorded 32 cache hits, 126 misses and 57 completed writes when the first pipeline report was emitted; those are process-cumulative counters, and writes may still be pending. At inspection the existing cache held 127.8 MiB against its 128 MiB limit. Neither observation proves eviction caused the specific misses or quantifies the time saved by a larger cache.

The existing exact-input, warning-free shader text cache now allows up to 384 MiB and 3072 entries on disk, with at most 16 MiB and 64 queued asynchronous writes. Previously those limits were 128 MiB/1024 entries and 4 MiB/16 queued writes. This is bounded extra disk and transient queued heap, used only when shader cache entries actually exist. Existing SHA-256 inputs, guarded Iris/JCPP hashes, diagnostics, corruption fallback, native return values and world/GPU pipeline behavior remain unchanged. No shader data is cached when preprocessing reports errors or warnings.

New process-cumulative counters distinguish dirty outputs, oversized entries, queue rejection, write errors, evictions, queued jobs and pending bytes. These make future performance decisions measurable without recording shader text or names. This does not cache Iris AST-transformed results or compiled GPU programs, and it is not claimed to remove all first-join stalls.

The actual installed Iris/JCPP transformed method and installed shader pack include graph were exercised with real compiler output in the headless native fixture; bounded-cache tests include a blocked writer burst of 32 items, persistence, corruption, invalidation and diagnostic paths. Existing update/reload/LOD suites pass. No Minecraft world or GL was started and actual frame time or cold restart effect has not been measured. On the first run, work still must be performed to create missing entries; later runs can reuse eligible entries. The writer remains single-threaded and directory scan per write remains, so the new counters must be reviewed before claiming the cache churn has been solved.
