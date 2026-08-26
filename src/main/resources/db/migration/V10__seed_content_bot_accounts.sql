-- content-seed-bot-001 ~ 005: 콘텐츠 자동 생성에 사용되는 봇 계정 5개를 시드합니다.
-- social_id에는 UNIQUE 제약이 없으므로(운영 데이터 중복 위험), social_id 존재 여부를 직접 확인해
-- 재실행되어도 중복 삽입되지 않도록 구성합니다.

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-001', 'KAKAO', 'USER', '한입만판사', '한입만판사', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-001');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-002', 'KAKAO', 'USER', '연애배심원', '연애배심원', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-002');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-003', 'KAKAO', 'USER', '장바구니철학자', '장바구니철학자', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-003');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-004', 'KAKAO', 'USER', '숨참고승부', '숨참고승부', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-004');

INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot)
SELECT NOW(6), NOW(6), 'content-seed-bot-005', 'KAKAO', 'USER', '결정은내일', '결정은내일', TRUE
WHERE NOT EXISTS (SELECT 1 FROM member WHERE social_id = 'content-seed-bot-005');

-- member_profile은 충돌한 social_id의 기존 회원이 봇(is_bot = TRUE)일 때만 생성한다.
-- 일반 회원과 social_id가 우연히 겹치더라도 그 회원의 프로필을 절대 건드리지 않기 위한 안전장치다.

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '한입만판사', 'FEMALE', 'TWENTY', 'E', 'F', 'ENFP', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-001'
  AND m.is_bot = TRUE
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '연애배심원', 'MALE', 'THIRTY', 'I', 'F', 'INFJ', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-002'
  AND m.is_bot = TRUE
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '장바구니철학자', 'FEMALE', 'THIRTY', 'I', 'T', 'ISTJ', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-003'
  AND m.is_bot = TRUE
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '숨참고승부', 'MALE', 'TWENTY', 'E', 'T', 'ESTP', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-004'
  AND m.is_bot = TRUE
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);

INSERT INTO member_profile (member_id, created_at, updated_at, nickname, gender, age, mbti_ie, mbti_tf, mbti, point)
SELECT m.id, NOW(6), NOW(6), '결정은내일', 'FEMALE', 'OVER_FORTY', 'I', 'T', 'INTP', 0
FROM member m
WHERE m.social_id = 'content-seed-bot-005'
  AND m.is_bot = TRUE
  AND NOT EXISTS (SELECT 1 FROM member_profile mp WHERE mp.member_id = m.id);
