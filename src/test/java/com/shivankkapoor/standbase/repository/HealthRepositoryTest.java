package com.shivankkapoor.standbase.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HealthRepositoryTest {

    @Test
    void checkDb_runsATrivialQuery() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        new HealthRepository(jdbcTemplate).checkDb();

        verify(jdbcTemplate).queryForObject("SELECT 0", Integer.class);
    }
}
