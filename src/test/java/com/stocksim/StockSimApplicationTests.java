package com.stocksim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StockSimApplicationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void contextLoadsAndFlywayCreatesSchema() {
        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(1);

        Integer usersTable = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'",
                Integer.class);
        assertThat(usersTable).isEqualTo(1);
    }
}
