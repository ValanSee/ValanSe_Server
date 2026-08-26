package com.valanse.valanse.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V10 봇 계정 시드 마이그레이션을 실제 MySQL 컨테이너에 적용해 검증하는 테스트입니다.
 * 테스트 프로필은 Flyway를 끄고 H2를 쓰기 때문에(application-test.yml), 이 SQL은
 * 별도로 실제 MySQL에서 검증해야 한다는 plan.md의 사항을 자동화합니다.
 */
@Testcontainers
class SeedContentBotAccountsMigrationTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void seedsCorrectBotPersonas() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            assertPersona(conn, "content-seed-bot-001", "한입만판사", "FEMALE", "TWENTY", "ENFP");
            assertPersona(conn, "content-seed-bot-002", "연애배심원", "MALE", "THIRTY", "INFJ");
            assertPersona(conn, "content-seed-bot-003", "장바구니철학자", "FEMALE", "THIRTY", "ISTJ");
            assertPersona(conn, "content-seed-bot-004", "숨참고승부", "MALE", "TWENTY", "ESTP");
            assertPersona(conn, "content-seed-bot-005", "결정은내일", "FEMALE", "OVER_FORTY", "INTP");
        }
    }

    @Test
    void skipsProfileCreationWhenSocialIdCollidesWithNonBotMember() throws Exception {
        migrateTo("9");

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot) "
                    + "VALUES (NOW(6), NOW(6), 'content-seed-bot-001', 'KAKAO', 'USER', '진짜유저', '진짜유저', FALSE)");
        }

        migrateToLatest();

        try (Connection conn = connect()) {
            // 충돌한 기존 회원은 그대로 유지되고, 프로필은 생성되지 않는다.
            assertThat(memberIsBot(conn, "content-seed-bot-001")).isFalse();
            assertThat(memberNickname(conn, "content-seed-bot-001")).isEqualTo("진짜유저");
            assertThat(hasProfile(conn, "content-seed-bot-001")).isFalse();

            // 충돌하지 않은 나머지 4개 봇은 정상적으로 시드된다.
            assertPersona(conn, "content-seed-bot-002", "연애배심원", "MALE", "THIRTY", "INFJ");
            assertPersona(conn, "content-seed-bot-003", "장바구니철학자", "FEMALE", "THIRTY", "ISTJ");
            assertPersona(conn, "content-seed-bot-004", "숨참고승부", "MALE", "TWENTY", "ESTP");
            assertPersona(conn, "content-seed-bot-005", "결정은내일", "FEMALE", "OVER_FORTY", "INTP");
        }
    }

    @Test
    void reApplyingSeedSqlDirectlyIsIdempotent() throws Exception {
        migrateToLatest();
        executeSqlFile("V10__seed_content_bot_accounts.sql"); // Flyway 밖에서 같은 SQL을 한 번 더 직접 실행

        try (Connection conn = connect()) {
            assertThat(memberCount(conn)).isEqualTo(5);
            assertThat(profileCount(conn)).isEqualTo(5);
        }
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void migrateTo(String targetVersion) {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .target(targetVersion)
                .load()
                .migrate();
    }

    private void executeSqlFile(String fileName) throws IOException, SQLException {
        String sql = Files.readString(MIGRATION_DIR.resolve(fileName), StandardCharsets.UTF_8);
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.strip();
                if (!trimmed.isEmpty()) {
                    st.execute(trimmed);
                }
            }
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    private void assertPersona(Connection conn, String socialId, String nickname, String gender, String age, String mbti) throws SQLException {
        String sql = "SELECT mp.nickname, mp.gender, mp.age, mp.mbti FROM member m "
                + "JOIN member_profile mp ON mp.member_id = m.id WHERE m.social_id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("profile row for %s", socialId).isTrue();
                assertThat(rs.getString("nickname")).isEqualTo(nickname);
                assertThat(rs.getString("gender")).isEqualTo(gender);
                assertThat(rs.getString("age")).isEqualTo(age);
                assertThat(rs.getString("mbti")).isEqualTo(mbti);
            }
        }
    }

    private boolean memberIsBot(Connection conn, String socialId) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT is_bot FROM member WHERE social_id = ?")) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    private String memberNickname(Connection conn, String socialId) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT nickname FROM member WHERE social_id = ?")) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private boolean hasProfile(Connection conn, String socialId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member_profile mp JOIN member m ON m.id = mp.member_id WHERE m.social_id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private int memberCount(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM member WHERE social_id LIKE 'content-seed-bot-%'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int profileCount(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member_profile mp JOIN member m ON m.id = mp.member_id "
                + "WHERE m.social_id LIKE 'content-seed-bot-%'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
