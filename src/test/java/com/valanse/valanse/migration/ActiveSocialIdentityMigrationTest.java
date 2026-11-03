package com.valanse.valanse.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V11의 active_identity 생성 컬럼 + UNIQUE(social_type, social_id, active_identity) 제약을
 * 실제 MySQL 8 컨테이너에 적용해 검증하는 테스트입니다. (테스트 프로필은 H2를 쓰고 Flyway를 끄기 때문에
 * 이 제약은 H2에는 존재하지 않으며, 여기서 실제 MySQL로만 검증합니다.)
 */
@Testcontainers
class ActiveSocialIdentityMigrationTest {

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void migratesCleanlyToLatest() {
        migrateToLatest();
        // 예외 없이 끝나면 통과. V10 시드(활성 봇 5명, social_id 모두 다름)까지도 제약을 통과해야 한다.
    }

    @Test
    void rejectsDuplicateActiveMemberWithSameSocialTypeAndSocialId() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            insertMember(conn, "APPLE", "dup-social-id");

            assertThatThrownBy(() -> insertMember(conn, "APPLE", "dup-social-id"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void allowsDifferentSocialTypeWithSameSocialId() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            insertMember(conn, "KAKAO", "shared-social-id");
            insertMember(conn, "APPLE", "shared-social-id"); // 다른 social_type이면 충돌하지 않는다.

            assertThat(countMembers(conn, "shared-social-id")).isEqualTo(2);
        }
    }

    @Test
    void allowsReSignupAfterSoftDelete() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            long firstId = insertMember(conn, "APPLE", "resignup-social-id");
            softDelete(conn, firstId);

            // 탈퇴(soft delete)된 회원은 active_identity가 NULL이 되므로, 같은 소셜 계정으로 재가입해도 충돌하지 않는다.
            long secondId = insertMember(conn, "APPLE", "resignup-social-id");

            assertThat(secondId).isNotEqualTo(firstId);
            assertThat(countMembers(conn, "resignup-social-id")).isEqualTo(2);
        }
    }

    @Test
    void allowsMultipleSoftDeletedMembersWithSameSocialIdentity() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            long firstId = insertMember(conn, "APPLE", "multi-deleted-social-id");
            softDelete(conn, firstId);
            long secondId = insertMember(conn, "APPLE", "multi-deleted-social-id");
            softDelete(conn, secondId);

            // 둘 다 탈퇴 상태(active_identity = NULL)이므로 충돌 없이 공존할 수 있다.
            assertThat(countMembers(conn, "multi-deleted-social-id")).isEqualTo(2);
        }
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    private long insertMember(Connection conn, String socialType, String socialId) throws SQLException {
        String sql = "INSERT INTO member (social_type, social_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, socialType);
            ps.setString(2, socialId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void softDelete(Connection conn, long memberId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE member SET deleted_at = NOW(6) WHERE id = ?")) {
            ps.setLong(1, memberId);
            ps.executeUpdate();
        }
    }

    private int countMembers(Connection conn, String socialId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM member WHERE social_id = ?")) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
