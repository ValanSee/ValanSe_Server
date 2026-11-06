-- REQ-002/003, M1: 콘텐츠 시드 봇 5개의 닉네임을 "판사/배심원" 같은 컨셉 문구형에서
-- 자연스러운 커뮤니티 닉네임으로 교체한다. 관심사 힌트는 더 이상 닉네임에 결합되지
-- 않는다(BotPersonaCatalog가 social_id로 관리) - 이 마이그레이션은 표시용 닉네임만
-- 바꾼다.
--
-- 안전 정책(AC-005/AC-006):
--   1. 멱등성: 각 UPDATE는 "현재 닉네임이 여전히 옛 값"인 행만 대상으로 하므로,
--      한 번 적용된 뒤에는 재실행(Flyway 안/밖 모두)해도 조건에 더 이상 매칭되지
--      않아 아무 효과가 없다.
--   2. 충돌 시 안전 스킵: 새 닉네임이 이미 다른 활성(deleted_at IS NULL) 회원의
--      닉네임과 겹치면 그 봇의 교체를 건너뛰고 기존 닉네임을 보존한다. NOT EXISTS
--      서브쿼리는 파생 테이블(derived table)로 감싸 MySQL의
--      "같은 테이블을 UPDATE 대상과 서브쿼리에서 동시에 참조할 수 없다" 제약을 피한다.
--   3. member.nickname/name과 member_profile.nickname을 함께 바꾸되, member_profile
--      쪽은 member.nickname이 이미 새 값으로 바뀐 행만 대상으로 해 두 테이블의
--      일관성을 보장한다(member 쪽이 충돌로 스킵되면 profile 쪽도 자동으로 스킵됨).

UPDATE member m
SET m.nickname = '한라봉', m.name = '한라봉'
WHERE m.social_id = 'content-seed-bot-001'
  AND m.is_bot = TRUE
  AND m.nickname = '한입만판사'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT id, nickname, deleted_at FROM member) AS m2
      WHERE m2.nickname = '한라봉'
        AND m2.deleted_at IS NULL
        AND m2.id <> m.id
  );

UPDATE member_profile mp
JOIN member m ON m.id = mp.member_id
SET mp.nickname = '한라봉'
WHERE m.social_id = 'content-seed-bot-001'
  AND m.is_bot = TRUE
  AND m.nickname = '한라봉';

UPDATE member m
SET m.nickname = '존이냐', m.name = '존이냐'
WHERE m.social_id = 'content-seed-bot-002'
  AND m.is_bot = TRUE
  AND m.nickname = '연애배심원'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT id, nickname, deleted_at FROM member) AS m2
      WHERE m2.nickname = '존이냐'
        AND m2.deleted_at IS NULL
        AND m2.id <> m.id
  );

UPDATE member_profile mp
JOIN member m ON m.id = mp.member_id
SET mp.nickname = '존이냐'
WHERE m.social_id = 'content-seed-bot-002'
  AND m.is_bot = TRUE
  AND m.nickname = '존이냐';

UPDATE member m
SET m.nickname = '휴지조각', m.name = '휴지조각'
WHERE m.social_id = 'content-seed-bot-003'
  AND m.is_bot = TRUE
  AND m.nickname = '장바구니철학자'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT id, nickname, deleted_at FROM member) AS m2
      WHERE m2.nickname = '휴지조각'
        AND m2.deleted_at IS NULL
        AND m2.id <> m.id
  );

UPDATE member_profile mp
JOIN member m ON m.id = mp.member_id
SET mp.nickname = '휴지조각'
WHERE m.social_id = 'content-seed-bot-003'
  AND m.is_bot = TRUE
  AND m.nickname = '휴지조각';

UPDATE member m
SET m.nickname = '한판더', m.name = '한판더'
WHERE m.social_id = 'content-seed-bot-004'
  AND m.is_bot = TRUE
  AND m.nickname = '숨참고승부'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT id, nickname, deleted_at FROM member) AS m2
      WHERE m2.nickname = '한판더'
        AND m2.deleted_at IS NULL
        AND m2.id <> m.id
  );

UPDATE member_profile mp
JOIN member m ON m.id = mp.member_id
SET mp.nickname = '한판더'
WHERE m.social_id = 'content-seed-bot-004'
  AND m.is_bot = TRUE
  AND m.nickname = '한판더';

UPDATE member m
SET m.nickname = '크림이', m.name = '크림이'
WHERE m.social_id = 'content-seed-bot-005'
  AND m.is_bot = TRUE
  AND m.nickname = '결정은내일'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT id, nickname, deleted_at FROM member) AS m2
      WHERE m2.nickname = '크림이'
        AND m2.deleted_at IS NULL
        AND m2.id <> m.id
  );

UPDATE member_profile mp
JOIN member m ON m.id = mp.member_id
SET mp.nickname = '크림이'
WHERE m.social_id = 'content-seed-bot-005'
  AND m.is_bot = TRUE
  AND m.nickname = '크림이';
