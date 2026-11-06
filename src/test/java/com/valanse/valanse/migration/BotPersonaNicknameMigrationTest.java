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
 * V12 봇 닉네임 교체 마이그레이션(M1, REQ-002/003/AC-005/006)을 실제 MySQL
 * 컨테이너에 적용해 검증하는 테스트입니다. SeedContentBotAccountsMigrationTest와
 * 동일한 패턴 - 테스트 프로필은 Flyway를 끄고 H2를 쓰기 때문에, 실제 마이그레이션
 * SQL은 별도로 MySQL에서 검증해야 한다.
 */
@Testcontainers
class BotPersonaNicknameMigrationTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
    private static final String V12_FILE = "V12__update_bot_account_nicknames.sql";

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void 다섯_봇의_닉네임이_자연스러운_새_닉네임으로_교체된다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            assertNickname(conn, "content-seed-bot-001", "한라봉");
            assertNickname(conn, "content-seed-bot-002", "존이냐");
            assertNickname(conn, "content-seed-bot-003", "휴지조각");
            assertNickname(conn, "content-seed-bot-004", "한판더");
            assertNickname(conn, "content-seed-bot-005", "크림이");
        }
    }

    @Test
    void member와_member_profile의_닉네임이_함께_바뀐다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            assertThat(memberNickname(conn, "content-seed-bot-001")).isEqualTo("한라봉");
            assertThat(memberName(conn, "content-seed-bot-001")).isEqualTo("한라봉");
            assertThat(profileNickname(conn, "content-seed-bot-001")).isEqualTo("한라봉");
        }
    }

    @Test
    void 새_닉네임이_이미_다른_활성_회원과_충돌하면_해당_봇의_닉네임_교체는_안전하게_건너뛴다() throws Exception {
        // AC-006 Edge Case: 후보 닉네임이 기존 활성 회원과 충돌하면 그 봇만 교체를
        // 스킵하고 기존 닉네임을 보존한다 - 나머지 4개 봇은 정상적으로 교체된다.
        migrateTo("11");

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot) "
                    + "VALUES (NOW(6), NOW(6), 'real-user-001', 'KAKAO', 'USER', '한라봉', '한라봉', FALSE)");
        }

        migrateToLatest();

        try (Connection conn = connect()) {
            // bot-001의 새 닉네임('한라봉')이 실제 회원과 충돌하므로 교체가 스킵된다.
            assertThat(memberNickname(conn, "content-seed-bot-001")).isEqualTo("한입만판사");
            assertThat(profileNickname(conn, "content-seed-bot-001")).isEqualTo("한입만판사");

            // 충돌하지 않은 나머지 4개 봇은 정상적으로 교체된다.
            assertNickname(conn, "content-seed-bot-002", "존이냐");
            assertNickname(conn, "content-seed-bot-003", "휴지조각");
            assertNickname(conn, "content-seed-bot-004", "한판더");
            assertNickname(conn, "content-seed-bot-005", "크림이");
        }
    }

    @Test
    void 재실행해도_중복이나_덮어쓰기가_발생하지_않는다() throws Exception {
        migrateToLatest();
        executeSqlFile(V12_FILE); // Flyway 밖에서 같은 SQL을 한 번 더 직접 실행

        try (Connection conn = connect()) {
            assertNickname(conn, "content-seed-bot-001", "한라봉");
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

    private void assertNickname(Connection conn, String socialId, String nickname) throws SQLException {
        assertThat(memberNickname(conn, socialId)).isEqualTo(nickname);
        assertThat(profileNickname(conn, socialId)).isEqualTo(nickname);
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

    private String memberName(Connection conn, String socialId) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT name FROM member WHERE social_id = ?")) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private String profileNickname(Connection conn, String socialId) throws SQLException {
        String sql = "SELECT mp.nickname FROM member m JOIN member_profile mp ON mp.member_id = m.id WHERE m.social_id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
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
