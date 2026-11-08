-- V13에서 만든 봇 전용 칭호를 제거하고, 기존 활성 칭호를 봇별로 하나씩 장착한다.
-- 최초 실행 때만 무작위로 배정하며, 이미 다른 칭호를 장착한 봇은 유지한다.
-- 활성 칭호가 5개 이상이면 서로 다른 칭호가 배정되고, 부족하면 순환하여 중복한다.

DELETE mpt
FROM member_profile_title mpt
JOIN title t ON t.id = mpt.title_id
WHERE t.code = 'bot-curator';

DELETE FROM title
WHERE code = 'bot-curator';

INSERT INTO member_profile_title (
    created_at, updated_at, acquired_at, equipped, member_profile_id, title_id
)
WITH unassigned_bots AS (
    SELECT
        m.id AS member_profile_id,
        ROW_NUMBER() OVER (ORDER BY RAND()) AS position_number
    FROM member m
    WHERE m.social_id IN (
        'content-seed-bot-001', 'content-seed-bot-002', 'content-seed-bot-003',
        'content-seed-bot-004', 'content-seed-bot-005'
    )
      AND m.is_bot = TRUE
      AND NOT EXISTS (
          SELECT 1
          FROM member_profile_title equipped_title
          WHERE equipped_title.member_profile_id = m.id
            AND equipped_title.equipped = TRUE
      )
),
random_titles AS (
    SELECT
        t.id AS title_id,
        ROW_NUMBER() OVER (ORDER BY RAND()) AS position_number,
        COUNT(*) OVER () AS title_count
    FROM title t
    WHERE t.active = TRUE
)
SELECT
    NOW(6), NOW(6), NOW(6), TRUE, bots.member_profile_id, titles.title_id
FROM unassigned_bots bots
JOIN random_titles titles
  ON titles.position_number = MOD(bots.position_number - 1, titles.title_count) + 1
ON DUPLICATE KEY UPDATE
    updated_at = NOW(6),
    equipped = TRUE;
