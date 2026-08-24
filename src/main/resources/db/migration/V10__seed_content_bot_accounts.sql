-- content-seed-bot-001 ~ 005: 콘텐츠 자동 생성에 사용되는 봇 계정 5개를 시드합니다.
-- social_id에는 UNIQUE 제약이 없으므로(운영 데이터 중복 위험), social_id 존재 여부를 직접 확인해
-- 재실행되어도 중복 삽입되지 않도록 구성합니다.

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-001', 'KAKAO', 'USER', '노을', '노을', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-001');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-002', 'KAKAO', 'USER', '하람', '하람', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-002');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-003', 'KAKAO', 'USER', '다인', '다인', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-003');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-004', 'KAKAO', 'USER', '서준', '서준', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-004');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-005', 'KAKAO', 'USER', '이든', '이든', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-005');

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '노을', 'FEMALE', 'TEN', 'I', 'F', 'INFP', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-001'
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '하람', 'MALE', 'TWENTY', 'E', 'T', 'ESTJ', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-002'
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '다인', 'FEMALE', 'THIRTY', 'I', 'T', 'ISTP', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-003'
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '서준', 'MALE', 'OVER_FORTY', 'E', 'F', 'ENFJ', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-004'
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '이든', 'FEMALE', 'TWENTY', 'E', 'F', 'ENFP', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-005'
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);
