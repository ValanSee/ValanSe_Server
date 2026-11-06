-- REQ-010/011, M4: 콘텐츠 시드 봇 5개에게 칭호를 지급하고 장착한다.
-- TitleServiceImpl.getTitleList()의 지연 자동지급 로직(TitleServiceImpl.java:61-117)은
-- 사용자 API(칭호 목록 조회) 호출 시에만 발동하므로 봇에게는 적용되지 않는다 -
-- 이 마이그레이션이 member_profile_title에 직접 INSERT한다.
--
-- M4 구현 시점 결정(plan.md [RESOLVED] 참조): 이 저장소의 마이그레이션에는
-- title 테이블을 시드하는 기존 데이터가 없다(title은 CMS 관리자 API로만 생성됨).
-- 따라서 봇에 쓸 수 있는 기존 활성 DEFAULT 칭호가 없어, 이 마이그레이션에서
-- 새 칭호를 직접 생성한다.
--
-- active=FALSE로 생성하는 이유(AC-020 - 일반 회원 흐름에 영향 없음):
-- TitleServiceImpl.getTitleList()의 지연 자동지급은 "활성 DEFAULT 칭호" 전체를
-- findAllByActiveTrueOrderByDisplayOrderAscIdAsc()로 조회해 아직 보유하지 않은
-- 일반 회원 모두에게 부여한다. 이 칭호를 active=TRUE로 만들면 그 조회에 포함되어
-- 모든 일반 회원에게도 자동으로 부여되고 보여지게 되므로, 봇 전용으로 의도한
-- 칭호가 일반 회원 칭호 목록에까지 새어나가 AC-020을 위반한다. active=FALSE로
-- 두면 그 조회에서 제외되어 일반 회원에게는 전혀 노출되지 않는다. 반면 CMS/댓글
-- 응답의 "장착 칭호" 조회(memberProfileTitle.equipped=TRUE 기준 LEFT JOIN,
-- title.active 필터 없음 - CommentRepositoryImpl 참고)에는 title.active와 무관하게
-- 정상적으로 표시된다. 이 마이그레이션은 member_profile_title에 직접 INSERT하므로
-- getTitleList()의 활성 칭호 순회를 거치지 않는다.
--
-- 멱등성: code UNIQUE 제약으로 title 중복 생성을 막고, member_profile_title은
-- (member_profile_id, title_id) UNIQUE 제약 위반을 피하기 위해 INSERT 전에
-- NOT EXISTS로 이미 보유한 봇을 걸러낸다 - 재실행해도 중복 row가 생기지 않는다.
INSERT INTO title (code, name, description, price, tier, acquisition_type, requirement_text, active, display_order)
SELECT 'bot-curator', 'AI 큐레이터', '발란스 콘텐츠를 큐레이션하는 AI 계정입니다.', 0, 'BASIC', 'DEFAULT', NULL, FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM title WHERE code = 'bot-curator');

INSERT INTO member_profile_title (created_at, updated_at, acquired_at, equipped, member_profile_id, title_id)
SELECT NOW(6), NOW(6), NOW(6), TRUE, m.id, t.id
FROM member m
JOIN title t ON t.code = 'bot-curator'
WHERE m.social_id IN (
    'content-seed-bot-001', 'content-seed-bot-002', 'content-seed-bot-003',
    'content-seed-bot-004', 'content-seed-bot-005'
)
  AND m.is_bot = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM member_profile_title mpt
      WHERE mpt.member_profile_id = m.id AND mpt.title_id = t.id
  );
