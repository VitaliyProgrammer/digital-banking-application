package io.github.vitaliyprogrammer.banking;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Smoke test: the application starts on the real stack and every migration has run. */
class DigitalBankingApplicationTests extends IntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void startsAgainstRealPostgres() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

        assertThat(version).startsWith("PostgreSQL 17");
    }

    @Test
    void leavesNoPendingMigrations() {
        assertThat(flyway.info().pending()).isEmpty();
    }
}
