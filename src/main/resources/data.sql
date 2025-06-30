-- Member 테이블에 테스트 사용자 삽입 (Vote를 생성할 Member가 필요합니다)
INSERT INTO MEMBER (created_at, email, name, role, social_type, social_id, updated_at)
VALUES (NOW(), 'test@example.com', 'TestUser', 'USER', 'KAKAO', '1234567890', NOW())
    ON DUPLICATE KEY UPDATE updated_at = NOW(); -- 이미 존재하면 업데이트

-- Member_Profile 테이블에 테스트 사용자의 프로필 정보 삽입
-- (ERD에 따라 member_id가 MEMBER 테이블의 id와 매핑되어야 합니다)
INSERT INTO MEMBER_PROFILE (created_at, updated_at, member_id, nickname, gender, age, mbti_ie, mbti_tf, mbti) -- 'id' 컬럼 제거
SELECT
    NOW(),
    NOW(),
    m.id, -- MEMBER 테이블의 id를 member_id로 참조
    '테스트닉네임', -- 예시 닉네임
    'M',           -- 성별 (M: 남성, F: 여성)
    'TWENTY',      -- 나이 (TEN, TWENTY, THIRTY, FORTY)
    'E',           -- MBTI I/E (I: 내향, E: 외향)
    'T',           -- MBTI T/F (T: 사고, F: 감정)
    'ENTJ'         -- MBTI 전체 유형 (e.g., ENTJ, INFP)
FROM MEMBER m
WHERE m.email = 'test@example.com'
    ON DUPLICATE KEY UPDATE -- 이미 프로필이 존재하면 업데이트
                         nickname = VALUES(nickname),
                         gender = VALUES(gender),
                         age = VALUES(age),
                         mbti_ie = VALUES(mbti_ie),
                         mbti_tf = VALUES(mbti_tf),
                         mbti = VALUES(mbti),
                         updated_at = NOW();

-- (나머지 SQL 문은 동일하게 유지)
-- Vote 테이블에 투표 삽입
-- (가장 많이 참여한 투표가 되도록 total_vote_count를 높게 설정)
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at)
VALUES ('FOOD', '오늘의 점심 메뉴 대결', 200, NOW(), (SELECT id FROM MEMBER WHERE email = 'test@example.com'), NOW())
    ON DUPLICATE KEY UPDATE total_vote_count = VALUES(total_vote_count), updated_at = NOW();

-- 삽입된 Vote의 ID를 조회 (실제 Vote ID를 가져옴)
-- (만약 이 파일이 여러 번 실행될 경우를 대비하여 EXISTS 체크)
INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '짜장면', 'A', 120, NOW(), v.id, NOW()
FROM VOTE v
WHERE v.title = '오늘의 점심 메뉴 대결'
  AND NOT EXISTS (SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'A');

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '볶음밥', 'B', 80, NOW(), v.id, NOW()
FROM VOTE v
WHERE v.title = '오늘의 점심 메뉴 대결'
  AND NOT EXISTS (SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'B');

-- 추가적인 투표 데이터 (다른 투표들도 넣어두면 /votes/best가 더 정확한 결과를 낼 수 있습니다)
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at)
VALUES ('ETC', '주말에 뭐할까?', 50, NOW(), (SELECT id FROM MEMBER WHERE email = 'test@example.com'), NOW())
    ON DUPLICATE KEY UPDATE total_vote_count = VALUES(total_vote_count), updated_at = NOW();

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '집에서 쉬기', 'A', 30, NOW(), v.id, NOW()
FROM VOTE v
WHERE v.title = '주말에 뭐할까?'
  AND NOT EXISTS (SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'A');

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '나가서 놀기', 'B', 20, NOW(), v.id, NOW()
FROM VOTE v
WHERE v.title = '주말에 뭐할까?'
  AND NOT EXISTS (SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'B');

-- CommentGroup 데이터 추가 (댓글 그룹)
INSERT INTO COMMENT_GROUP (created_at, updated_at, vote_id, total_comment_count)
SELECT NOW(), NOW(), v.id, 0
FROM VOTE v
WHERE v.title = '오늘의 점심 메뉴 대결'
  AND NOT EXISTS (SELECT 1 FROM COMMENT_GROUP WHERE vote_id = v.id);

INSERT INTO COMMENT_GROUP (created_at, updated_at, vote_id, total_comment_count)
SELECT NOW(), NOW(), v.id, 0
FROM VOTE v
WHERE v.title = '주말에 뭐할까?'
  AND NOT EXISTS (SELECT 1 FROM COMMENT_GROUP WHERE vote_id = v.id);

-- Comment 데이터 추가 (댓글)
-- '오늘의 점심 메뉴 대결' 투표에 댓글 추가
INSERT INTO COMMENT (created_at, updated_at, content, like_count, reply_count, is_deleted, member_id, comment_group_id, parent_id)
SELECT NOW(), NOW(), '저는 짜장면이요!', 5, 0, FALSE, m.id, cg.id, NULL
FROM MEMBER m, VOTE v, COMMENT_GROUP cg
WHERE m.email = 'test@example.com'
  AND v.title = '오늘의 점심 메뉴 대결'
  AND cg.vote_id = v.id
  AND NOT EXISTS (SELECT 1 FROM COMMENT WHERE content = '저는 짜장면이요!' AND member_id = m.id AND comment_group_id = cg.id);

INSERT INTO COMMENT (created_at, updated_at, content, like_count, reply_count, is_deleted, member_id, comment_group_id, parent_id)
SELECT NOW(), NOW(), '볶음밥도 맛있는데...', 3, 0, FALSE, m.id, cg.id, NULL
FROM MEMBER m, VOTE v, COMMENT_GROUP cg
WHERE m.email = 'test@example.com'
  AND v.title = '오늘의 점심 메뉴 대결'
  AND cg.vote_id = v.id
  AND NOT EXISTS (SELECT 1 FROM COMMENT WHERE content = '볶음밥도 맛있는데...' AND member_id = m.id AND comment_group_id = cg.id);

-- '주말에 뭐할까?' 투표에 댓글 추가
INSERT INTO COMMENT (created_at, updated_at, content, like_count, reply_count, is_deleted, member_id, comment_group_id, parent_id)
SELECT NOW(), NOW(), '집에서 쉬는게 최고죠!', 10, 0, FALSE, m.id, cg.id, NULL
FROM MEMBER m, VOTE v, COMMENT_GROUP cg
WHERE m.email = 'test@example.com'
  AND v.title = '주말에 뭐할까?'
  AND cg.vote_id = v.id
  AND NOT EXISTS (SELECT 1 FROM COMMENT WHERE content = '집에서 쉬는게 최고죠!' AND member_id = m.id AND comment_group_id = cg.id);