# Implementation Plan — SPEC-CONTENT-323

> Source: GitHub Issue #323, 2026-11-05T01:25:03Z comment (authoritative breakdown). Milestones below map 1:1 to the issue's 단계 0-5. 작업(tasks) / 검증(verification) granularity is preserved verbatim — these are the load-bearing checkboxes that acceptance.md traces back to.

## [RESOLVED] clarification markers

Below are the 3 items originally flagged `[NEEDS CLARIFICATION]` during SPEC authoring, resolved via user confirmation on 2026-11-05 before Implementation Kickoff Approval. None block run-phase entry.

- **[RESOLVED: Weekly-selection bias tolerance threshold]** — 사용자 확정: 52주 시뮬레이션에서 봇별 선택 횟수가 **평균 대비 ±20% 이내**이면 통과. (원래 질문: M2 요구사항은 "선택 횟수 편차가 사전 허용 범위 내"를 요구했으나 수치가 없었음.)
- **[RESOLVED: Final weekly-selection strategy choice]** — 오케스트레이터 판단으로 확정: 두 후보 전략(①seed 기반 결정적 셔플+과거 선택 횟수 균형 보정, ②고정 순환 순열에서 주별 창 이동) 모두 편향 기준(위 ±20%)을 통과하면 **더 단순한 ①번(seed 셔플)을 채택**한다. 둘 중 하나만 통과하면 통과하는 쪽을 채택한다.
- **[RESOLVED: New-title creation decision]** — 사용자 확정: **run-phase 진입 전에 미리 정하지 않고, M4 구현 시점에 CMS의 활성 DEFAULT 칭호 목록을 직접 확인한 뒤 결정**한다. 쓸 만한 칭호가 있으면 그걸 사용하고, 없으면 그 자리에서 새 칭호 생성 여부를 정한다. (명시적으로 "결정을 미루기"로 확정된 것이며, 미해결 상태가 아니다.)

## Milestone M0 — 기준선과 데이터 확인 (단계 0)

**Why first**: 이후 모든 마일스톤(페르소나 분리, 닉네임 교체, 선택 전략 변경, 칭호 지급)이 현재 데이터 상태에 대한 가정 위에 서 있다. 가정이 틀리면 뒤 마일스톤 전체가 재작업된다 — decision-reversibility가 가장 높은 단계.

작업:
- `member.is_bot=true` 활성 봇 수/ID/social_id/nickname/성별/연령/MBTI 조회
- 봇별 게시글·댓글 수와 최근 실행의 연속 생성 패턴 확인
- 봇 닉네임과 일반 회원 닉네임 중복 여부 확인
- 활성 DEFAULT 칭호와 봇별 `member_profile_title` 보유/장착 상태 확인
- 콘텐츠 프롬프트의 관심사 매핑이 닉네임 문자열에 결합된 부분 확인 (`ContentSeedOrchestrator.java:32-41`)

산출물: 변경 전 봇/칭호 현황을 이슈에 기록. 추가 봇 계정 필요 여부는 근거 없이 늘리지 않고 M0 조사 결과로만 결정.

완료 기준(verification):
- [ ] 봇별 현재 페르소나·콘텐츠 수·칭호 상태 확인
- [ ] 닉네임 중복 여부 확인
- [ ] 새 봇 계정 필요 시에만 계정 수/마이그레이션 범위 확정

## Milestone M1 — 페르소나와 닉네임 데이터 정리 (단계 1)

**Why second**: 닉네임을 바꾸기 전에 관심사/말투 매핑을 닉네임 문자열 의존에서 분리해야 한다 — 이것이 새 데이터 모델(안정적 ID 키)을 결정하는 지점이므로 이후 전부(선택 전략, 작성자 배정, 프롬프트)가 이 모델 위에서 동작한다.

작업:
- 봇별 닉네임/성별/연령대/MBTI/관심사/말투 특징 표로 정리
- 관심사 힌트를 닉네임 기반 `Map<String,String>`(`ContentSeedOrchestrator.java:32-41`)에서 안정적인 봇 ID/social_id 기반 페르소나 설정으로 이동(닉네임을 바꿔도 관심사가 유지되도록)
- 닉네임을 컨셉 문구형 반복에서 자연스러운 사용자 이름 후보로 정리 (유명인/실존인물/개인정보 연상 금지)
- 후보 닉네임을 활성 회원 전체와 대조해 중복 시 안전하게 대체/실패하는 정책 수립
- 기존 봇의 `member.nickname`과 `member_profile.nickname`을 함께 바꾸는 멱등 마이그레이션 작성 (화면에 쓰이는 `name` 필드는 필요시만 같이 수정)
- 프롬프트에 말투 예시를 짧게 추가 (과장된 캐릭터 말투·반복 고정 문구는 피함)

검증:
- [ ] 관심사/말투 매핑이 닉네임 변경과 무관하게 유지
- [ ] 마이그레이션 재실행/기존 데이터 환경에서 중복·덮어쓰기 없음
- [ ] 후보 닉네임 상호 중복 없고 기존 회원과 충돌 없음
- [ ] 각 봇 페르소나가 생성 프롬프트에 반영되는 단위 테스트 존재

## Milestone M2 — 주간 봇 선택 전략 (단계 2)

**Why third**: `BotAccountSelector.selectActiveBots()`의 입출력 계약 변경은 재현성·편향 요구사항을 모두 만족하는 알고리즘을 먼저 결정해야 하는 설계 분기점이다 (두 후보 전략 비교 — 위 [RESOLVED] 참조).

설계:
- 기존 `BotAccountSelector.selectActiveBots()`(`BotAccountSelector.java:36-52`)의 입출력 계약과 "자동·수동 실행이 같은 주엔 같은 봇 조합" 요구 유지
- "ID 정렬 후 매주 연속 구간" 현재 정책(`floorMod` 기반, `BotAccountSelector.java:45`)을 대체
- 주차별 seed로 재현 가능하게 하고, `bots-per-run>1`이면 조합 내 순서도 섞음
- 두 후보 전략을 테스트로 비교해 편향 상한(평균 대비 ±20% 이내) 고정: (1) seed 기반 결정적 셔플+과거 선택 횟수 균형 보정, (2) 고정 순환 순열에서 주별 창 이동. 둘 다 통과하면 더 단순한 (1)을 채택한다 ([RESOLVED] 참조).
- 같은 주 반복 호출 시 결과 동일해야 함
- 날짜 경계는 기존 Asia/Seoul 월요일 주차 기준 유지
- 랜덤 seed/선택 결과 로그는 봇 ID나 민감정보 없이 최소한만

검증:
- [ ] 같은 주의 다른 날짜에서 자동/수동 실행이 동일 봇 조합
- [ ] 다음 주 선택 조합/순서가 단순 ID 순환처럼 반복 안 됨
- [ ] 최소 1년치(52주) 주차 시뮬레이션에서 봇별 선택 횟수가 평균 대비 ±20% 이내
- [ ] 봇 수가 bots-per-run 이하/0일 때 기존 경계 동작 유지
- [ ] 저장된 콘텐츠 작성자 ID가 선택된 봇 안에만 포함

## Milestone M3 — 한 실행 안의 작성자 배정 분산 (단계 3)

**Why fourth**: 오케스트레이터의 생성 순서를 바꾸는 작업이며, 목표량/재시도/실패 격리라는 기존 불변식을 깨지 않아야 하는 회귀 위험이 가장 큰 변경 지점 — M2에서 선택 전략이 확정된 뒤에 진행한다.

작업:
- 현재 `ContentSeedOrchestrator.run()`(`ContentSeedOrchestrator.java:49`)이 게시글 단계를 봇별로 전부 생성 후 상호작용 단계로 넘어가는 구조에서 "같은 봇 게시글 연속" 패턴이 어디서 생기는지 먼저 확인
- 생성 항목을 봇별 배치로 몰지 않고 항목 단위 라운드/섞인 순서로 배정(각 봇의 총 목표량과 개별 실패 격리는 유지)
- 상호작용(댓글)도 한 봇의 연속 생성을 방지 — 봇과 후보 게시글 조합을 교차 배정
- 같은 봇이 같은 게시글에 중복 참여하지 않는 기존 후보 제외/트랜잭션 제약 유지
- `runCommentsOnly()`(`ContentSeedOrchestrator.java:76`)에도 동일 분산 규칙 적용 (게시글 생성은 안 함)
- 셔플 경계에 주차 또는 실행 seed를 명시적으로 전달, 테스트는 고정 seed 사용

검증:
- [ ] 테스트에서 동일 봇의 연속 생성 항목이 2개를 넘지 않음을 확인 (REQ-006 — 3개 이상 연속 금지)
- [ ] 봇별 목표량/부족분 재시도/실패 격리가 기존대로 동작
- [ ] 기존 사용자 우선 후보 및 봇 댓글 한도 규칙에 회귀 없음
- [ ] 전체 실행과 댓글 전용 실행 모두에서 순서 분산 적용

## Milestone M4 — 봇 칭호 지급/장착 (단계 4)

**Why fifth**: 칭호 지급은 다른 마일스톤과 독립적인 데이터 보정이다. 새 칭호 생성 여부는 M4 구현 시점에 CMS를 직접 확인해 그 자리에서 정하기로 확정되었다 ([RESOLVED] 참조) — run-phase 진입 전 별도 결정이 필요하지 않다.

작업:
- 활성 DEFAULT 칭호 중 봇에 쓸 수 있는 칭호 확인 (사용자 보상/구매 전용 칭호는 임의 부여 금지)
- 적합한 DEFAULT 칭호가 없으면 그 자리에서 새 칭호 생성 여부를 정한다 (M4 구현 시점 결정 — [RESOLVED] 참조)
- 봇별 칭호를 고정 매핑해 부여하고 `member_profile_title`에 직접 보유+`equipped=true` 상태로 저장하는 멱등 데이터 마이그레이션 작성 (`TitleServiceImpl.getTitleList()`의 지연 자동지급 로직 — `TitleServiceImpl.java:61-117` — 은 사용자 API 호출 시에만 발동하므로 봇에는 적용되지 않음 — 반드시 마이그레이션으로 직접 INSERT)
- 기존 봇 프로필/부분 등록 환경에서도 안전하게 재실행 가능해야 함
- CMS 응답이 장착 칭호를 조회하는 경로를 확인해 데이터만 넣고 화면에 null로 남는 상태를 방지

검증:
- [ ] 모든 활성 봇에 유효 칭호가 장착됨
- [ ] 마이그레이션 재실행 시 중복 member_profile_title row 없음
- [ ] CMS 응답에서 칭호가 실제 표시되는 통합 테스트 존재
- [ ] 일반 회원 칭호 목록/보유/장착 동작에 영향 없음

## Milestone M5 — 통합 테스트와 테섭 확인 (단계 5)

**Why last**: 앞선 마일스톤들이 각자 단위 테스트를 포함하므로, M5는 통합 수준 검증과 수동 테스트 서버(테섭) 확인이라는 마무리 단계다 — 코드 변경이 아닌 검증 전용 마일스톤.

범위:
- 선택기 단위 테스트 (재현성 / 주별 변화 / 장기 편향 / 봇 수 경계)
- 오케스트레이터 테스트 (항목별 분산 / 목표량·재시도·실패 격리 / 댓글 전용 경로)
- 프롬프트 테스트 (관심사·말투 주입, 닉네임 의존성 제거)
- 마이그레이션 테스트 (기존 데이터 보정 / 중복 실행 멱등성 / 일반 회원 데이터 보존)
- API 통합 테스트 (CMS 응답의 봇 닉네임·칭호 표시)
- 관련 테스트 후 전체 `./gradlew test` 실행
- 테섭에서 여러 번 생성해 봇별 작성 수/연속 작성 횟수/닉네임 다양성/칭호 표시 확인

(M5 자체에 별도의 `[ ]` 체크리스트는 원문에 없음 — M1-M4의 검증 항목이 이 통합/테섭 확인으로 수렴된다. 최종 완료 조건은 acceptance.md의 최종 6개 AC로 별도 추적한다.)

## Risks

- M1에서 관심사 매핑을 ID 기반으로 옮기는 작업이 기존 콘텐츠 생성 프롬프트 호출부(`BotPersonaContext` 생성 경로, `ClaudeContentGenerator.appendPersona()`)와 결합되어 있어, 리팩터링 범위가 M1 작업 범위를 넘을 위험이 있다 — M0 조사에서 결합 지점을 먼저 확인한다.
- M2의 선택 전략 교체는 `BotAccountSelector`의 기존 호출부(스케줄러 `ContentSeedScheduler`, 관리자 수동 실행 API)와의 계약을 깨지 않아야 한다 — 입출력 시그니처 불변 유지가 전제조건.
- M3의 분산 로직은 기존 "실제 사용자 글 → 이번 실행 봇 글 → 기존 봇 글" 우선순위 주석(`ContentSeedOrchestrator.java:22-26`)을 깨지 않아야 한다 — 회귀 테스트로만 확인 가능.
- M4의 칭호 지급은 TitleServiceImpl의 지연 자동지급 로직과 동시에 동작해야 하므로, 마이그레이션이 먼저 실행된 뒤 사용자가 API를 호출해도 중복 지급이 일어나지 않는지 확인 필요 (REQ-010/011).

## Commit Strategy (issue-proposed 커밋 단위)

1. 봇 페르소나 매핑·닉네임 데이터·프롬프트 (M0+M1)
2. 주간 봇 선택 및 실행 내 작성자 배정 분산 (M2+M3)
3. 봇 칭호 데이터 보정/장착과 응답 검증 (M4)
4. 각 변경에 직접 연결되는 테스트 (M5, 각 커밋에 동반)

코드와 테스트는 같은 커밋에 포함하고, 데이터 마이그레이션은 관련 기능 커밋에 포함한다 (별도의 마이그레이션 전용 커밋을 만들지 않는다).

## Technical Approach Notes

- Development mode: TDD (`quality.yaml` `constitution.development_mode: tdd`) — 각 마일스톤은 RED(실패하는 테스트)→GREEN→REFACTOR 순서로 진행.
- 기존 공개 계약(`BotAccountSelector.selectActiveBots()`의 반환 타입, `ContentSeedOrchestrator.run()`/`runCommentsOnly()`의 시그니처)은 변경하지 않는다 — 내부 구현만 교체.
