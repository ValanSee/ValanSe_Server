-- Member 테이블에 테스트 사용자 삽입 (Vote를 생성할 Member가 필요합니다)
INSERT INTO MEMBER (created_at, email, name, role, social_type, social_id, updated_at)
VALUES (NOW(), 'test@example.com', 'TestUser', 'USER', 'KAKAO', '1234567890', NOW())
    ON DUPLICATE KEY UPDATE updated_at = NOW(); -- 이미 존재하면 업데이트

-- Vote 테이블에 투표 삽입
-- (가장 많이 참여한 투표가 되도록 total_vote_count를 높게 설정)
INSERT INTO VOTE (category, title, total_vote_count, created_at, member_id, updated_at)
VALUES ('FOOD', '오늘의 점심 메뉴 대결', 200, NOW(), (SELECT id FROM MEMBER WHERE email = 'test@example.com'), NOW())
    ON DUPLICATE KEY UPDATE total_vote_count = VALUES(total_vote_count), updated_at = NOW();

-- 삽입된 Vote의 ID를 조회 (실제 Vote ID를 가져옴)
-- (만약 이 파일이 여러 번 실행될 경우를 대비하여 EXISTS 체크)
INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '짜장면 vs 짬뽕', 'A', 120, NOW(), v.id, NOW()
FROM VOTE v
WHERE v.title = '오늘의 점심 메뉴 대결'
  AND NOT EXISTS (SELECT 1 FROM VOTE_OPTION WHERE vote_id = v.id AND label = 'A');

INSERT INTO VOTE_OPTION (content, label, vote_count, created_at, vote_id, updated_at)
SELECT '볶음밥 vs 카레', 'B', 80, NOW(), v.id, NOW()
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