# Acceptance Criteria — SPEC-CONTENT-323

> Every AC below is a direct Given-When-Then conversion of a `[ ]` checkbox from the GitHub Issue #323 2026-11-05 implementation-plan comment. The `stage`/`milestone` column traces each AC back to its originating 단계 (plan.md M0-M5). 26 checkboxes in the source → 26 AC entries below (M0: 3, M1: 4, M2: 5, M3: 4, M4: 4, Final: 6).
>
> **AC-count ceiling exception**: 26 exceeds the Tier M ceiling (16) and the Tier L ceiling (25). This is a documented, deliberate exception — see spec.md § AC-Count Ceiling Exception. The 26 criteria are not orchestrator-inflated; they are a 1:1 mirror of the issue's own checklist, preserved per the user's explicit "lightweight SPEC" decision.

## M0 — 기준선과 데이터 확인 (plan.md Milestone M0)

**AC-001** (stage: M0)
- Given 운영 DB에 `member.is_bot=true`이고 `deleted_at IS NULL`인 활성 봇 계정들이 존재할 때
- When 봇별 페르소나(성별/연령/MBTI)·콘텐츠 작성 수·칭호 보유/장착 상태를 조회하면
- Then 모든 활성 봇에 대해 현재 페르소나·콘텐츠 수·칭호 상태가 누락 없이 확인된다.

**AC-002** (stage: M0)
- Given 활성 봇 닉네임 목록과 일반 회원 닉네임 전체가 주어졌을 때
- When 두 집합을 대조하면
- Then 봇 닉네임과 일반 회원 닉네임 사이의 중복 여부가 명확히 확인된다 (중복이 있다면 그 목록이 산출된다).

**AC-003** (stage: M0)
- Given M0 조사 결과(현재 봇 수, 페르소나 다양성, 닉네임 중복 현황)가 산출되었을 때
- When 추가 봇 계정이 필요한지 판단하면
- Then 그 판단은 M0에서 확인된 근거에만 기반하며, 추가 봇 계정이 필요한 경우에만 계정 수와 마이그레이션 범위가 확정된다 (근거 없이 계정 수를 늘리지 않는다).

## M1 — 페르소나와 닉네임 데이터 정리 (plan.md Milestone M1)

**AC-004** (stage: M1)
- Given 봇의 관심사/말투 매핑이 봇 ID(또는 social_id) 기반 페르소나 설정으로 이동되었을 때
- When 해당 봇의 닉네임을 변경하면
- Then 그 봇의 관심사/말투 매핑은 닉네임 변경과 무관하게 그대로 유지된다.

**AC-005** (stage: M1)
- Given 봇 닉네임 마이그레이션이 이미 한 번 적용된 환경이 있을 때
- When 동일한 마이그레이션을 재실행하면
- Then 기존 봇 데이터나 무관한 일반 회원 데이터에 중복 생성이나 의도치 않은 덮어쓰기가 발생하지 않는다.

**AC-006** (stage: M1)
- Given 새로 정리된 후보 닉네임 목록이 주어졌을 때
- When 후보 닉네임들을 상호 비교하고 활성 회원 전체 닉네임과 대조하면
- Then 후보 닉네임 간 중복이 없고, 기존 회원 닉네임과도 충돌이 없다.

**AC-007** (stage: M1)
- Given 각 봇의 페르소나(관심사/말투)가 설정되어 있을 때
- When 해당 봇에 대한 콘텐츠 생성 프롬프트가 구성되면
- Then 그 프롬프트에 페르소나가 반영되었음을 검증하는 단위 테스트가 존재하고 통과한다.

## M2 — 주간 봇 선택 전략 (plan.md Milestone M2)

**AC-008** (stage: M2)
- Given 자동 실행(스케줄러)과 수동 실행이 같은 ISO 주(Asia/Seoul, 월요일 기준) 내의 서로 다른 날짜에 호출될 때
- When `BotAccountSelector.selectActiveBots(referenceDate)`를 각각 호출하면
- Then 두 호출 모두 동일한 봇 조합을 반환한다.

**AC-009** (stage: M2)
- Given 여러 주에 걸친 봇 선택 결과가 누적되었을 때
- When 주차가 바뀔 때마다의 선택 조합과 조합 내 순서를 관찰하면
- Then 그 결과가 단순 ID 순차 순환(고정 폭 슬라이딩 윈도우)처럼 반복되지 않는다.

**AC-010** (stage: M2)
- Given 최소 1년(52주) 분량의 주차를 시뮬레이션했을 때
- When 봇별로 선택된 횟수의 편차를 측정하면
- Then 그 편차가 **평균 대비 ±20% 이내**에 든다.

**AC-011** (stage: M2)
- Given 활성 봇 수가 `bots-per-run` 설정값 이하이거나 0인 경계 상황일 때
- When `selectActiveBots()`를 호출하면
- Then 기존 경계 동작(0개 반환 또는 봇 수만큼만 반환)이 변경 없이 유지된다.

**AC-012** (stage: M2)
- Given 특정 주차에 선택된 봇 조합이 주어졌을 때
- When 그 주차의 실행 결과로 저장된 콘텐츠(게시글/댓글)의 작성자 ID를 조회하면
- Then 모든 작성자 ID가 그 주차에 선택된 봇 집합 안에만 포함된다.

## M3 — 한 실행 안의 작성자 배정 분산 (plan.md Milestone M3)

**AC-013** (stage: M3)
- Given `ContentSeedOrchestrator.run()`이 여러 봇에 대해 게시글/상호작용 생성 작업을 배정할 때
- When 생성 작업의 실행 순서를 테스트로 관찰하면
- Then 동일 봇의 작업이 **연속 2개를 넘지 않도록**(3개 이상 연속 금지, REQ-006) 항목 단위로 분산되어 있음이 확인된다 — 단, 그 배치 내 봇별 유효 항목 수 분포 `[maxCount, rest...]`가 `maxCount <= 2 * (total - maxCount + 1)`(스케줄링 cooldown 하한)을 만족하는 수학적으로 가능한(feasible) 경우에 한한다. 이 부등식이 거짓인 극단적 편차에서는 수학적으로 가능한 최선(`ceil(maxCount / (total - maxCount + 1))` 연속)까지만 요구한다.

**AC-014** (stage: M3)
- Given 작성자 배정 분산 로직이 적용된 이후에도
- When 특정 봇의 목표량에 부족분이 발생하거나 다른 봇의 생성이 예외로 실패하면
- Then 부족분에 대한 최대 1회 재시도와 봇 단위 실패 격리가 기존과 동일하게 동작한다.

**AC-015** (stage: M3)
- Given 실제 사용자 글 우선 후보 선정 규칙과 봇 댓글 상한 규칙이 기존에 존재할 때
- When 작성자 배정 분산 로직을 적용한 뒤 동일 시나리오로 회귀 테스트를 실행하면
- Then 두 규칙 모두에서 회귀(동작 변화)가 발생하지 않는다.

**AC-016** (stage: M3)
- Given 전체 실행 경로(`run()`)와 댓글 전용 실행 경로(`runCommentsOnly()`)가 각각 호출될 때
- When 두 경로 각각에서 생성 작업 순서를 관찰하면
- Then 두 경로 모두에서 동일한 작성자 배정 분산 규칙이 적용되어 있다.

## M4 — 봇 칭호 지급/장착 (plan.md Milestone M4)

**AC-017** (stage: M4)
- Given 봇 칭호 데이터 마이그레이션이 적용된 이후
- When 모든 활성 봇 계정의 장착 칭호 상태를 조회하면
- Then 모든 활성 봇에 유효한 칭호가 장착되어 있다.

**AC-018** (stage: M4)
- Given 봇 칭호 마이그레이션이 이미 한 번 적용된 환경이 있을 때
- When 동일한 마이그레이션을 재실행하면
- Then `member_profile_title` 테이블에 중복 row가 생성되지 않는다.

**AC-019** (stage: M4)
- Given 봇 계정에 칭호가 직접 INSERT로 부여되어 있을 때
- When CMS가 해당 봇 회원의 프로필 응답을 조회하면
- Then 응답에 장착된 칭호가 null이 아닌 실제 값으로 표시됨을 검증하는 통합 테스트가 존재하고 통과한다.

**AC-020** (stage: M4)
- Given 일반(비봇) 회원의 칭호 목록 조회/보유/장착 흐름(`TitleServiceImpl.getTitleList()`의 지연 자동지급 포함)이 기존에 존재할 때
- When 봇 칭호 마이그레이션과 선택 전략 변경을 적용한 뒤 동일 흐름을 실행하면
- Then 일반 회원의 칭호 목록/보유/장착 동작에 영향이 없다.

## Final — 최종 완료 조건 (plan.md 전체, issue 원문 "최종 완료 조건")

**AC-021** (stage: final)
- Given M1의 페르소나·닉네임 데이터 정리가 완료되었을 때
- When 모든 활성 봇의 페르소나와 닉네임을 상호 비교하고 전체 회원 닉네임과 대조하면
- Then 봇 간 페르소나·닉네임이 서로 구별되고, 일반 회원과의 충돌도 없다.

**AC-022** (stage: final)
- Given M2에서 채택된 주간 봇 선택 전략이 적용되었을 때
- When 여러 주에 걸친 선택 조합을 재현성(같은 주 동일 결과)과 예측 가능성(단순 ID 순환 여부) 두 기준으로 검증하면
- Then 조합은 재현 가능하면서도 단순 ID 순환보다 예측 가능성이 낮고, AC-010의 편향 기준을 만족한다.

**AC-023** (stage: final)
- Given M3의 작성자 배정 분산 로직이 적용된 실행 결과가 있을 때
- When 실행 내 동일 봇의 연속 작성 횟수를 분산 적용 전/후로 비교하면
- Then 한 봇이 몰아서 작성하는 패턴이 감소했음이 확인된다.

**AC-024** (stage: final)
- Given M4의 봇 칭호 마이그레이션이 적용되었을 때
- When CMS에서 모든 활성 봇의 칭호 표시 여부를 확인하면
- Then 모든 봇 칭호가 CMS에서 null 없이 확인된다.

**AC-025** (stage: final)
- Given M0-M4의 모든 변경이 적용되었을 때
- When 관련 모듈 테스트와 전체 `./gradlew test`를 실행하면
- Then 관련 테스트와 전체 테스트가 모두 통과한다.

**AC-026** (stage: final)
- Given M0-M4의 모든 변경이 테스트 서버(테섭)에 배포되었을 때
- When 테섭에서 콘텐츠 생성을 여러 번 트리거하여 봇별 작성 수, 연속 작성 횟수, 닉네임 다양성, 칭호 표시를 수동 확인하면
- Then 수동 확인 결과가 AC-021~AC-024의 기대와 일치함이 기록된다.

## Edge Cases (추가 — issue 원문 외 보강)

- 활성 봇이 0개인 경우 `selectActiveBots()`는 빈 리스트를 반환하고 오케스트레이터는 생성 작업을 스킵한다 (AC-011과 연결).
- 닉네임 후보 풀이 모두 소진되어 신규 후보를 생성할 수 없는 경우, 마이그레이션은 안전하게 실패(no-op)하고 기존 닉네임을 보존한다 (AC-006의 "안전하게 대체/실패" 정책).
- DEFAULT 칭호가 비활성화(`active=false`)된 상태에서 마이그레이션이 재실행되는 경우, 비활성 칭호는 후보에서 제외된다 (AC-017/018과 연결).

## Definition of Done

- [ ] AC-001 ~ AC-026 전부 PASS
- [ ] plan.md의 [RESOLVED] 항목 3건(편향 허용치 ±20%, 선택 전략 tie-break, 칭호 생성 결정 시점)이 모두 사용자 확인으로 해소됨 (2026-11-05 완료)
- [ ] `./gradlew test` 전체 그린
- [ ] 테섭 수동 확인 기록 완료 (AC-026)
- [ ] 일반 회원 흐름 회귀 없음 (AC-015, AC-020)
