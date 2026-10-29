> 과거 실험 기록입니다. 백그라운드 creative 생성·1.5초 고정 대기와 지도 전체 갱신 제한은 안정판2026103001에서 철회했습니다. 우선순위/mesh 제한도 이미 퇴역했습니다. 아래 효과 주장은 통제된 실측 결과가 아니며 현행 동작 설명으로 사용하지 않습니다. 개인 EMF 설정은 이번 검토에서 변경하지 않았습니다.

# 첫 접속 프레임 안정화 및 렌더 스레드 병목 완화

## 배경
멀티플레이 첫 접속 시 14:07:55 구간에서 4.65초 프리즈 및 36초 동안 2.9~25 FPS 수준의 극심한 프레임 드랍이 관측되었습니다.

## 주요 변경 사항
1. **EMI Creative Entries Background Offload**:
   - `dev.emi.emi.registry.EmiStackList.reload`에서 렌더 스레드로 제출되던 `lambda$reload$4` 크리에이티브 탭 엔트리 생성을 백그라운드 리로드 워커에서 직접 실행.
   - 예외 발생 시 `client.submit(supplier)`로 안전하게 폴백.
   - 렌더 스레드 1.654초 블로킹 및 4.65초 프리즈 프레임 방지.

2. **Render Thread OS Priority Elevation & Voxy Worker Throttling**:
   - `InitialWork.ensureRenderThreadPriority()`를 통해 첫 틱에서 `Render thread`의 우선순위를 `Thread.MAX_PRIORITY` (10)으로 격상.
   - `VoxyMeshRunMixin`에서 Section mesh worker 스레드의 우선순위를 `Thread.NORM_PRIORITY - 2` (3)로 설정.
   - 16코어 백그라운드 JVMCI C2 컴파일러 및 지형 메쉬 스레드로부터 렌더 스레드 CPU 기아 방지.

3. **Xaero WorldMap onRender Throttling**:
   - 초기 로딩 구간(`InitialWork.active()`) 동안 `xaero.map.MapWriter.onRender`를 250ms에 1회로 제한하여 매 프레임 발생하던 ModernFix `DynamicBakedModelProvider.get` 동기화 락 경합 및 바이옴 조회 부담 제거.

4. **EMF LOD Configuration**:
   - `retainDetailOnLowFps: false`로 저프레임 데스 스파이럴 차단, `animationLODDistance: 12`로 엔티티 수식 연산 부담 완화.
