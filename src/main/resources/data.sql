-- base_entity (공통 필드: created_at, updated_at, deleted_at)는 대부분의 테이블에 포함되므로, 각 테이블 INSERT 시 자동으로 채워진다고 가정하거나 수동으로 추가합니다. 여기서는 편의상 일부만 명시합니다.

-- 1. member 테이블 (사용자 정보)
INSERT INTO member (id, name, email, profile_image_url, social_type, social_id, role, created_at, updated_at, deleted_at) VALUES
                                                                                                                              (1, '테스트유저1', 'user1@example.com', 'http://example.com/profile1.jpg', 'KAKAO', 'kakao_id_1', 'USER', '2023-01-15 10:00:00', '2023-01-15 10:00:00', NULL),
                                                                                                                              (2, '테스트유저2', 'user2@example.com', 'http://example.com/profile2.jpg', 'KAKAO', 'kakao_id_2', 'USER', '2023-02-01 11:30:00', '2023-02-01 11:30:00', NULL),
                                                                                                                              (3, '어드민유저', 'admin@example.com', 'http://example.com/profile3.jpg', 'KAKAO', 'kakao_id_3', 'ADMIN', '2023-03-10 14:00:00', '2023-03-10 14:00:00', NULL),
                                                                                                                              (4, '김철수', 'chulsu.kim@example.com', 'http://example.com/chulsu.jpg', 'KAKAO', 'kakao_id_4', 'USER', '2023-04-05 09:00:00', '2023-04-05 09:00:00', NULL),
                                                                                                                              (5, '이영희', 'younghee.lee@example.com', 'http://example.com/younghee.jpg', 'KAKAO', 'kakao_id_5', 'USER', '2023-05-20 16:00:00', '2023-05-20 16:00:00', NULL),
                                                                                                                              (6, '박지민', 'jimin.park@example.com', 'http://example.com/jimin.jpg', 'KAKAO', 'kakao_id_6', 'USER', '2024-01-01 08:00:00', '2024-01-01 08:00:00', NULL),
                                                                                                                              (7, '최수현', 'suhyun.choi@example.com', 'http://example.com/suhyun.jpg', 'KAKAO', 'kakao_id_7', 'USER', '2024-02-10 13:00:00', '2024-02-10 13:00:00', NULL),
                                                                                                                              (8, '정은지', 'eunji.jung@example.com', 'http://example.com/eunji.jpg', 'KAKAO', 'kakao_id_8', 'USER', '2024-03-15 17:00:00', '2024-03-15 17:00:00', NULL),
                                                                                                                              (9, '강민준', 'minjun.kang@example.com', 'http://example.com/minjun.jpg', 'KAKAO', 'kakao_id_9', 'USER', '2024-04-20 10:00:00', '2024-04-20 10:00:00', NULL),
                                                                                                                              (10, '송혜교', 'hyegyo.song@example.com', 'http://example.com/hyegyo.jpg', 'KAKAO', 'kakao_id_10', 'USER', '2024-05-01 11:00:00', '2024-05-01 11:00:00', NULL);

-- 2. member_profile 테이블 (사용자 추가 정보)
INSERT INTO member_profile (member_id, nickname, gender, age, mbti, mbti_ie, mbti_tf, created_at, updated_at, deleted_at) VALUES
                                                                                                                              (1, '꽃님이', 'FEMALE', 'TWENTY', 'ISTJ', 'I', 'T', NOW(), NOW(), NULL),
                                                                                                                              (2, '돌쇠', 'MALE', 'THIRTY', 'ENFP', 'E', 'F', NOW(), NOW(), NULL),
                                                                                                                              (3, '관리자123', 'MALE', 'OVER_FORTY', 'ESTP', 'E', 'T', NOW(), NOW(), NULL),
                                                                                                                              (4, '철수짱', 'MALE', 'TWENTY', 'INTP', 'I', 'T', NOW(), NOW(), NULL),
                                                                                                                              (5, '영희씨', 'FEMALE', 'THIRTY', 'ESFJ', 'E', 'F', NOW(), NOW(), NULL),
                                                                                                                              (6, '지민쓰', 'MALE', 'TWENTY', 'ISFP', 'I', 'F', NOW(), NOW(), NULL),
                                                                                                                              (7, '수현킴', 'FEMALE', 'TWENTY', 'ENTJ', 'E', 'T', NOW(), NOW(), NULL),
                                                                                                                              (8, '은지맘', 'FEMALE', 'THIRTY', 'INFJ', 'I', 'F', NOW(), NOW(), NULL),
                                                                                                                              (9, '민준킹', 'MALE', 'TWENTY', 'ESTJ', 'E', 'T', NOW(), NOW(), NULL),
                                                                                                                              (10, '교미', 'FEMALE', 'OVER_FORTY', 'ISTP', 'I', 'T', NOW(), NOW(), NULL);

-- 3. vote 테이블 (투표 정보)
INSERT INTO vote (id, category, title, total_vote_count, member_id, created_at, updated_at, deleted_at, is_deleted) VALUES
                                                                                                                        (1, 'LOVE', '솔로탈출! 소개팅 앱 vs 지인 소개?', 0, 1, '2025-06-15 10:00:00', '2025-06-15 10:00:00', NULL, FALSE),
                                                                                                                        (2, 'FOOD', '오늘 점심, 한식 vs 양식?', 0, 2, '2024-06-20 11:00:00', '2024-06-20 11:00:00', NULL, FALSE),
                                                                                                                        (3, 'ETC', '여름 휴가, 해외여행 vs 국내여행?', 0, 4, '2024-06-25 12:00:00', '2024-06-25 12:00:00', NULL, FALSE),
                                                                                                                        (4, 'LOVE', '썸 단계에서 애인에게 선물 준다면?', 0, 5, '2024-06-28 13:00:00', '2024-06-28 13:00:00', NULL, FALSE),
                                                                                                                        (5, 'FOOD', '아침 식사, 밥 vs 빵?', 0, 6, '2024-07-01 14:00:00', '2024-07-01 14:00:00', NULL, FALSE),
                                                                                                                        (6, 'ETC', '나에게 더 중요한 것은?', 0, 7, '2024-07-03 15:00:00', '2024-07-03 15:00:00', NULL, FALSE),
                                                                                                                        (7, 'LOVE', '재회, 긍정적 vs 부정적?', 0, 8, '2024-07-05 16:00:00', '2024-07-05 16:00:00', NULL, FALSE),
                                                                                                                        (8, 'FOOD', '맵고수 인정? 매운 음식 레벨은?', 0, 9, '2024-07-06 17:00:00', '2024-07-06 17:00:00', NULL, FALSE),
                                                                                                                        (9, 'ETC', '나만의 스트레스 해소법은?', 0, 10, '2024-07-07 18:00:00', '2024-07-07 18:00:00', NULL, FALSE),
                                                                                                                        (10, 'LOVE', '첫 데이트 복장, 캐주얼 vs 포멀?', 0, 1, '2025-07-04 19:00:00', '2025-07-04 19:00:00', NULL, FALSE),
                                                                                                                        (11, 'FOOD', '최애 카페 메뉴는?', 0, 2, '2025-07-05 20:00:00', '2025-07-05 20:00:00', NULL, FALSE);

-- 4. comment_group 테이블 (댓글 그룹 - 각 투표에 연결)
INSERT INTO comment_group (id, total_comment_count, created_at, deleted_at, updated_at, vote_id) VALUES
                                                                                                     (1001, 0, '2023-06-01 10:00:00', NULL, '2023-06-01 10:00:00', 1),
                                                                                                     (1002, 0, '2023-07-01 11:00:00', NULL, '2023-07-01 11:00:00', 2),
                                                                                                     (1003, 0, '2023-08-01 12:00:00', NULL, '2023-08-01 12:00:00', 3),
                                                                                                     (1004, 0, '2024-01-01 13:00:00', NULL, '2024-01-01 13:00:00', 4),
                                                                                                     (1005, 0, '2024-02-01 14:00:00', NULL, '2024-02-01 14:00:00', 5),
                                                                                                     (1006, 0, '2024-03-01 15:00:00', NULL, '2024-03-01 15:00:00', 6),
                                                                                                     (1007, 0, '2024-04-01 16:00:00', NULL, '2024-04-01 16:00:00', 7),
                                                                                                     (1008, 0, '2024-05-01 17:00:00', NULL, '2024-05-01 17:00:00', 8),
                                                                                                     (1009, 0, '2024-06-01 18:00:00', NULL, '2024-06-01 18:00:00', 9),
                                                                                                     (1010, 0, '2024-07-01 19:00:00', NULL, '2024-07-01 19:00:00', 10),
                                                                                                     (1011, 0, '2024-08-01 20:00:00', NULL, '2024-08-01 20:00:00', 11);

-- 5. vote_option 테이블 (투표 선택지)
INSERT INTO vote_option (id, vote_id, content, vote_count, label, created_at, updated_at, deleted_at) VALUES
                                                                                                          (100, 1, '소개팅 앱', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (101, 1, '지인 소개', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (102, 2, '한식', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (103, 2, '양식', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (104, 3, '해외여행', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (105, 3, '국내여행', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (106, 4, '선물하지 않는다', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (107, 4, '가볍게 선물한다', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (108, 4, '고가의 선물도 OK', 0, 'C', NOW(), NOW(), NULL),
                                                                                                          (109, 5, '밥', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (110, 5, '빵', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (111, 6, '돈', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (112, 6, '시간', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (113, 7, '재회는 긍정적', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (114, 7, '재회는 부정적', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (115, 8, '신라면도 매워요', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (116, 8, '엽떡 정도는 먹죠', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (117, 8, '불닭볶음면은 기본', 0, 'C', NOW(), NOW(), NULL),
                                                                                                          (118, 8, '핵불닭도 가능', 0, 'D', NOW(), NOW(), NULL),
                                                                                                          (119, 9, '잠자기', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (120, 9, '운동하기', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (121, 9, '친구 만나기', 0, 'C', NOW(), NOW(), NULL),
                                                                                                          (122, 10, '캐주얼', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (123, 10, '포멀', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (124, 11, '아메리카노', 0, 'A', NOW(), NOW(), NULL),
                                                                                                          (125, 11, '라떼', 0, 'B', NOW(), NOW(), NULL),
                                                                                                          (126, 11, '아인슈페너', 0, 'C', NOW(), NOW(), NULL);


-- 6. member_vote_option 테이블 (사용자의 투표 선택)
INSERT INTO member_vote_option (id, member_id, vote_option_id, vote_id, created_at, updated_at, deleted_at) VALUES
-- 투표 1: 솔로탈출 (총 10명 참여)
(1000, 1, 100, 1, NOW(), NOW(), NULL), (1001, 2, 100, 1, NOW(), NOW(), NULL), (1002, 3, 100, 1, NOW(), NOW(), NULL), (1003, 4, 100, 1, NOW(), NOW(), NULL), (1004, 5, 100, 1, NOW(), NOW(), NULL),
(1005, 6, 101, 1, NOW(), NOW(), NULL), (1006, 7, 101, 1, NOW(), NOW(), NULL), (1007, 8, 101, 1, NOW(), NOW(), NULL), (1008, 9, 101, 1, NOW(), NOW(), NULL),
(1009, 10, 101, 1, NOW(), NOW(), NULL),
-- 투표 2: 오늘 점심 (총 8명 참여)
(1010, 1, 102, 2, NOW(), NOW(), NULL), (1011, 2, 102, 2, NOW(), NOW(), NULL), (1012, 3, 102, 2, NOW(), NOW(), NULL), (1013, 4, 102, 2, NOW(), NOW(), NULL),
(1014, 5, 103, 2, NOW(), NOW(), NULL), (1015, 6, 103, 2, NOW(), NOW(), NULL), (1016, 7, 103, 2, NOW(), NOW(), NULL), (1017, 8, 103, 2, NOW(), NOW(), NULL),
-- 투표 3: 여름 휴가 (총 12명 참여)
(1018, 1, 104, 3, NOW(), NOW(), NULL), (1019, 2, 104, 3, NOW(), NOW(), NULL), (1020, 3, 104, 3, NOW(), NOW(), NULL), (1021, 4, 104, 3, NOW(), NOW(), NULL), (1022, 5, 104, 3, NOW(), NOW(), NULL), (1023, 6, 104, 3, NOW(), NOW(), NULL), (1024, 7, 104, 3, NOW(), NOW(), NULL),
(1025, 8, 105, 3, NOW(), NOW(), NULL), (1026, 9, 105, 3, NOW(), NOW(), NULL), (1027, 10, 105, 3, NOW(), NOW(), NULL), (1028, 1, 105, 3, NOW(), NOW(), NULL), (1029, 2, 105, 3, NOW(), NOW(), NULL),
-- 투표 4: 썸 선물 (총 9명 참여)
(1030, 3, 106, 4, NOW(), NOW(), NULL), (1031, 4, 106, 4, NOW(), NOW(), NULL), (1032, 5, 106, 4, NOW(), NOW(), NULL), (1033, 6, 106, 4, NOW(), NOW(), NULL), (1034, 7, 106, 4, NOW(), NOW(), NULL),
(1035, 8, 107, 4, NOW(), NOW(), NULL), (1036, 9, 107, 4, NOW(), NOW(), NULL), (1037, 10, 107, 4, NOW(), NOW(), NULL),
(1038, 1, 108, 4, NOW(), NOW(), NULL),
-- 투표 5: 아침 식사 (총 7명 참여)
(1039, 2, 109, 5, NOW(), NOW(), NULL), (1040, 3, 109, 5, NOW(), NOW(), NULL), (1041, 4, 109, 5, NOW(), NOW(), NULL), (1042, 5, 109, 5, NOW(), NOW(), NULL),
(1043, 6, 110, 5, NOW(), NOW(), NULL), (1044, 7, 110, 5, NOW(), NOW(), NULL), (1045, 8, 110, 5, NOW(), NOW(), NULL),
-- 투표 6: 나에게 더 중요한 것 (총 6명 참여)
(1046, 9, 111, 6, NOW(), NOW(), NULL), (1047, 10, 111, 6, NOW(), NOW(), NULL), (1048, 1, 111, 6, NOW(), NOW(), NULL),
(1049, 2, 112, 6, NOW(), NOW(), NULL), (1050, 3, 112, 6, NOW(), NOW(), NULL), (1051, 4, 112, 6, NOW(), NOW(), NULL),
-- 투표 7: 재회 (총 5명 참여)
(1052, 5, 113, 7, NOW(), NOW(), NULL), (1053, 6, 113, 7, NOW(), NOW(), NULL), (1054, 7, 113, 7, NOW(), NOW(), NULL),
(1055, 8, 114, 7, NOW(), NOW(), NULL), (1056, 9, 114, 7, NOW(), NOW(), NULL),
-- 투표 8: 매운 음식 레벨 (총 15명 참여 - 인기 투표 예시)
(1057, 10, 115, 8, NOW(), NOW(), NULL), (1058, 1, 115, 8, NOW(), NOW(), NULL),
(1059, 2, 116, 8, NOW(), NOW(), NULL), (1060, 3, 116, 8, NOW(), NOW(), NULL), (1061, 4, 116, 8, NOW(), NOW(), NULL), (1062, 5, 116, 8, NOW(), NOW(), NULL),
(1063, 6, 117, 8, NOW(), NOW(), NULL), (1064, 7, 117, 8, NOW(), NOW(), NULL), (1065, 8, 117, 8, NOW(), NOW(), NULL), (1066, 9, 117, 8, NOW(), NOW(), NULL), (1067, 10, 117, 8, NOW(), NOW(), NULL),
(1068, 1, 118, 8, NOW(), NOW(), NULL), (1069, 2, 118, 8, NOW(), NOW(), NULL), (1070, 3, 118, 8, NOW(), NOW(), NULL), (1071, 4, 118, 8, NOW(), NOW(), NULL),
-- 투표 9: 스트레스 해소법 (총 3명 참여)
(1072, 5, 119, 9, NOW(), NOW(), NULL), (1073, 6, 120, 9, NOW(), NOW(), NULL), (1074, 7, 121, 9, NOW(), NOW(), NULL),
-- 투표 10: 첫 데이트 복장 (총 15명 참여 - 인기 투표 예시)
(1075, 1, 122, 10, NOW(), NOW(), NULL), (1076, 2, 122, 10, NOW(), NOW(), NULL), (1077, 3, 122, 10, NOW(), NOW(), NULL), (1078, 4, 122, 10, NOW(), NOW(), NULL), (1079, 5, 122, 10, NOW(), NOW(), NULL),
(1080, 6, 122, 10, NOW(), NOW(), NULL), (1081, 7, 122, 10, NOW(), NOW(), NULL), (1082, 8, 122, 10, NOW(), NOW(), NULL), (1083, 9, 122, 10, NOW(), NOW(), NULL), (1084, 10, 122, 10, NOW(), NOW(), NULL),
(1085, 1, 123, 10, NOW(), NOW(), NULL), (1086, 2, 123, 10, NOW(), NOW(), NULL), (1087, 3, 123, 10, NOW(), NOW(), NULL), (1088, 4, 123, 10, NOW(), NOW(), NULL), (1089, 5, 123, 10, NOW(), NOW(), NULL),
-- 투표 11: 최애 카페 메뉴 (총 8명 참여 - 최신 투표 예시)
(1090, 6, 124, 11, NOW(), NOW(), NULL), (1091, 7, 124, 11, NOW(), NOW(), NULL), (1092, 8, 124, 11, NOW(), NOW(), NULL), (1093, 9, 124, 11, NOW(), NOW(), NULL), (1094, 10, 124, 11, NOW(), NOW(), NULL),
(1095, 1, 125, 11, NOW(), NOW(), NULL), (1096, 2, 125, 11, NOW(), NOW(), NULL),
(1097, 3, 126, 11, NOW(), NOW(), NULL);


-- 7. comment 테이블 (댓글 정보)
INSERT INTO comment (id, member_id, comment_group_id, parent_id, content, reply_count, like_count, is_deleted, created_at, updated_at, deleted_at) VALUES
-- 투표 1 (ID: 1001) 관련 댓글
(10001, 1, 1001, NULL, '앱이 빠르고 편하죠!', 0, 0, FALSE, '2024-06-16 10:00:00', '2024-06-16 10:00:00', NULL),
(10002, 2, 1001, 10001, '그래도 지인 소개가 더 믿음직해요.', 0, 0, FALSE, '2024-06-16 10:30:00', '2024-06-16 10:30:00', NULL),
(10003, 3, 1001, NULL, '저는 둘 다 해봤어요!', 0, 0, FALSE, '2024-06-17 11:00:00', '2024-06-17 11:00:00', NULL),
(10004, 4, 1001, 10003, '어떤게 더 좋았나요?', 0, 0, FALSE, '2024-06-17 11:15:00', '2024-06-17 11:15:00', NULL),
(10005, 5, 1001, NULL, '삭제된 댓글입니다.', 0, 0, TRUE, '2024-06-18 12:00:00', '2024-06-18 12:05:00', '2024-06-18 12:05:00'),
-- 투표 3 (ID: 1003) 관련 댓글
(10006, 6, 1003, NULL, '여름엔 동남아 최고!', 0, 0, FALSE, '2024-06-26 09:00:00', '2024-06-26 09:00:00', NULL),
(10007, 7, 1003, 10006, '어디가 좋았어요?', 0, 0, FALSE, '2024-06-26 09:30:00', '2024-06-26 09:30:00', NULL),
(10008, 8, 1003, NULL, '제주도가 최고! 국내에도 갈 곳 많아요.', 0, 0, FALSE, '2024-06-27 10:00:00', '2024-06-27 10:00:00', NULL),
-- 투표 10 (ID: 1010) 관련 댓글
(10009, 9, 1010, NULL, '캐주얼이 짱이죠!', 0, 0, FALSE, '2024-07-09 10:00:00', '2024-07-09 10:00:00', NULL),
(10010, 10, 1010, 10009, '저도 동의해요!', 0, 0, FALSE, '2024-07-09 10:15:00', '2024-07-09 10:15:00', NULL),
(10011, 1, 1010, NULL, '포멀하게 입고 가면 뭔가 더 대접받는 느낌?', 0, 0, FALSE, '2024-07-09 11:00:00', '2024-07-09 11:00:00', NULL),
(10012, 2, 1010, NULL, '편안한게 최고예요.', 0, 0, FALSE, '2024-07-09 11:30:00', '2024-07-09 11:30:00', NULL);


-- 8. comment_like 테이블 (댓글 좋아요 정보)
INSERT INTO comment_like (id, user_id, comment_id, created_at, updated_at, deleted_at) VALUES
                                                                                           (100001, 2, 10001, NOW(), NOW(), NULL), -- 유저2가 댓글1 좋아요
                                                                                           (100002, 3, 10001, NOW(), NOW(), NULL), -- 유저3이 댓글1 좋아요
                                                                                           (100003, 4, 10001, NOW(), NOW(), NULL), -- 유저4가 댓글1 좋아요
                                                                                           (100004, 5, 10002, NOW(), NOW(), NULL), -- 유저5가 댓글2 좋아요
                                                                                           (100005, 6, 10003, NOW(), NOW(), NULL), -- 유저6이 댓글3 좋아요
                                                                                           (100006, 7, 10003, NOW(), NOW(), NULL), -- 유저7이 댓글3 좋아요
                                                                                           (100007, 8, 10004, NOW(), NOW(), NULL), -- 유저8이 댓글4 좋아요
                                                                                           (100008, 9, 10006, NOW(), NOW(), NULL), -- 유저9가 댓글6 좋아요
                                                                                           (100009, 10, 10006, NOW(), NOW(), NULL), -- 유저10이 댓글6 좋아요
                                                                                           (100010, 1, 10008, NOW(), NOW(), NULL), -- 유저1이 댓글8 좋아요
                                                                                           (100011, 2, 10009, NOW(), NOW(), NULL), -- 유저2가 댓글9 좋아요
                                                                                           (100012, 3, 10009, NOW(), NOW(), NULL), -- 유저3이 댓글9 좋아요
                                                                                           (100013, 4, 10009, NOW(), NOW(), NULL), -- 유저4가 댓글9 좋아요
                                                                                           (100014, 5, 10010, NOW(), NOW(), NULL), -- 유저5가 댓글10 좋아요
                                                                                           (100015, 6, 10011, NOW(), NOW(), NULL), -- 유저6이 댓글11 좋아요
                                                                                           (100016, 7, 10012, NOW(), NOW(), NULL); -- 유저7이 댓글12 좋아요


-- 최종 데이터 일관성을 위한 업데이트 (애플리케이션 로직에서 처리될 수 있으나, mock 데이터 정확성을 위해 포함)
-- MySQL의 'You can't specify target table for update in FROM clause' 오류를 피하기 위해 JOIN 사용

-- vote_option의 vote_count 업데이트
UPDATE vote_option vo
    LEFT JOIN (
        SELECT mvo.vote_option_id AS option_id_val, COUNT(*) AS count_val
        FROM member_vote_option mvo
        GROUP BY mvo.vote_option_id
    ) AS counted_votes
    ON vo.id = counted_votes.option_id_val
SET vo.vote_count = COALESCE(counted_votes.count_val, 0);


-- vote의 total_vote_count 업데이트
UPDATE vote v
    LEFT JOIN (
        SELECT vo.vote_id AS vote_id_val, SUM(vo.vote_count) AS total_sum
        FROM vote_option vo
        GROUP BY vo.vote_id
    ) AS summed_options
    ON v.id = summed_options.vote_id_val
SET v.total_vote_count = COALESCE(summed_options.total_sum, 0);


-- comment의 like_count 업데이트
UPDATE comment c
    LEFT JOIN (
        SELECT cl.comment_id AS comment_id_val, COUNT(*) AS like_count_val
        FROM comment_like cl
        GROUP BY cl.comment_id
    ) AS counted_likes
    ON c.id = counted_likes.comment_id_val
SET c.like_count = COALESCE(counted_likes.like_count_val, 0);


-- comment의 reply_count 업데이트
UPDATE comment c_parent
    LEFT JOIN (
        SELECT c_child.parent_id AS parent_id_val, COUNT(*) AS reply_count_val
        FROM comment c_child
        WHERE c_child.parent_id IS NOT NULL AND c_child.is_deleted = FALSE
        GROUP BY c_child.parent_id
    ) AS count_replies
    ON c_parent.id = count_replies.parent_id_val
SET c_parent.reply_count = COALESCE(count_replies.reply_count_val, 0)
WHERE c_parent.parent_id IS NULL; -- 최상위 댓글만 업데이트


-- comment_group의 total_comment_count 업데이트
UPDATE comment_group cg
    LEFT JOIN (
        SELECT c.comment_group_id AS group_id_val, COUNT(*) AS comment_count_val
        FROM comment c
        WHERE c.is_deleted = FALSE
        GROUP BY c.comment_group_id
    ) AS total_count
    ON cg.id = total_count.group_id_val
SET cg.total_comment_count = COALESCE(total_count.comment_count_val, 0);