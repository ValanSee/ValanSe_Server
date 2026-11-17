-- 회원 차단 (App Store 심사 가이드라인 1.2 - UGC 앱의 사용자 차단 기능).
-- blocker가 blocked를 차단하면 blocker의 목록 조회(투표/트렌딩/댓글/대댓글/대표 댓글)에서 blocked의 콘텐츠가 제외된다.
-- 회원 퍼지(SoftDeletePurgeService) 시 해당 회원이 포함된 행을 먼저 삭제하며, FK의 ON DELETE CASCADE는 안전망이다.
CREATE TABLE member_block (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_block_blocker_blocked (blocker_id, blocked_id),
    KEY idx_member_block_blocked (blocked_id),
    CONSTRAINT fk_member_block_blocker FOREIGN KEY (blocker_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_member_block_blocked FOREIGN KEY (blocked_id) REFERENCES member (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
