-- 1. MEMBER 테이블에 테스트 사용자 삽입
INSERT INTO MEMBER (created_at, email, name, role, social_type, social_id, updated_at)
VALUES (
           NOW(),
           'test@example.com',
           'TestUser',
           'USER',
           'KAKAO',
           '1234567890',
           NOW()
       )
    ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 2. 투표 추가: 오늘 저녁 뭐 먹지?
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at)
SELECT 'FOOD', '오늘 저녁 뭐 먹지?', 150, NOW(), m.id, NOW()
FROM MEMBER m
WHERE m.email = 'test@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE v WHERE v.title = '오늘 저녁 뭐 먹지?' AND v.member_id = m.id
);

-- 3. 투표 옵션 A: 삼겹살
INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '삼겹살', 'A', 90, NOW(), v.id, NOW()
FROM VOTE v
         JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '오늘 저녁 뭐 먹지?' AND m.email = 'test@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION vo WHERE vo.vote_id = v.id AND vo.label = 'A'
);

-- 4. 투표 옵션 B: 초밥
INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '초밥', 'B', 60, NOW(), v.id, NOW()
FROM VOTE v
         JOIN MEMBER m ON v.member_id = m.id
WHERE v.title = '오늘 저녁 뭐 먹지?' AND m.email = 'test@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM VOTE_OPTION vo WHERE vo.vote_id = v.id AND vo.label = 'B'
);
