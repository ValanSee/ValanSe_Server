ALTER TABLE member
    MODIFY COLUMN social_type enum('KAKAO','APPLE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    ADD COLUMN apple_refresh_token varchar(255) DEFAULT NULL,
    ADD COLUMN apple_client_id varchar(255) DEFAULT NULL;
