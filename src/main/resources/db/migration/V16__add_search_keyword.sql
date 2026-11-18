CREATE TABLE search_keyword (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(32) NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    normalized_keyword VARCHAR(100) NOT NULL,
    monthly_search_volume INT NULL,
    impressions INT NULL,
    clicks INT NULL,
    avg_position DECIMAL(6,2) NULL,
    first_seen_at DATETIME(6) NOT NULL,
    last_seen_at DATETIME(6) NOT NULL,
    last_used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_search_keyword_source_normalized UNIQUE (source, normalized_keyword),
    INDEX idx_search_keyword_last_used_at (last_used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE search_keyword_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    search_keyword_id BIGINT NOT NULL,
    collected_date DATE NOT NULL,
    monthly_search_volume INT NULL,
    impressions INT NULL,
    clicks INT NULL,
    avg_position DECIMAL(6,2) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_search_keyword_snapshot_keyword_date UNIQUE (search_keyword_id, collected_date),
    CONSTRAINT fk_search_keyword_snapshot_keyword FOREIGN KEY (search_keyword_id) REFERENCES search_keyword (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
