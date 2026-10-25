# 검증 범위

- 서명 위조·잘못된 키·잘못된 서버 채널/팩/파일명/주소와 비정상 크기 거부.
- 손상·잘림·초과 다운로드 거부, 기존 JAR·수동 변경 보존과 백업 검증.
- 임시 Java 부모 프로세스가 살아 있는 동안에는 원본을 유지하고, 실제 종료 후 별도 도우미가 교체하는 통합 실험.
- 한글·공백 경로에서의 파일 처리 및 화면 배치 경계 검사.
- 월드·플레이어가 없는 실제 메인 메뉴 상태에 대응하는 회귀 검사, 로딩/다른 GUI 대기와 시작·메뉴 콜백 연결 검사.
- 신규 bytecode와 기존 누적 클래스/믹신·진입점·라이선스 보존 검사.
- 공개 GitHub 피드를 통해 내려받은 서명과 JAR의 재검증.

테스트는 임시 폴더와 도우미 프로세스만 사용합니다. Minecraft 클라이언트·서버·월드를 실행하지 않습니다. 새 게임 화면의 실제 렌더링/버튼/종료 흐름은 적용 후 별도의 게임 검수가 필요합니다. 검증 결과를 게임 내 모든 기능 검수 완료로 해석하지 않습니다.

build6의 flush만 적용한 화면은 사용자 스크린샷에서 겹침이 지속돼 실패로 확인됐습니다. build7은 부모 Screen.render 호출 자체를 제거하고 업데이트 두 화면만 FancyMenu 공개 blacklist rule에 등록합니다. `tools/verify.py --fancymenu FANCYMENU_JAR`로 설치된 실제 ScreenBlacklistRule 인터페이스와의 연동·대상 범위를 확인할 수 있습니다. 부모를 그리지 않는 컴파일 경계 검사는 기존 build6에서 실패하고 build7에서 통과합니다. 이 검사는 실제 GPU 화면 검수를 대신하지 않습니다.
# Client shader parser cache lifetime (build 2026102508)

Heap ownership was checked with Eclipse MAT, alongside a client JFR recording. Iris' concrete-syntax parser cache retained 650,925,728 bytes in the observed post-travel snapshot. This is diagnostic evidence, not a measured post-fix RAM reduction or a claim about GPU memory.

The native GLSL parser tests cover 180 real programs before/after release, retained small AST snippets, unchanged cache object/capacity/eviction policy, repeated release and uncached operation (192 checks). Separate checks exercise known/modified/missing fingerprints and explicit opt-out. The shipped Iris class was transformed offline using Mixin 0.8.7: 474 original constructor calls stay in order, the successful RETURN invokes cleanup, and 87 methods pass ASM analysis. The offline transformer intentionally bypasses the loader-specific mod guard; the fingerprints and opt-out have separate checks.

No Minecraft, world or OpenGL instance was started by these tests. Real client activation, RSS/post-GC comparisons and uninterrupted first-join timing remain pending. One capture-only heap-dump pause overlapped the start of the diagnostic join; that interval is excluded, and full join speed is not inferred from this recording.

## Measured follow-up and backpack listener lifetime (build 2026102509)

After an actual user restart and dimension travel, cache release was present in the game log. MAT observed the targeted parser cache at 6 entries / 30,318,000 retained bytes, compared with 303 entries / 650,925,728 bytes in the earlier session. This is about 592 MiB less retained by that cache; it does not establish a whole-process RAM reduction or a controlled join-time improvement. The background recorder failed to attach for this run, so there is no first-join JFR for it. The process query was repaired and tested against the actual master PID; the missing recording is not treated as a successful collection.

The next fix replaces backpack constructor-captured global unload callbacks with two native Fabric event bridges and weak identity targets. Live targets retain their registration order. Every surviving target receives the original native callback body with the original arguments; no block/chunk inventory behavior is reimplemented. Collected targets and weak keys are purged. The target version and backpack/Fabric class fingerprints gate the mixin.

253 checks exercise real Fabric event invokers, live callbacks, duplicate and ordered registration, concurrent registration, actual target collection, and guard fallbacks. Offline transformation of the installed backpack class verifies both constructor registration replacements, preservation of 35 original method call bodies and the two native callback invokers; 41 methods pass ASM analysis. All 352 preexisting compatibility classes and the updater helper remain byte-identical. Minecraft/OpenGL/world execution of this backpack fix remains pending.

## Obsolete client sound/player owners (build 2026102510)

The backpack module activated in the user's next game. The post-play heap contains the weak registry at about 7 KiB; remaining old worlds have independent sound, tooltip and key-state roots. A valid 90-second post-join JFR was collected separately. The automatic startup recording belongs to an earlier launch that exited after 33 seconds with an ImmediatelyFast dependency error, so it is not a first-join profile of the subsequent successful launch.

On client world/player transitions, exact-guarded cleanup unlinks only obsolete Aquamirae blizzard sounds, Bumblezone flying sound entries and Sortilege client tooltip owners. Current-world ambience and the current player are preserved. It never clears server players. Optional integration failures disable only the affected cleanup. Transition state uses weak references, and idle frames do not iterate the maps.

131 tests exercise a still-retained transition tracker releasing old objects, disconnect/reconnect/respawn, current ambience preservation, stop reentrancy and the actual pinned native field shapes without initializing game worlds. New behavior still requires game sound and memory verification after restart. Native Industrialization key-state and MoreMobVariants allocation candidates remain separate, unfixed work; no performance improvement is claimed for those paths.

## BetterDays client-world lifecycle (build 2026102511)

A valid uninterrupted five-minute startup/join/travel JFR was collected. Heap dumping and MAT were deferred until after recording. Runtime logs confirm the previous three sound/tooltip cleanups; the remaining worlds have independent renderer, item-holder, footsteps and time-interpolator owners.

BetterDays 3.3.4.5 calls onWorldLoad(oldWorld) from both Minecraft setLevel HEAD and clearLevel HEAD. This overwrites the new world's constructor-created interpolator and retains an obsolete world on disconnect. A low-priority client marker rewrites exactly those two merged, source-identified handlers to the existing onWorldUnload method. Four exact native fingerprints and the Minecraft/mod versions guard activation. A shape mismatch leaves both original handlers untouched.

The offline native Mixin fixture proves exactly two invocation-name changes and otherwise byte-identical class bodies, preserves the new-world constructor registration, and ASM-verifies 227 client methods. Seven lifecycle probes execute the actual native callback bytecode after type remapping to inert fixture objects: the original behavior reproduces the bug, while the correction preserves a new interpolation target and clears an old one. Seven malformed-shape cases leave native bytes intact. Fourteen checks cover valid, missing and modified actual native fingerprints. No Minecraft world is instantiated or initialized.

Reproduce with `python tools/verify_betterdays_native.py --build-dir BUILD --classpath CLASSPATH_JSON --java-home JDK_HOME`; the classpath must contain the installed pinned BetterDays, intermediary Minecraft, Mixin and ASM dependencies used for the build. The fixture deliberately bypasses FabricLoader discovery; actual class fingerprints are tested separately. Real post-restart activation and RAM reduction remain pending. Existing compatibility classes and updater helper must still compare byte-identically against the immediately previous cumulative JAR.

## Obsolete interaction/music holders (build 2026102512)

The next valid five-minute pre-connection recording and separate post-recording MAT heap confirm that the BetterDays interpolator now references the actual current world. Independent old-world roots remain. Two observed paths are the Porting Lib client interaction manager's captured block-click event (old local player) and Immersive Melodies' singleton concurrent progress map (entity keys). The native GeckoLib4 mob-renderer base already clears its animatable, so no blanket change to that renderer or its normal cache was introduced.

An additional client-only initializer detects world/player changes with the existing weak transition tracker. It clears a captured event only when its player is an obsolete ClientPlayerEntity, with an identity recheck to preserve a new event. It conditionally removes only ConcurrentMap entries whose entity belongs to an obsolete ClientWorld. Native current-world music, current input and server-player/world entries are preserved. The integrations require reviewed versions and four exact native fingerprints, then validate runtime field/API shapes. Optional failures disable the affected integration. There are no new mixins or per-frame map scans, and no forced GC in game code.

34 new checks use the actual native MelodyProgressManager concurrent-map storage, current state preservation, native replacement from another thread, reentrant event replacement, actual old-world fixture collection while the holders remain alive, and native class fingerprints/field shapes. Existing tests and ASM verification pass. All previous 363 compatibility classes, the updater helper and prior non-build entries remain byte-identical. Runtime RAM savings of this new module are unmeasured. Diagnostic heap dumps and JFR/log files remain local.

## Combined memory batch (build 2026102513)

Reuse valid captures and implement independently justified fixes as one guarded batch before asking the player for one restart/combined measurement. Do not restart the client after every small fix. No additional game capture was requested while implementing this batch. Post-recording heap saving is followed immediately by permission to close the game; offline analysis does not require it running.

Six independently version/fingerprint-gated client modules cover: PresenceFootsteps queue scheduling/one-shot consumption; obsolete Azure entity-render owners; duplicate GeckoLib/AzureLib Armor contexts; a weak Aquamirae walk-query supplier using the original calculation; global EMPTY ItemStack holder prevention; and MMV visual-only VariantID projection. The native GeckoLib4 entity renderer already cleans its normal animatable and is not changed. Current client contexts and server-world objects are preserved. Render registry keys are weak; scans run only on world/player changes. There is no in-game forced GC, settings change or dedicated-server update.

`tools/verify_batch_memory_native.py --build-dir BUILD --classpath CLASSPATH_JSON --java-home JDK_HOME` applies real native MMV and both native armor mixins together with the batch to 17 installed classes. It verifies280methods, all11visual NBT rewrites with otherwise identical callback bytecode, native Azure render/native Aquamirae walk calls, and both uniquely renamed armor owner fields.41behavior checks use the actual transformed footstep queue with a controlled clock and inert input/render fixtures: the original bug reproduces, fixed sounds are consumed once, enqueue-after-empty and late/immediate behavior work, current/server contexts survive, duplicate old armor contexts and stacks clear, and an old fixture world collects while its layer remains alive. The fixture instantiates no Minecraft world or OpenGL context. Native helper/data/fingerprint tests add160checks, including real NBT/MobVariant values, live variant changes, fallback paths, owner collection and all37fingerprints.

The native fixture bypasses FabricLoader discovery; dependency versions/hashes are checked separately. Full-pack integration, actual new-module activation and post-fix RAM savings remain unverified until the single combined user run. All previous366classes and updater helper are byte-identical. No JFR/heap/log files are uploaded. A MAT compressed random-seek failure in the last snapshot was resolved offline by CRC-validating/decompressing the existing gzip heap and rereading it; no replacement capture was needed.
