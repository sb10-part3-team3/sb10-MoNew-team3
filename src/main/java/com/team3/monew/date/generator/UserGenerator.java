package com.team3.monew.date.generator;

import static org.instancio.Select.field;

import com.team3.monew.entity.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
public class UserGenerator extends AbstractGenerator<User> {

  public UserGenerator(JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor executor) {
    super(jdbcTemplate, executor);
  }

  @Override
  protected Model<User> getModel() {
    return Instancio.of(User.class)
        .generate(field(User::getEmail), gen -> gen.net().email())
        .toModel();
  }

  @Override
  protected String getSql() {
    return
        "INSERT INTO users (id, email, nickname, password, delete_status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (email) DO NOTHING";
  }

  @Override
  protected void setValues(PreparedStatement ps, User user) throws SQLException {
    ps.setObject(1, user.getId());
    ps.setString(2, user.getEmail());
    ps.setString(3, user.getNickname());
    ps.setString(4, user.getPassword());
    ps.setString(5, "ACTIVE");
    ps.setTimestamp(6, Timestamp.from(Instant.now()));
    ps.setTimestamp(7, Timestamp.from(Instant.now()));
  }
}