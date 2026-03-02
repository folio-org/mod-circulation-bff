package org.folio.circulationbff.config;

import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

  @Bean
  public JdbcTemplate jdbcTemplate() {
    return mock(JdbcTemplate.class);
  }

  @Bean
  public FolioSpringLiquibase folioSpringLiquibase() {
    return mock(FolioSpringLiquibase.class);
  }
}
