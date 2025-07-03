-- MEMBER 삭제 후 삽입 (ID = 1 보장)
DELETE FROM member_vote_option;
DELETE FROM comment WHERE parent_id IS NOT NULL;
DELETE FROM comment_group;
DELETE FROM vote_option;
DELETE FROM vote;
DELETE FROM member_profile;
DELETE FROM member;

INSERT INTO member (id, created_at, email, name, role, social_type, social_id, updated_at)
VALUES (
           1,
           NOW(),
           'test@naver.com',
           'TestUser',
           'USER',
           'KAKAO',
           '1234567890',
           NOW()
       );

-- MEMBER_PROFILE
INSERT INTO member_profile (created_at, updated_at, member_id, nickname, gender, age, mbti_ie, mbti_tf, mbti)
VALUES (
           NOW(), NOW(),
           1,
           '테스트닉',
           'M',
           'TWENTY',
           'I',
           'F',
           'INFJ'
       );

-- 내가 만든 투표 1
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (
           101, 'FOOD', '오늘 저녁 뭐 먹지?', 150,
           '2025-07-01 12:00:00', '2025-07-01 12:00:00', 1, false
       );

-- 옵션
INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (201, '삼겹살', 'A', 90, 101, '2025-07-01 12:00:00', '2025-07-01 12:00:00'),
    (202, '초밥', 'B', 60, 101, '2025-07-01 12:00:00', '2025-07-01 12:00:00');

-- 내가 만든 투표 2
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (
           102, 'ETC', '점심 디저트 뭐 먹지?', 100,
           '2025-07-01 14:00:00', '2025-07-01 14:00:00', 1, false
       );

INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (203, '아이스크림', 'A', 55, 102, '2025-07-01 14:00:00', '2025-07-01 14:00:00'),
    (204, '과일', 'B', 45, 102, '2025-07-01 14:00:00', '2025-07-01 14:00:00');

-- 내가 참여한 다른 유저가 만든 투표
INSERT INTO member (id, created_at, email, name, role, social_type, social_id, updated_at)
VALUES (
           2,
           NOW(),
           'other@naver.com',
           'OtherUser',
           'USER',
           'KAKAO',
           '999999999',
           NOW()
       );

INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (
           103, 'LOVE', '연애 스타일은?', 80,
           '2025-07-01 10:00:00', '2025-07-01 10:00:00', 2, false
       );

INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (205, '직진형', 'A', 50, 103, '2025-07-01 10:00:00', '2025-07-01 10:00:00'),
    (206, '츤데레', 'B', 30, 103, '2025-07-01 10:00:00', '2025-07-01 10:00:00');

-- 내가 참여한 기록 (투표 103에서 A 선택)
INSERT INTO member_vote_option (member_id, vote_id, vote_option_id, created_at, updated_at)
VALUES (
           1, 103, 205, NOW(), NOW()
       );

-- 오전 09:00 투표 (참여 O)
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (104, 'FOOD', '아침 뭐 먹지?', 100, '2025-07-01 09:00:00', '2025-07-01 09:00:00', 2, false);

INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (207, '토스트', 'A', 60, 104, '2025-07-01 09:00:00', '2025-07-01 09:00:00'),
    (208, '시리얼', 'B', 40, 104, '2025-07-01 09:00:00', '2025-07-01 09:00:00');

INSERT INTO member_vote_option (member_id, vote_id, vote_option_id, created_at, updated_at)
VALUES (1, 104, 207, NOW(), NOW());

-- 오전 11:30 투표 (참여 X)
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (105, 'ETC', '시험 끝나고 뭐 하지?', 50, '2025-07-01 11:30:00', '2025-07-01 11:30:00', 1, false);

INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (209, '여행', 'A', 30, 105, '2025-07-01 11:30:00', '2025-07-01 11:30:00'),
    (210, '집콕', 'B', 20, 105, '2025-07-01 11:30:00', '2025-07-01 11:30:00');

-- 오후 13:00 투표 (참여 O)
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (106, 'LOVE', '데이트 장소 추천은?', 60, '2025-07-01 13:00:00', '2025-07-01 13:00:00', 2, false);

INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (211, '놀이공원', 'A', 45, 106, '2025-07-01 13:00:00', '2025-07-01 13:00:00'),
    (212, '카페', 'B', 15, 106, '2025-07-01 13:00:00', '2025-07-01 13:00:00');

INSERT INTO member_vote_option (member_id, vote_id, vote_option_id, created_at, updated_at)
VALUES (1, 106, 211, NOW(), NOW());

-- 오후 15:30 투표 (참여 X)
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (107, 'FOOD', '간식 뭐 먹을까?', 70, '2025-07-01 15:30:00', '2025-07-01 15:30:00', 1, false);

INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (213, '쿠키', 'A', 30, 107, '2025-07-01 15:30:00', '2025-07-01 15:30:00'),
    (214, '케이크', 'B', 40, 107, '2025-07-01 15:30:00', '2025-07-01 15:30:00');

-- 오후 18:00 투표 (참여 X)
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (108, 'ETC', '저녁에 뭐할까?', 90, '2025-07-01 18:00:00', '2025-07-01 18:00:00', 1, false);

INSERT INTO vote_option (id, content, label, vote_count, vote_id, created_at, updated_at)
VALUES
    (215, '넷플릭스', 'A', 50, 108, '2025-07-01 18:00:00', '2025-07-01 18:00:00'),
    (216, '산책', 'B', 40, 108, '2025-07-01 18:00:00', '2025-07-01 18:00:00');

