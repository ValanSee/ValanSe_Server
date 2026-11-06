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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V13 봇 칭호 지급 마이그레이션(M4, REQ-010/011/AC-017~020)을 실제 MySQL 컨테이너에
 * 적용해 검증하는 테스트입니다. SeedContentBotAccountsMigrationTest/
 * BotPersonaNicknameMigrationTest와 동일한 패턴입니다.
 */
@Testcontainers
class BotTitleGrantMigrationTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
    private static final String V13_FILE = "V13__grant_bot_default_title.sql";
    private static final List<String> BOT_SOCIAL_IDS = List.of(
            "content-seed-bot-001", "content-seed-bot-002", "content-seed-bot-003",
            "content-seed-bot-004", "content-seed-bot-005");

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void 모든_활성_봇에_유효한_칭호가_장착된다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            for (String socialId : BOT_SOCIAL_IDS) {
                assertThat(equippedTitleName(conn, socialId))
                        .as("bot %s 장착 칭호", socialId)
                        .isNotNull()
                        .isNotBlank();
            }
        }
    }

    // AC-017/AC-020: 새로 생성된 봇 전용 칭호는 active=FALSE여야 한다 - 그래야
    // TitleServiceImpl.getTitleList()의 지연 자동지급(활성 DEFAULT 칭호만 순회)
    // 대상에서 제외되어 일반 회원에게 노출되지 않는다.
    @Test
    void 새로_생성된_봇_전용_칭호는_비활성_상태다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            assertThat(titleActive(conn, "bot-curator")).isFalse();
            assertThat(titleAcquisitionType(conn, "bot-curator")).isEqualTo("DEFAULT");
        }
    }

    @Test
    void 재실행해도_member_profile_title에_중복_row가_생기지_않는다() throws Exception {
        migrateToLatest();
        executeSqlFile(V13_FILE); // Flyway 밖에서 같은 SQL을 한 번 더 직접 실행

        try (Connection conn = connect()) {
            for (String socialId : BOT_SOCIAL_IDS) {
                assertThat(memberProfileTitleRowCount(conn, socialId)).isEqualTo(1);
            }
            assertThat(titleRowCount(conn, "bot-curator")).isEqualTo(1);
        }
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
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

    private String equippedTitleName(Connection conn, String socialId) throws SQLException {
        String sql = "SELECT t.name FROM member m "
                + "JOIN member_profile_title mpt ON mpt.member_profile_id = m.id AND mpt.equipped = TRUE "
                + "JOIN title t ON t.id = mpt.title_id "
                + "WHERE m.social_id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private boolean titleActive(Connection conn, String code) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT active FROM title WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    private String titleAcquisitionType(Connection conn, String code) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT acquisition_type FROM title WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private int memberProfileTitleRowCount(Connection conn, String socialId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member m "
                + "JOIN member_profile_title mpt ON mpt.member_profile_id = m.id "
                + "WHERE m.social_id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int titleRowCount(Connection conn, String code) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT COUNT(*) FROM title WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
