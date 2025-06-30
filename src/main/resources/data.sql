-- 삭제 순서 (자식 → 부모 순서 중요!)
DELETE FROM member_vote_option;

-- 대댓글 먼저
DELETE FROM comment WHERE parent_id IS NOT NULL;

-- 부모 댓글
DELETE FROM comment WHERE parent_id IS NULL;

DELETE FROM comment_group;
DELETE FROM vote_option;
DELETE FROM vote;
DELETE FROM member;

-- MEMBER 삽입 (ID = 1)
INSERT INTO MEMBER (id, created_at, email, name, role, social_type, social_id, updated_at)
VALUES (
           1,
           '2025-06-30 14:00:00',
           'test@example.com',
           'TestUser',
           'USER',
           'KAKAO',
           '1234567890',
           '2025-06-30 14:00:00'
       );

-- 투표 1
INSERT INTO VOTE (id, category, title, total_vote_count, created_at, member_id, updated_at, is_deleted)
VALUES (101, 'FOOD', '오늘 저녁 뭐 먹지?', 150, '2025-06-30 14:01:00', 1, '2025-06-30 14:01:00', false);

INSERT INTO VOTE_OPTION (id, content, label, vote_count, created_at, vote_id, updated_at)
VALUES
    (1, '삼겹살', 'A', 90, '2025-06-30 14:01:00', 101, '2025-06-30 14:01:00'),
    (2, '초밥', 'B', 60, '2025-06-30 14:01:00', 101, '2025-06-30 14:01:00');

-- 투표 2
INSERT INTO VOTE (id, category, title, total_vote_count, created_at, member_id, updated_at, is_deleted)
VALUES (102, 'FOOD', '내일 점심 뭐 먹지?', 120, '2025-06-30 14:05:00', 1, '2025-06-30 14:05:00', false);

INSERT INTO VOTE_OPTION (id, content, label, vote_count, created_at, vote_id, updated_at)
VALUES
    (3, '제육볶음', 'A', 70, '2025-06-30 14:05:00', 102, '2025-06-30 14:05:00'),
    (4, '된장찌개', 'B', 50, '2025-06-30 14:05:00', 102, '2025-06-30 14:05:00');

-- 투표 3
INSERT INTO VOTE (id, category, title, total_vote_count, created_at, member_id, updated_at, is_deleted)
VALUES (103, 'SNACK', '최애 간식은?', 80, '2025-06-30 14:10:00', 1, '2025-06-30 14:10:00', false);

INSERT INTO VOTE_OPTION (id, content, label, vote_count, created_at, vote_id, updated_at)
VALUES
    (5, '떡볶이', 'A', 40, '2025-06-30 14:10:00', 103, '2025-06-30 14:10:00'),
    (6, '붕어빵', 'B', 40, '2025-06-30 14:10:00', 103, '2025-06-30 14:10:00');

-- member_vote_option
INSERT INTO member_vote_option (member_id, vote_id, vote_option_id, created_at, updated_at)
VALUES (1, 101, 1, NOW(), NOW());

-- 댓글 그룹 1 (투표 101번에 대한 댓글 그룹)
INSERT INTO comment_group (id, vote_id, created_at, updated_at, total_comment_count)
VALUES (201, 101, NOW(), NOW(), 2);

-- 댓글 1
INSERT INTO comment (id, content, like_count, reply_count, is_deleted, member_id, comment_group_id, created_at, updated_at, parent_id)
VALUES (301, '삼겹살 최고죠!', 10, 1, false, 1, 201, NOW(), NOW(), NULL);

-- 대댓글 1
INSERT INTO comment (id, content, like_count, reply_count, is_deleted, member_id, comment_group_id, created_at, updated_at, parent_id)
VALUES (302, '인정합니다 ㅋㅋ', 3, 0, false, 1, 201, NOW(), NOW(), 301);
