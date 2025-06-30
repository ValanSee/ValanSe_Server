-- 순서 중요 (자식 → 부모)
DELETE FROM VOTE_OPTION;
DELETE FROM VOTE;
DELETE FROM MEMBER;

-- 다시 data.sql 실행

-- 1. MEMBER 생성
INSERT INTO MEMBER (created_at, email, name, role, social_type, social_id, updated_at)
VALUES (
           '2025-06-30 14:00:00',
           'test@example.com',
           'TestUser',
           'USER',
           'KAKAO',
           '1234567890',
           '2025-06-30 14:00:00'
       )
    ON DUPLICATE KEY UPDATE updated_at = NOW();

-- -------------------------------
-- 투표 1: 오늘 저녁 뭐 먹지?
-- -------------------------------
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at)
SELECT 'FOOD', '오늘 저녁 뭐 먹지?', 150, '2025-06-30 14:01:00', m.id, '2025-06-30 14:01:00'
FROM MEMBER m WHERE m.email = 'test@example.com';

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '삼겹살', 'A', 90, '2025-06-30 14:01:00', v.id, '2025-06-30 14:01:00'
FROM VOTE v JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '오늘 저녁 뭐 먹지?' AND m.email = 'test@example.com';

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '초밥', 'B', 60, '2025-06-30 14:01:00', v.id, '2025-06-30 14:01:00'
FROM VOTE v JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '오늘 저녁 뭐 먹지?' AND m.email = 'test@example.com';

-- -------------------------------
-- 투표 2: 내일 점심 뭐 먹지?
-- -------------------------------
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at)
SELECT 'FOOD', '내일 점심 뭐 먹지?', 120, '2025-05-30 14:05:00', m.id, '2025-06-30 14:05:00'
FROM MEMBER m WHERE m.email = 'test@example.com';

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '제육볶음', 'A', 70, '2025-05-30 14:05:00', v.id, '2025-06-30 14:05:00'
FROM VOTE v JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '내일 점심 뭐 먹지?' AND m.email = 'test@example.com';

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '된장찌개', 'B', 50, '2025-05-30 14:05:00', v.id, '2025-06-30 14:05:00'
FROM VOTE v JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '내일 점심 뭐 먹지?' AND m.email = 'test@example.com';

-- -------------------------------
-- 투표 3: 최애 간식은?
-- -------------------------------
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at)
SELECT 'SNACK', '최애 간식은?', 80, '2025-06-30 14:10:00', m.id, '2025-06-30 14:10:00'
FROM MEMBER m WHERE m.email = 'test@example.com';

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '떡볶이', 'A', 40, '2025-06-30 14:10:00', v.id, '2025-06-30 14:10:00'
FROM VOTE v JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '최애 간식은?' AND m.email = 'test@example.com';

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '붕어빵', 'B', 40, '2025-06-30 14:10:00', v.id, '2025-06-30 14:10:00'
FROM VOTE v JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '최애 간식은?' AND m.email = 'test@example.com';
