package com.team3.monew.global.init;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.diagnose-db", havingValue = "true")
public class DatabaseDiagnosticsRunner implements CommandLineRunner {

  private static final String ENUM_TYPES_QUERY = """
      select typname
      from pg_type
      where typname in (
        'news_source_types',
        'delete_statuses',
        'notification_types',
        'notification_resource_types',
        'backup_job_types',
        'backup_job_statuses'
      )
      order by typname
      """;

  private static final String COLUMN_INFO_QUERY = """
      select column_name, data_type, udt_name
      from information_schema.columns
      where table_schema = 'public'
        and table_name = ?
      order by ordinal_position
      """;

  private final JdbcTemplate jdbcTemplate;
  private final DataSourceProperties dataSourceProperties;

  @Override
  public void run(String... args) {
    log.info("[DB-DIAG] spring.datasource.url={}", dataSourceProperties.getUrl());
    log.info("[DB-DIAG] spring.datasource.username={}", dataSourceProperties.getUsername());
    log.info("[DB-DIAG] version/current_database/current_schema/server_addr/server_port={}",
        querySingle("""
            select version(),
                   current_database(),
                   current_schema(),
                   coalesce(inet_server_addr()::text, 'null'),
                   inet_server_port()
            """));
    log.info("[DB-DIAG] news_sources columns={}", queryRows(COLUMN_INFO_QUERY, "news_sources"));
    log.info("[DB-DIAG] notifications columns={}", queryRows(COLUMN_INFO_QUERY, "notifications"));
    log.info("[DB-DIAG] enum types={}", queryRows(ENUM_TYPES_QUERY));
  }

  private List<Map<String, Object>> queryRows(String sql, Object... args) {
    return jdbcTemplate.queryForList(sql, args);
  }

  private Map<String, Object> querySingle(String sql) {
    return jdbcTemplate.queryForMap(sql);
  }
}
