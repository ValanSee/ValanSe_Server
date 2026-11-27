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
class DailyVoteMigrationTest {

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void daily_vote_테이블이_생성된다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect()) {
            assertThat(tableCount(conn, "daily_vote")).isEqualTo(1);
            assertThat(tableCount(conn, "daily_vote_participation")).isEqualTo(1);
        }
    }

    @Test
    void 같은_날짜_회원_게임_참여는_중복_거부() throws Exception {
        migrateToLatest();

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            long voteId = insertVote(st);
            long memberId = insertMember(st);
            st.execute(insertDailyVote(voteId));
            String sql = "INSERT INTO daily_vote_participation (vote_date, vote_id, member_id, first_option_id, "
                    + "current_option_id, participation_point, created_at) VALUES ('2026-11-26', " + voteId + ", "
                    + memberId + ", 1, 1, 3, NOW(6))";
            st.execute(sql);

            assertThatThrownBy(() -> st.execute(sql))
                    .isInstanceOf(SQLIntegrityConstraintViolationException.class);
        }
    }

    @Test
    void vote를_지우면_daily_vote_vote_id는_null() throws Exception {
        migrateToLatest();

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            long voteId = insertVote(st);
            st.execute(insertDailyVote(voteId));

            st.execute("DELETE FROM vote WHERE id = " + voteId);

            try (ResultSet rs = st.executeQuery("SELECT vote_id FROM daily_vote WHERE vote_date = '2026-11-26'")) {
                assertThat(rs.next()).isTrue();
                rs.getLong(1);
                assertThat(rs.wasNull()).isTrue();
            }
        }
    }

    private String insertDailyVote(long voteId) {
        return "INSERT INTO daily_vote (vote_date, vote_id, source, question_snapshot, option_a_snapshot, "
                + "option_b_snapshot, created_at, updated_at) VALUES ('2026-11-26', " + voteId
                + ", 'RESERVED', 'Q', 'A', 'B', NOW(6), NOW(6))";
    }

    private long insertVote(Statement st) throws SQLException {
        st.execute("INSERT INTO vote (created_at, updated_at, title, pin_type, total_vote_count, reactivity_score) "
                + "VALUES (NOW(6), NOW(6), 'Q', 'NONE', 0, 0)");
        return lastInsertId(st);
    }

    private long insertMember(Statement st) throws SQLException {
        st.execute("INSERT INTO member (created_at, updated_at, social_id, social_type, role, name, nickname, is_bot) "
                + "VALUES (NOW(6), NOW(6), 'daily-vote-member', 'KAKAO', 'USER', '유저', '유저', FALSE)");
        return lastInsertId(st);
    }

    private long lastInsertId(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
            rs.next();
            return rs.getLong(1);
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
