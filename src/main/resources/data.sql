USE valanse;

-- 기존 데이터 초기화
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE comment;
TRUNCATE TABLE comment_group;
TRUNCATE TABLE member_vote_option;
TRUNCATE TABLE vote_option;
TRUNCATE TABLE vote;
TRUNCATE TABLE member_profile;
TRUNCATE TABLE member;
SET FOREIGN_KEY_CHECKS = 1;

-- AUTO_INCREMENT 초기화
ALTER TABLE member AUTO_INCREMENT = 1;

-- member + member_profile
INSERT INTO member (id, name, email, profile_image_url, role, social_type, social_id, kakao_access_token, kakao_refresh_token, created_at, updated_at, is_deleted)
VALUES
    (1, 'TestUser', 'test@naver.com', 'http://example.com/profile.jpg', 'USER', 'KAKAO', '1234567890', 'access_token', 'refresh_token', NOW(), NOW(), FALSE),
    (2, 'OtherUser', 'other@naver.com', 'http://example.com/profile2.jpg', 'USER', 'KAKAO', '999999999', 'access_token', 'refresh_token', NOW(), NOW(), FALSE),
    (3, 'ThirdUser', 'third@naver.com', 'http://example.com/profile3.jpg', 'USER', 'KAKAO', '888888888', 'access_token', 'refresh_token', NOW(), NOW(), FALSE);

INSERT INTO member_profile (member_id, nickname, gender, age, mbti_ie, mbti_tf, mbti, created_at, updated_at)
VALUES
    (1, '테스트닉', 'MALE', 'TWENTY', 'I', 'F', 'INFJ', NOW(), NOW()),
    (2, '다른유저', 'FEMALE', 'THIRTY', 'E', 'T', 'ENTJ', NOW(), NOW()),
    (3, '세번째유저', 'MALE', 'OVER_FORTY', 'I', 'T', 'ISTP', NOW(), NOW());

-- vote + vote_option (예시: 오늘 뭐 먹지?)
INSERT INTO vote (id, category, title, total_vote_count, created_at, updated_at, member_id, is_deleted)
VALUES (101, 'FOOD', '오늘 뭐 먹지?', 0, NOW(), NOW(), 1, FALSE);

INSERT INTO vote_option (id, vote_id, content, label, vote_count, created_at, updated_at)
VALUES
    (201, 101, '삼겹살', 'A', 0, NOW(), NOW()),
    (202, 101, '초밥', 'B', 0, NOW(), NOW());

-- member_vote_option (예시 유저 투표 기록)
INSERT INTO member_vote_option (member_id, vote_id, vote_option_id, created_at, updated_at)
VALUES
    (1, 101, 201, NOW(), NOW()),
    (2, 101, 202, NOW(), NOW()),
    (3, 101, 202, NOW(), NOW());

-- comment_group + comment
INSERT INTO comment_group (id, vote_id, total_comment_count, created_at, updated_at)
VALUES (1, 101, 0, NOW(), NOW());

INSERT INTO comment (id, content, like_count, reply_count, is_deleted, member_id, comment_group_id, parent_id, created_at, updated_at)
VALUES
    (1, '댓글 1입니다', 3, 0, FALSE, 1, 1, NULL, NOW(), NOW()),
    (2, '댓글 2입니다', 1, 1, FALSE, 2, 1, NULL, NOW(), NOW()),
    (3, '댓글 2-1 답글입니다', 0, 0, FALSE, 3, 1, 2, NOW(), NOW());

-- comment_like
INSERT INTO comment_like (user_id, comment_id, created_at, updated_at)
VALUES
    (2, 1, NOW(), NOW()),
    (3, 1, NOW(), NOW()),
    (1, 2, NOW(), NOW());

-- 득표수 갱신 쿼리
UPDATE vote_option vo
    LEFT JOIN (
    SELECT vote_option_id, COUNT(*) AS cnt FROM member_vote_option GROUP BY vote_option_id
    ) mv ON vo.id = mv.vote_option_id
    SET vo.vote_count = IFNULL(mv.cnt, 0);

UPDATE vote v
    LEFT JOIN (
    SELECT vote_id, COUNT(*) AS cnt FROM member_vote_option GROUP BY vote_id
    ) mv ON v.id = mv.vote_id
    SET v.total_vote_count = IFNULL(mv.cnt, 0);

-- 댓글 좋아요/답글 수 갱신
UPDATE comment c
    LEFT JOIN (
    SELECT parent_id, COUNT(*) AS cnt FROM comment WHERE parent_id IS NOT NULL GROUP BY parent_id
    ) r ON c.id = r.parent_id
    SET c.reply_count = IFNULL(r.cnt, 0)
WHERE c.parent_id IS NULL;

UPDATE comment c
    LEFT JOIN (
    SELECT comment_id, COUNT(*) AS cnt FROM comment_like GROUP BY comment_id
    ) l ON c.id = l.comment_id
    SET c.like_count = IFNULL(l.cnt, 0);

-- 댓글그룹의 총 댓글 수 갱신
UPDATE comment_group cg
    LEFT JOIN (
    SELECT comment_group_id, COUNT(*) AS cnt FROM comment WHERE is_deleted = FALSE GROUP BY comment_group_id
    ) c ON cg.id = c.comment_group_id
    SET cg.total_comment_count = IFNULL(c.cnt, 0);