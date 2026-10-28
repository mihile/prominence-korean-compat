# 클라이언트 로그 확인

성능·크래시·화면 문제를 조사할 때 현재 인스턴스의 `minecraft/logs/latest.log`도 함께 확인한다. 프로파일 폴더의 저장 로그와 현재 로그는 다른 실행일 수 있다.

실행 빌드·모드·셰이더·설정 변경 시점, ERROR/WARN, 반복 메시지 및 출력량을 확인한다. 여러 줄 리소스 누락 목록도 집계한다. 로그를 지우거나 무조건 억제하는 대신 생산한 모드·리소스 경로를 찾는다. 출력이 많다는 사실만으로 프레임 정지 원인을 확정하지 않는다. 원시 로그는 로컬에 보관한다.

2026-10-28 조사: 해당 실행의 대량 출력은 More Armor Trims 패턴과 AllTheTrims 재질 조합을 포함한 아이템 모델의 missing textures 목록이었다. 예제 mixin 문구는 별도 Chef's Delight/OpenBlocks Elevator의 시작 코드다. ModernFix 동적 리소스 설정을 끈 실행에서 일괄 베이킹하며 문제가 드러난 정황은 있지만, 설정 비활성화 자체가 텍스처 누락을 새로 만들었다고 확정하지 않는다.


## 갑옷 장식 누락 원인과 수정 (build2026102806)

사용자는 ModernFix dynamic_resources를 끄기 전에도 같은 출력을 봤다고 확인했다. 이를 새로 만든 원인으로 취급하지 않는다.

설치된 Dynamic Trim 1.4.1은 More Armor Trims용 2D 아이콘 마스크 24개(6패턴×4부위)만 제공한다. 설치 More Armor Trims 1.5.4.1은 13패턴을 등록한다. Dynamic Trim은 TEMPLATE_IDS의 모든 패턴×재질×장비 조합을 생성한다. 그중 horizon/nihility/origin/parasite/twilight/witness/wraith용 아이콘 원본이 없어 로그의 81,784개 atlas 누락 상세 행이 이 7종에 대응한다. 착용 방어구용 3D UV 텍스처와 아이콘 마스크는 서로 다른 리소스다.

수정은 원본 PNG가 실제 활성 리소스 관리자에 존재하는 패턴만 아이템 모델 생성 및 override 참조 양쪽에 사용한다. 지원하지 않는 패턴은 기본 아이콘으로 돌아가며, worn trim 렌더링·템플릿·NBT·레시피는 변경하지 않는다. 기존 6종과 vanilla 마스크, 다른 모드의 유효한 마스크는 유지한다. 원본을 추가하는 리소스팩은 모델 reload에서 재확인하여 자동 지원된다. 검사 결과는 한 번 캐시하므로 장비×재질마다 파일을 반복 조회하지 않는다. 새 그림을 임의 제작하거나 모든 경고를 억제한 수정이 아니다.

검증은 실제 Dynamic Trim JAR의 6지원/7미지원 패턴, 부분 마스크, 소스 목록 불변성, 반복 조회 캐시, 리로드 후 추가 리소스 복구, 실제 native 모델/참조/리로드 Mixin 변환을 포함한다. 모드 버전·클래스 지문이 다르면 비활성화한다. 게임에서 로그가 실제 줄었는지와 장식 표시 여부는 다음 실행에서 확인해야 한다.

출처: Dynamic Trim 1.4.1의 TrimmableResource, DynamicTrimLoader와 packaged PNG 목록, More Armor Trims 1.5.4.1의 ModItems 및 trim_pattern/texture 리소스를 직접 대조했다. upstream: https://github.com/Andrew6rant/Dynamic-Trim/tree/1.4.1 및 https://github.com/masik16u/More-Armor-Trims-Fabric . 이전 설명의 로그 억제 대신 잘못된 리소스 참조 생성을 수정한다.
