-- 오늘의 밸런스 (Daily Balance): 날짜별로 하나의 게임을 지정하고 참여/정산 결과를 기록한다.
-- daily_vote.vote_id 는 원본 게임이 삭제(purge)되면 NULL 이 되며, 지난 기록은 스냅샷 컬럼으로 보존한다.
-- 선택지 id(option_a_id/option_b_id/first_option_id/current_option_id)에는 FK 를 걸지 않는다(purge 가 vote_option 을 지움).
CREATE TABLE daily_vote (
    vote_date DATE NOT NULL,
    vote_id BIGINT DEFAULT NULL,
    source VARCHAR(20) NOT NULL,
    question_snapshot TEXT DEFAULT NULL,
    option_a_snapshot VARCHAR(255) DEFAULT NULL,
    option_b_snapshot VARCHAR(255) DEFAULT NULL,
    option_a_id BIGINT DEFAULT NULL,
    option_b_id BIGINT DEFAULT NULL,
    winner VARCHAR(10) DEFAULT NULL,
    settled_a_count INT DEFAULT NULL,
    settled_b_count INT DEFAULT NULL,
    settled_at DATETIME(6) DEFAULT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (vote_date),
    KEY idx_daily_vote_vote_id (vote_id),
    CONSTRAINT fk_daily_vote_vote FOREIGN KEY (vote_id) REFERENCES vote (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE daily_vote_participation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vote_date DATE NOT NULL,
    vote_id BIGINT DEFAULT NULL,
    member_id BIGINT NOT NULL,
    first_option_id BIGINT DEFAULT NULL,
    current_option_id BIGINT DEFAULT NULL,
    participation_point INT NOT NULL DEFAULT 0,
    bonus_point INT DEFAULT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dvp_date_vote_member (vote_date, vote_id, member_id),
    KEY idx_dvp_date_member (vote_date, member_id),
    CONSTRAINT fk_dvp_daily_vote FOREIGN KEY (vote_date) REFERENCES daily_vote (vote_date),
    CONSTRAINT fk_dvp_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
