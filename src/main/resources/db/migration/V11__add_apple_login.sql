ALTER TABLE member
    MODIFY COLUMN social_type enum('KAKAO','APPLE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    ADD COLUMN apple_refresh_token varchar(255) DEFAULT NULL,
    ADD COLUMN apple_client_id varchar(255) DEFAULT NULL,
    -- 탈퇴(soft delete)되지 않은 행만 1, 탈퇴된 행은 NULL이 되는 생성 컬럼.
    -- MySQL 8 UNIQUE 인덱스는 NULL을 서로 다른 값으로 취급하므로, 탈퇴한 회원·social_id가 없는 회원은
    -- 유니크 대상에서 자연히 제외되고, 탈퇴 후 같은 소셜 계정으로 재가입해도 충돌하지 않는다.
    ADD COLUMN active_identity TINYINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END) VIRTUAL,
    ADD UNIQUE INDEX uq_member_active_social_identity (social_type, social_id, active_identity);
