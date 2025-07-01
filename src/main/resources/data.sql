-- 삭제 순서 (자식 → 부모 순서 중요!)
DELETE FROM member_vote_option;
DELETE FROM comment WHERE parent_id IS NOT NULL;
DELETE FROM comment WHERE parent_id IS NULL;
DELETE FROM comment_group;
DELETE FROM vote_option;
DELETE FROM vote;
DELETE FROM member_profile;
DELETE FROM member;

-- MEMBER 삽입
INSERT INTO MEMBER (created_at, email, name, role, social_type, social_id, updated_at)
VALUES (
           NOW(),
           'mjk5949@naver.com',
           'TestUser',
           'USER',
           'KAKAO',
           '1234567890',
           NOW()
       )
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- MEMBER_PROFILE 삽입
INSERT INTO MEMBER_PROFILE (created_at, updated_at, member_id, nickname, gender, age, mbti_ie, mbti_tf, mbti)
SELECT
    NOW(), NOW(),
    m.id,
    '테스트닉네임',
    'M',
    'TWENTY',
    'E',
    'T',
    'ENTJ'
FROM MEMBER m
WHERE m.email = 'mjk5949@naver.com'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 투표 1
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at, is_deleted)
SELECT
    'FOOD',
    '오늘 저녁 뭐 먹지?',
    150,
    '2025-06-30 14:01:00',
    m.id,
    '2025-06-30 14:01:00',
    false
FROM MEMBER m
WHERE m.email = 'mjk5949@naver.com'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 투표 옵션 1
INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '삼겹살', 'A', 90, '2025-06-30 14:01:00', v.id, '2025-06-30 14:01:00'
FROM VOTE v
WHERE v.title = '오늘 저녁 뭐 먹지?'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'A'
);

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '초밥', 'B', 60, '2025-06-30 14:01:00', v.id, '2025-06-30 14:01:00'
FROM VOTE v
WHERE v.title = '오늘 저녁 뭐 먹지?'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'B'
);

-- 투표 2
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at, is_deleted)
SELECT
    'FOOD',
    '내일 점심 뭐 먹지?',
    120,
    '2025-06-30 14:05:00',
    m.id,
    '2025-06-30 14:05:00',
    false
FROM MEMBER m
WHERE m.email = 'mjk5949@naver.com'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 투표 옵션 2
INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '제육볶음', 'A', 70, '2025-06-30 14:05:00', v.id, '2025-06-30 14:05:00'
FROM VOTE v
WHERE v.title = '내일 점심 뭐 먹지?'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'A'
);

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '된장찌개', 'B', 50, '2025-06-30 14:05:00', v.id, '2025-06-30 14:05:00'
FROM VOTE v
WHERE v.title = '내일 점심 뭐 먹지?'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'B'
);

-- 투표 3
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at, is_deleted)
SELECT
    'SNACK',
    '최애 간식은?',
    80,
    '2025-06-30 14:10:00',
    m.id,
    '2025-06-30 14:10:00',
    false
FROM MEMBER m
WHERE m.email = 'mjk5949@naver.com'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 투표 옵션 3
INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '떡볶이', 'A', 40, '2025-06-30 14:10:00', v.id, '2025-06-30 14:10:00'
FROM VOTE v
WHERE v.title = '최애 간식은?'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'A'
);

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '붕어빵', 'B', 40, '2025-06-30 14:10:00', v.id, '2025-06-30 14:10:00'
FROM VOTE v
WHERE v.title = '최애 간식은?'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'B'
);

-- member_vote_option
INSERT INTO member_vote_option (member_id, vote_id, vote_option_id, created_at, updated_at)
SELECT m.id, v.id, vo.id, NOW(), NOW()
FROM MEMBER m
         JOIN VOTE v ON v.title = '오늘 저녁 뭐 먹지?'
         JOIN VOTE_OPTION vo ON vo.vote_id = v.id AND vo.label = 'A'
WHERE m.email = 'mjk5949@naver.com'
  AND NOT EXISTS (
    SELECT 1 FROM member_vote_option WHERE member_id = m.id AND vote_id = v.id
);

-- 댓글 그룹
INSERT INTO comment_group (vote_id, created_at, updated_at, total_comment_count)
SELECT v.id, NOW(), NOW(), 2
FROM VOTE v
WHERE v.title = '오늘 저녁 뭐 먹지?'
  AND NOT EXISTS (
    SELECT 1 FROM comment_group WHERE vote_id = v.id
);

-- 댓글
INSERT INTO comment (content, like_count, reply_count, is_deleted, member_id, comment_group_id, created_at, updated_at, parent_id)
SELECT
    '삼겹살 최고죠!', 10, 1, false, m.id, cg.id, NOW(), NOW(), NULL
FROM MEMBER m
         JOIN VOTE v ON v.title = '오늘 저녁 뭐 먹지?'
         JOIN COMMENT_GROUP cg ON cg.vote_id = v.id
WHERE m.email = 'mjk5949@naver.com'
  AND NOT EXISTS (
    SELECT 1 FROM comment WHERE content = '삼겹살 최고죠!' AND member_id = m.id AND comment_group_id = cg.id
);

-- 대댓글
INSERT INTO comment (content, like_count, reply_count, is_deleted, member_id, comment_group_id, created_at, updated_at, parent_id)
SELECT
    '인정합니다 ㅋㅋ', 3, 0, false, m.id, cg.id, NOW(), NOW(), c.id
FROM MEMBER m
         JOIN VOTE v ON v.title = '오늘 저녁 뭐 먹지?'
         JOIN COMMENT_GROUP cg ON cg.vote_id = v.id
         JOIN COMMENT c ON c.content = '삼겹살 최고죠!' AND c.comment_group_id = cg.id
WHERE m.email = 'mjk5949@naver.com'
  AND NOT EXISTS (
    SELECT 1 FROM comment WHERE content = '인정합니다 ㅋㅋ' AND parent_id = c.id
);