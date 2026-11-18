package com.valanse.valanse.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class SearchKeywordMigrationTest {

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void 검색_키워드_테이블이_생성된다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            assertThat(tableCount(conn, "search_keyword")).isEqualTo(1);
            assertThat(tableCount(conn, "search_keyword_snapshot")).isEqualTo(1);
        }
    }

    @Test
    void 출처와_정규화_키워드_중복은_거부한다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute(insertKeyword("NAVER_SEARCH_AD", "밸런스게임"));

            assertThatThrownBy(() -> st.execute(insertKeyword("NAVER_SEARCH_AD", "밸런스게임")))
                    .isInstanceOf(SQLIntegrityConstraintViolationException.class);
            st.execute(insertKeyword("GOOGLE_SEARCH_CONSOLE", "밸런스게임"));
        }
    }

    @Test
    void 키워드와_수집일_중복_스냅샷은_거부한다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute(insertKeyword("NAVER_SEARCH_AD", "밸런스게임"));
            long id;
            try (ResultSet rs = st.executeQuery("SELECT id FROM search_keyword")) {
                rs.next();
                id = rs.getLong(1);
            }
            String sql = "INSERT INTO search_keyword_snapshot (search_keyword_id, collected_date) VALUES ("
                    + id + ", '2026-11-16')";
            st.execute(sql);

            assertThatThrownBy(() -> st.execute(sql))
                    .isInstanceOf(SQLIntegrityConstraintViolationException.class);
        }
    }

    private String insertKeyword(String source, String normalized) {
        return "INSERT INTO search_keyword (source, keyword, normalized_keyword, first_seen_at, last_seen_at, "
                + "created_at, updated_at) VALUES ('" + source + "', '" + normalized + "', '" + normalized
                + "', NOW(6), NOW(6), NOW(6), NOW(6))";
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

    private int tableCount(Connection conn, String table) throws SQLException {
        try (var ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
