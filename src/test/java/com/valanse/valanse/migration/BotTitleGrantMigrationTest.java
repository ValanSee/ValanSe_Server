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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class BotTitleGrantMigrationTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
    private static final String V14_FILE = "V14__replace_bot_title_with_random_existing_titles.sql";
    private static final List<String> BOT_SOCIAL_IDS = List.of(
            "content-seed-bot-001", "content-seed-bot-002", "content-seed-bot-003",
            "content-seed-bot-004", "content-seed-bot-005");

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void 기존_활성_칭호를_봇마다_하나씩_장착한다() throws Exception {
        migrateToV13();
        seedExistingTitles(5);
        migrateToLatest();

        try (Connection conn = connect()) {
            Set<String> equippedTitleCodes = new HashSet<>();
            for (String socialId : BOT_SOCIAL_IDS) {
                String titleCode = equippedTitleCode(conn, socialId);
                assertThat(titleCode).startsWith("existing-title-");
                assertThat(equippedTitleCount(conn, socialId)).isEqualTo(1);
                equippedTitleCodes.add(titleCode);
            }
            assertThat(equippedTitleCodes).hasSize(5);
        }
    }

    @Test
    void 활성_칭호가_부족하면_중복을_허용해_모든_봇에_장착한다() throws Exception {
        migrateToV13();
        seedExistingTitles(2);
        migrateToLatest();

        try (Connection conn = connect()) {
            Set<String> equippedTitleCodes = new HashSet<>();
            for (String socialId : BOT_SOCIAL_IDS) {
                equippedTitleCodes.add(equippedTitleCode(conn, socialId));
            }
            assertThat(equippedTitleCodes).hasSize(2);
        }
    }

    @Test
    void AI_큐레이터_칭호를_제거한다() throws Exception {
        migrateToV13();
        seedExistingTitles(5);
        migrateToLatest();

        try (Connection conn = connect()) {
            assertThat(titleRowCount(conn, "bot-curator")).isZero();
        }
    }

    @Test
    void 재실행해도_최초_배정된_칭호를_유지한다() throws Exception {
        migrateToV13();
        seedExistingTitles(5);
        migrateToLatest();

        try (Connection conn = connect()) {
            Map<String, String> initialAssignments = equippedTitleAssignments(conn);

            executeSqlFile(V14_FILE);

            assertThat(equippedTitleAssignments(conn)).isEqualTo(initialAssignments);
            for (String socialId : BOT_SOCIAL_IDS) {
                assertThat(equippedTitleCount(conn, socialId)).isEqualTo(1);
            }
        }
    }

    private void migrateToV13() {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .target("13")
                .load()
                .migrate();
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void seedExistingTitles(int count) throws SQLException {
        String sql = "INSERT INTO title "
                + "(code, name, description, price, tier, acquisition_type, requirement_text, active, display_order) "
                + "VALUES (?, ?, NULL, 0, 'BASIC', 'DEFAULT', NULL, TRUE, ?)";
        try (Connection conn = connect(); var ps = conn.prepareStatement(sql)) {
            for (int index = 1; index <= count; index++) {
                ps.setString(1, "existing-title-" + index);
                ps.setString(2, "기존 칭호 " + index);
                ps.setInt(3, index);
                ps.addBatch();
            }
            ps.executeBatch();
        }
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

    private String equippedTitleCode(Connection conn, String socialId) throws SQLException {
        String sql = "SELECT t.code FROM member m "
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

    private int equippedTitleCount(Connection conn, String socialId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member m "
                + "JOIN member_profile_title mpt ON mpt.member_profile_id = m.id AND mpt.equipped = TRUE "
                + "WHERE m.social_id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, socialId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Map<String, String> equippedTitleAssignments(Connection conn) throws SQLException {
        Map<String, String> assignments = new HashMap<>();
        for (String socialId : BOT_SOCIAL_IDS) {
            assignments.put(socialId, equippedTitleCode(conn, socialId));
        }
        return assignments;
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
