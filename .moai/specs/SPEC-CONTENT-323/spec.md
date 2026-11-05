---
id: SPEC-CONTENT-323
title: "AI 작성자 다채롭게 + 닉네임 자연스럽게 개선"
version: "0.1.0"
status: in-progress
created: 2026-11-05
updated: 2026-11-05
author: solbin
priority: P2
phase: "v1.1.0 target"
module: "src/main/java/com/valanse/valanse/service/ContentSeedService"
lifecycle: spec-anchored
tags: "content-seed, bot-persona, nickname, title, github-323"
tier: M
issue_number: 323
related_specs: []
---

## HISTORY

| Date | Change | Author |
|------|--------|--------|
| 2026-11-05 | Initial SPEC drafted from GitHub Issue #323 (incl. 2026-11-05 detailed implementation-plan comment) | manager-spec |
| 2026-11-05 | Resolved 3 `[NEEDS CLARIFICATION]` markers (bias tolerance, strategy tie-break, title-creation timing) per user confirmation; fixed REQ-006's undefined "agreed threshold" to 2 items; documented an explicit AC-count ceiling exception (below) | orchestrator (post plan-audit iteration 1) |
| 2026-11-05 | M1-M4 implemented on `feat/#323-ai-author-diversity` (commits `74d1adb`, `12d2601`, `0df81b4`, `57e6526`); 435/435 tests passing (independently verified); status `draft` → `in-progress`. AC-026 (테섭 수동 확인) remains manual/pending. | manager-develop (run-phase) |

### AC-Count Ceiling Exception (documented, per `.claude/rules/moai/development/manager-develop-prompt-template.md` tier-ceiling governance)

This SPEC carries **26 acceptance criteria**, exceeding the Tier M ceiling (16) and even the Tier L ceiling (25). This is a deliberate, documented exception rather than a tier-up: `acceptance.md` is a 1:1 Given-When-Then conversion of the 26 `[ ]` checkboxes already authored by the user in GitHub Issue #323's 2026-11-05 implementation-plan comment. Consolidating them to fit the ceiling would discard the issue's own granularity and break the traceability the user explicitly asked to preserve (see the "가벼운 SPEC" / lightweight-SPEC decision — mirror the issue closely, do not add ceremony beyond it). Tiering up to L would add design.md/research.md artifacts the user also explicitly declined. The ceiling's purpose — preventing orchestrator-driven over-formalization — does not apply here, since the scope was authored by the user, not inflated by the SPEC-authoring process.

## 1. Goal

AI가 생성하는 게시글/댓글의 작성자(봇) 페르소나와 닉네임이 단조롭고 봇처럼 느껴진다는 피드백(이슈 #265 ContentSeedService 기반)에 대응하여, (1) 봇 페르소나·닉네임의 다양성과 자연스러움을 높이고, (2) 한 실행 내에서 같은 봇이 콘텐츠를 몰아서 작성하는 패턴을 줄이고, (3) 주간 봇 선택 조합이 재현 가능하되 단순 ID 순환처럼 보이지 않게 하고, (4) 봇 계정에 적절한 칭호를 부여해 CMS에서 null로 보이지 않게 한다.

## 2. Background

- 원 이슈 #265가 ContentSeedService 전체(주간 게시글/댓글 자동 생성 스케줄러)를 만들었다.
- 이후 테스트 서버 피드백(PR #335)에서 AI 작성자가 단조롭고 닉네임이 부자연스럽다는 지적이 나와 이슈 #323으로 범위가 확장되었다.
- 게시 시각 분산/예약은 별도 이슈 #337로 분리되어 본 SPEC에서는 다루지 않는다.

### Current State (코드 근거)

- `member.is_bot` 플래그: `src/main/resources/db/migration/V9__add_member_is_bot_column.sql`
- 활성 봇 5개 계정 하드코딩 시드: `src/main/resources/db/migration/V10__seed_content_bot_accounts.sql` (닉네임 전량 하드코딩, `member_profile`에 gender/age/mbti만 존재, 관심사/말투 전용 컬럼 없음)
- 닉네임-관심사 결합: `ContentSeedOrchestrator.java:32-41`의 `INTEREST_HINT_BY_NICKNAME` (닉네임 문자열 키 Map) — 주석에 "MemberProfile에는 이 정보를 저장할 필드가 없어 여기서 정적으로 관리"라고 명시됨. 닉네임을 바꾸면 관심사 매핑이 끊어진다.
- 주간 봇 선택: `BotAccountSelector.java:36-52` — 기준 월요일(`REFERENCE_MONDAY = 2026-01-05`)로부터의 주차 오프셋을 `Math.floorMod(weekOffset * botsPerRun, botCount)`로 계산해 ID 정렬된 봇 목록에서 연속 구간을 선택. 같은 주 재호출 시 결과는 동일하지만, 주가 바뀔 때 창(window)이 고정 폭으로 미끄러지는 예측 가능한 순환 패턴을 보인다.
- 실행 오케스트레이션: `ContentSeedOrchestrator.java:49` `run()` — 모든 봇의 게시글 생성 단계(`generatePostsForBot`, 89행)를 전부 끝낸 뒤에야 상호작용(투표+댓글) 생성 단계(`generateInteractionsForBot`, 144행)로 넘어간다. 이로 인해 한 봇의 게시글/댓글이 실행 내에서 연속적으로 몰리는 패턴이 생길 수 있다. `runCommentsOnly()`(76행)도 동일한 상호작용 생성 경로를 공유한다.
- 칭호 체계: `Title.java`(code/name/tier/acquisitionType/price/active/displayOrder), `MemberProfileTitle.java`(Title↔MemberProfile N:1, `equipped` boolean), `TitleAcquisitionType.DEFAULT` 존재. `TitleServiceImpl.getTitleList()`(`TitleServiceImpl.java:61-117`)가 미보유 DEFAULT 칭호를 **지연(lazy) 자동 지급+자동 장착**하지만, 이는 "사용자가 칭호 목록 API를 호출할 때"만 발동한다 — 봇 계정은 이 API를 호출하지 않으므로 칭호가 영구히 비어 있다. DB 시드 마이그레이션에는 Title INSERT가 없다(DEFAULT 칭호는 CMS API로 생성됨).

## 3. Scope-in

- AI 작성자 풀 다양화 (성별·말투·관심사 등 페르소나 확장)
- AI 계정 닉네임을 실제 사용자처럼 자연스럽게 개선
- 기존 봇 페르소나/닉네임과 중복되지 않는지 확인 (활성 회원 전체 대상)
- 한 실행에서 같은 봇이 여러 게시글/댓글을 연달아 작성하지 않도록 작성자·콘텐츠 배정을 섞거나 분산
- 봇 계정에 적절한 칭호를 부여해 CMS에 null로만 보이지 않게 함
- (부수적으로) 주간 봇 선택 조합이 재현 가능하면서 단순 ID 순환보다 예측 가능성을 낮추도록 선택 전략을 재검토

## 4. Out of Scope

### Out of Scope — Post Timing/Scheduling
- 게시 시각 분산·예약 로직은 별도 이슈 #337에서 처리한다. 본 SPEC은 생성 순서(작성자 배정 분산)만 다루며, 저장/게시 시각 자체는 변경하지 않는다.

### Out of Scope — Vote Option Label Ordering
- `/votes` 선택지 A/B 순서·label 존재 여부 확인은 본 SPEC의 범위가 아니다 (이미 PR #328 계열에서 처리됨).

### Out of Scope — Response Timestamp Timezone
- API 응답의 timestamp 타임존 처리는 본 SPEC의 범위가 아니다.

## 5. Requirements (GEARS)

> `<subject>` 표기: `ContentSeedOrchestrator`, `BotAccountSelector`, `BotPersonaContext`(또는 후속 봇 페르소나 설정), `TitleServiceImpl`/마이그레이션, CMS 응답 경로를 가리킨다.

- **REQ-001 (Ubiquitous)**: The bot persona configuration shall associate interest/tone hints with a stable bot identifier (member ID or social_id), not with the bot's nickname string.
- **REQ-002 (Ubiquitous)**: The bot nickname migration shall be idempotent — re-running it on an environment with existing bot data MUST NOT duplicate or overwrite unrelated member data.
- **REQ-003 (Event-driven)**: When a candidate bot nickname is generated, the nickname-assignment process shall check it against every active member's nickname and avoid or safely replace a collision.
- **REQ-004 (Event-driven)**: When `BotAccountSelector.selectActiveBots(referenceDate)` is called multiple times within the same ISO week (Asia/Seoul, Monday boundary), the selector shall return the identical bot combination each time.
- **REQ-005 (Event-driven)**: When the reference week changes, the selector shall produce a selection sequence that is reproducible from a weekly seed but is not a fixed-stride ID rotation.
- **REQ-006 (Event-driven)**: When `ContentSeedOrchestrator.run()` or `runCommentsOnly()` assigns generation work items (posts or comments) to bots within one execution, the assignment order shall be interleaved/shuffled such that one bot's items are never scheduled in an unbroken consecutive run longer than **2 items** (i.e., 3+ consecutive same-bot items is prohibited — resolved threshold, down from the observed "3 consecutive" pattern reported in the PR #335 test-server feedback).
- **REQ-007 (Ubiquitous)**: The item-assignment distribution shall preserve each bot's existing per-bot target quantity, shortfall-retry (max 1 retry), and per-bot failure isolation behavior.
- **REQ-008 (Event-driven)**: When the content-generation prompt is built for a bot, it shall include that bot's persona (gender/age/MBTI/interest/tone) regardless of the bot's current nickname.
- **REQ-009 (Event-driven)**: When the bot-title data migration runs, it shall grant each active bot a valid `Title` via a directly-inserted, `equipped=true` `member_profile_title` row — not via the lazy auto-grant path in `TitleServiceImpl.getTitleList()`.
- **REQ-010 (Ubiquitous)**: The bot-title migration shall be idempotent and shall not create duplicate `member_profile_title` rows on repeated execution.
- **REQ-011 (While)**: While a regular (non-bot) member's title list/ownership/equip flow is exercised, the bot-title migration and selection changes shall not alter its existing behavior.
- **REQ-012 (Event-driven)**: When the CMS reads a bot member's profile, the response shall display the bot's equipped title rather than null.

## 6. Constraints

- Asia/Seoul 월요일 기준 주차 경계는 변경하지 않는다 (기존 `BotAccountSelector` 정책 유지).
- 닉네임 후보는 유명인/실존 인물/개인정보 연상 문구를 사용하지 않는다.
- 사용자 보상/구매 전용 칭호(`TitleAcquisitionType.DEFAULT` 아님)는 봇에 임의로 부여하지 않는다.
- 랜덤 seed·선택 결과 로그에는 봇 ID나 민감정보를 포함하지 않는다(최소 로그).
