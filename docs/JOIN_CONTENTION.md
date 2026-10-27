# First-join contention and recipe index allocation

Client build 2026102708; Minecraft 1.20.1 Fabric. Client compatibility JAR only. Voxy/VoxyServer/LOD Only binaries, server state and user settings are unchanged.

## Evidence and limits

The prior build's early Voxy renderer suppression was active: the initial renderer was not immediately recreated in the next capture. Remaining first-join observations include a render-thread model-bakery monitor wait of 114.48ms, with Sodium workers and the Voxy model processor among observed owners. This proves contention, not that Voxy accounts for the entire stall. ModernFix 5.24.3 already checks its concurrent baked cache before synchronizing; removing its bakery lock would be unsafe.

EMI initialization took 18.987s wall time while rendering and terrain work were active. Its native recipe manager copies each input/catalyst/output stack before checking for an existing bucket. JFR allocation samples attributed an estimated 426,462,776 bytes to stacks containing ItemEmiStack.copy and recipe-manager insertion (344,825,040 bytes ItemEmiStack). These are allocation estimates over the capture, not retained memory or a promised reduction.

ModernFix upstream branch 1.20 at commit [441fe62210157ae62e925eaa2ea95c43652836ad](https://github.com/embeddedt/ModernFix/tree/441fe62210157ae62e925eaa2ea95c43652836ad) was consulted. The installed Fabric 5.24.3 bytecode is the authority; current upstream has additional model batch logic and is not assumed identical.

## Changes

- EMI's three native insertion lambdas probe an existing bucket first only for exact ItemEmiStack, no crafting remainder, the exact native fastutil map/EMI strategy and built-in default or NBT comparison. Hits add to the same ordered recipe set without making a disposable stack/NBT copy. Misses keep native copying, so the index never retains an input stack as a new key. Unknown subclasses, custom comparison functions, remainders and maps use native behavior. No recipe, predicate, tooltip or search entry is omitted. No persistent cache is introduced.
- While the existing EMI reload scope is active, Voxy's model worker finishes the current model, then yields its batch after 8ms. Its existing loop sleeps 10ms and resumes the untouched FIFO queue. EMI completion restores native draining. This is a responsiveness/throughput tradeoff: distant models can finish later during initialization. It does not cap individual model latency or promise an 8ms frame. Biome preparation, texture readback, upload draining/commit, publication of model IDs, native locks and teardown remain unchanged. In particular upload draining was deliberately not limited because IDs can be published before GPU uploads complete.
- The reusable profiler makes bounded normal-exit NVIDIA queries once per second, flushing complete CSV rows and writing a coverage/error summary. It no longer kills the looping writer and risks losing its buffered tail. Query overhead is recorded (short local smoke: four samples, zero errors, worst query wall time about 133ms). Whole-game impact/180-second coverage remains to be measured. JFR -> heap -> user exit -> MAT ordering is unchanged.

## Verification

Actual installed EMI and Voxy Mixin/MixinExtras transformation and ASM verification: two targets, 58 methods. Native uploads, enqueue, model processing and free call sequences preserved. 45 version/hash/fallback guard checks. Model slicing: 113 FIFO/progress/nested/exception/thread/clock-overflow checks; existing terrain budget 25,061 checks.

A headless native EMI fixture executes original and transformed insertion lambdas with actual ItemEmiStack.copy, EMI comparisons and fastutil. Minecraft item/NBT boundaries are probes, unused manager bootstrap/client initialization is omitted. Identical bucket contents/order, source/key separation and fallback behavior; copies 30,027 -> 27 on deliberately repetitive input. This is not a game benchmark. Existing guard/cache/updater regression suite passes. Previous 487 classes: 485 byte-identical; only WorkGuard and InitialWork.Scope changed, four new classes added; updater helper/key unchanged.

No game/world/server was started for validation. Actual FPS, retained-heap change, distant-terrain appearance and total EMI timing remain unmeasured. One combined post-update capture is needed, not a restart for each change. Inspect `recipeInsertion=enabled`, `voxyModelSlice=enabled` and `voxyModelYields` in the log, compare recipe/search availability, first-home frame timeline and eventual LOD completion. Do not treat PresentMon timestamp gaps as single frame times.

Diagnostic opt-outs: `-Dprominence.emiHome.recipeInsertion=false` and `-Dprominence.emiHome.voxyModelSlice=false`. Do not change user launch flags without a requested diagnostic run.
