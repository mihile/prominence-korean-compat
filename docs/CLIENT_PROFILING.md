# Reusable three-minute client measurement

2026-10-27 user decision: every new performance measurement includes **JFR, a post-recording heap dump, and Eclipse MAT**. The heap is always collected after the timed trace has been stopped/saved. It can pause the game and request a Full GC; announce this before capture, keep the game open through heap saving, then immediately tell the user it may close. MAT runs after that client exits. No agent stops/restarts the client or server.

The primary memory question is occupancy **within the target JVM's actual maximum heap** and its after-GC behavior. Read MaxHeapSize from that JVM, not an assumed 6GiB and not the launcher's display label. System available physical RAM, process working set/private commit and VRAM are different quantities. Do not use low machine-wide free RAM as proof that the client's assigned heap is full. By user preference, the default sampler records only client process metrics; no system free-RAM warning drives an optimization decision.

Use `tools/client_profiler/profile.py` instead of writing a new capture script every time. It supports the user's Windows/Prism/Fabric1.20.1 setup. Configure paths locally at `INSTANCE/ClientProfiler/settings.local.json`:

```json
{
  "runtime_java": "C:/path/to/client-jdk",
  "analysis_java": "C:/path/to/jdk-21",
  "mat_home": "C:/path/to/mat",
  "mat_heap": "3g",
  "presentmon": "C:/path/to/CX-PresentMon-2.6.1-x64.exe"
}
```

If runtime_java is omitted, the client executable's parent JDK is used. Raw artifacts default to the local Temp/prominence-profile folder. Long or non-ASCII artifact roots use a short ASCII staging root, default `C:/Temp/ProminenceProfiles`: HotSpot diagnostic commands can misencode a Unicode path even when Python/Java file APIs handle it. No user folder is moved or deleted. Personal settings and raw recordings/dumps never enter Git or update archives.

From the repository, at the requested pre-join or dimension-test starting point:

```text
python tools/client_profiler/profile.py start --instance "C:/path/to/instance" --label first-join-home
python tools/client_profiler/profile.py status --instance "C:/path/to/instance"
python tools/client_profiler/profile.py stop --instance "C:/path/to/instance"
python tools/client_profiler/profile.py analyze --run "C:/path/to/saved/run"
python tools/client_profiler/profile.py mat --run "C:/path/to/saved/run" --report "C:/path/to/additional-query.xml"
```

Default recording is **180 seconds**. `stop` ends it early but still performs the mandatory heap after saving. The start command confirms the actual named JFR is recording before inviting the player to enter. It selects only the exact Prism instance, including Prism commands that identify it via the exact `-Djava.library.path=<instance>/natives` rather than including its Minecraft working directory; a dedicated server or similarly prefixed instance is rejected. An existing JFR is not stopped. A PID/command identity check prevents attaching to a replacement process. Installed compatibility-JAR build/hash and startup activation are recorded. The workspace policy supplies build-specific messages; delayed renderer verification is marked deferred. Use `--diagnostic` only for an explicitly labeled diagnostic-disabled run, never to conceal a failed guard.

The worker also collects PresentMon frames (unique ETW session), GPU1-second data where NVIDIA tooling exists, client process CPU/memory metrics every second (JSONL and CSV), synchronized QPC/UTC, logs, and JFR GC/heap/CPU/allocation/thread evidence. It does not alter game settings or launch a world. After180seconds it saves JFR/frames first, then gzip-compresses the heap dump. Only `canCloseClient=true` confirms the heap is saved. `wait` announces that the client may close, then keeps the BAT window open to show the MAT waiting/running/failed/complete states. It reports success only after MAT completes and the report files exist. A background worker waits for this client to exit, runs streaming JFR analysis and serializes MAT jobs under the same artifact root. The wait is bounded at six hours; `mat` can resume later. MAT is capped to two processors and a configured heap.

Artifacts include `state.json`, `capture.jfr`, `frames.csv`, raw QPC frames, GPU/process CSV/JSONL, actual JVM flags, before/after heap info, `heap.hprof.gz`, `analysis/heap.tsv`, GC/CPU/render waits/execution/allocation tables, and MAT leak-suspects/overview/custom retained-owner reports. Allocation samples are not retained ownership; after-young-GC occupancy is not the same as after-Full-GC live heap. Nested retained sizes must not be summed. Histogram/leak suspects are starting points; identify current/old contexts and strong paths to GC roots before calling a leak.

For reliable random access to large dumps MAT now analyzes a CRC-checked uncompressed copy under `mat-uncompressed`, keeping the original gzip. The MAT report script is copied into that short ASCII path too. The uncompressed heap and MAT indexes require additional disk space. Do not describe its state as complete until the reports have been produced.

`frame-consistency.json` flags gaps between observed CPU-start timestamps that disagree with reported frame metrics. Loading/display/capture gaps must not be advertised as frame-time improvements. QPC timestamps are converted with the saved UTC anchor; do not apply the earlier date_time nine-hour correction to these new UTC fields.

Validation: `tests.py` checks target rejection, timestamp normalization/missing-interval detection, and atomic status. Its `--fixture-config PATH --output PATH` mode runs JFR → compressed heap → MAT end to end on a tiny disposable64MiB Java process. It verifies the real heap limit and trace-before-heap ordering, exits only its own fixture by a flag, and never launches Minecraft/server/worlds. Frame/GPU behavior in a real client remains a separate capture check. Do not replay an expensive real-game measurement merely to test the tool.

GPU samples use one normal-exit NVIDIA query per second, with a 3-second timeout, complete-row validation and flush. `gpu-summary.json` records coverage, errors and worst query wall time. Do not confuse query latency with game frame time. The profiler does not terminate an unrelated GPU process; its timed recording is still completed before heap capture.


## Console improvements (2026-11-03)

`Profile_Client.bat` uses a Korean menu: Enter/1 measures for 180 seconds or reconnects to an active job; 2 resumes the latest saved JFR/MAT analysis without another trace or dump; 3 opens the latest results; 4 copies their path. It preserves failure exit codes. `Profile_Client.bat run --seconds 120` optionally records for two minutes, while the default stays three minutes. The server tool only captures for two minutes and does not run MAT, so its completion time is not comparable with this full workflow.

The console displays elapsed/remaining recording time, heap output size, decompression progress and the latest streamed MAT log line. A completed capture still requires all three MAT report archives. Closing/reopening the console reconnects to the background worker. If analysis was interrupted after the heap was saved, use menu 2; a missing trace/heap cannot be repaired by resuming. Default recording does not silently replace an older failed analysis.

Preparation checks disk storage against an estimated three times the actual maximum heap plus 512 MiB (gzip, raw heap, MAT indexes). This is a storage estimate, not a system-RAM check or guaranteed maximum. JFR summaries reuse a compiled analyzer keyed by source/JDK path in local `ClientProfiler/.cache/summary`, while MAT retains its existing raw dump and indexes for resume. The slow first parse of a large heap remains real work; no MAT result is declared ready before it exists.

Before the trace, capture bounded logs and thread/native-memory diagnostics. `client-log-during-trace.txt` and `log-summary.json` capture the trace interval separately from post-trace heap/Full-GC work. Logs are capped at 16 MiB per snapshot/interval and report truncation/rotation; repeated messages are counted without hiding errors in the game log. NMT-disabled JVMs simply record the diagnostic response. Raw logs remain local. Process CPU is recorded per second as one-core percent and whole-machine-normalized percent, together with working set/private commit; no machine-free-RAM warning is added.

`README.txt` describes result files and current failures. The BAT template is tracked in `tools/client_profiler/Profile_Client.bat`; the installed root BAT and `ClientProfiler/profile.py`, tests, Java analyzer and report XML must match these sources. Personal settings, policies, latest-run pointers and raw captures are preserved locally.

Validation on 2026-11-03: 11 regression tests passed; isolated 64 MiB JVM completed JFR → post-trace heap → all three MAT reports, with CPU samples and streamed MAT log. Resume regenerated reports without changing trace, heap or original JFR-start log. Root BAT menu/exit and installed/source equality passed. No live Minecraft/server recording was started for this validation; real client frame/GPU coverage remains a capture-time check.

2026-11-04: menu 5 sets an integer duration of 1–30 minutes and persists `capture_seconds` in the existing local settings, preserving other keys. The menu returns after save/cancel; Enter/1 starts the configured duration. An explicit `--seconds` overrides it for one invocation without updating the saved preference. Default remains 180 seconds. Recording already in progress keeps its original duration. Menu validation, persistence/cancel and CLI override regression checks pass.

2026-11-04: startup activation checks now read daily `.log.gz` archives as well as `latest.log`, scoped to the selected JVM creation time from Windows GetProcessTimes. Archive date and each line timestamp exclude earlier launches; arbitrary old logs cannot satisfy a missing guard. Log sources/matches/read errors are recorded in `activation-before.json`, while real missing guards still block unlabelled capture. Verified against the running build 2026110307 after midnight rollover: all 32 expected messages recovered from the current launch, none from the previous launch. Fifteen regression tests pass, including rollover, previous-session exclusion and corrupt archives. No live JFR or heap dump was triggered during this read-only check.
