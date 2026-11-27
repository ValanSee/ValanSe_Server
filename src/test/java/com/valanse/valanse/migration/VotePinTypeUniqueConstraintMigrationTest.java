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
class VotePinTypeUniqueConstraintMigrationTest {

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void 마이그레이션_전_중복_TRENDING과_소프트삭제된_TRENDING은_정리된다() throws Exception {
        migrateTo("17");

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            long older = insertVote(st, "TRENDING", false);
            long deleted = insertVote(st, "TRENDING", true);
            long newer = insertVote(st, "TRENDING", false);
            insertVote(st, "NONE", false);

            migrateToLatest();

            assertThat(pinType(st, older)).isNull();
            assertThat(pinType(st, deleted)).isNull();
            assertThat(pinType(st, newer)).isEqualTo("TRENDING");
        }
    }

    @Test
    void 마이그레이션_후_pin_type은_유니크_제약을_가지고_NULL은_여러_개_허용한다() throws Exception {
        migrateToLatest();

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            long voteA = insertVote(st, null, false);
            long voteB = insertVote(st, null, false);

            assertThat(pinType(st, voteA)).isNull();
            assertThat(pinType(st, voteB)).isNull();

            st.execute("UPDATE `vote` SET `pin_type` = 'TRENDING' WHERE `id` = " + voteA);

            assertThatThrownBy(() ->
                    st.execute("UPDATE `vote` SET `pin_type` = 'TRENDING' WHERE `id` = " + voteB))
                    .isInstanceOf(SQLIntegrityConstraintViolationException.class);
        }
    }

    private long insertVote(Statement st, String pinType, boolean deleted) throws SQLException {
        String pinTypeLiteral = pinType == null ? "NULL" : "'" + pinType + "'";
        String sql = "INSERT INTO `vote` (`total_vote_count`, `pin_type`, `hot_issue_voted`, `deleted_at`) "
                + "VALUES (0, " + pinTypeLiteral + ", 0, " + (deleted ? "NOW(6)" : "NULL") + ")";
        st.execute(sql, Statement.RETURN_GENERATED_KEYS);
        try (ResultSet rs = st.getGeneratedKeys()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private String pinType(Statement st, long voteId) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT `pin_type` FROM `vote` WHERE `id` = " + voteId)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private void migrateToLatest() {
        flyway().migrate();
    }

    private void migrateTo(String target) {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }
}
