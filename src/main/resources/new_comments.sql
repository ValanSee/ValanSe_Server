INSERT INTO comment (id, member_id, comment_group_id, parent_id, content, reply_count, like_count, created_at, updated_at, deleted_at) VALUES
-- 투표 1 (ID: 1001) 관련 댓글
(10060, 11, 1001, NULL, '요즘은 앱으로 만나는 사람도 많더라구요', 1, 3, '2025-07-20 10:00:00', '2025-07-20 10:00:00', NULL),
(10061, 11, 1001, 10060, '하지만 저는 지인소개에 한표..', 0, 1, '2025-07-20 10:30:00', '2025-07-20 10:30:00', NULL),
(10062, 11, 1002, NULL, '양식!!', 1, 2, '2025-07-20 11:00:00', '2025-07-20 11:00:00', NULL);