package com.team3.monew.testdata.generator;

import static org.instancio.Select.field;

import com.team3.monew.entity.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
public class UserGenerator extends AbstractGenerator<User> {

  private final PasswordEncoder passwordEncoder;
  private final String preEncodedPassword;

  public UserGenerator(JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor executor,
      PasswordEncoder passwordEncoder) {
    super(jdbcTemplate, executor);
    this.passwordEncoder = passwordEncoder;
    this.preEncodedPassword = passwordEncoder.encode("TestPassword123!");
  }

  @Override
  protected Model<User> getModel() {
    return Instancio.of(User.class)
        .generate(field(User::getNickname), gen -> gen.string().minLength(2).maxLength(10))
        .supply(field(User::getEmail), () -> {
          String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
          String email = "user_" + uniqueId + "@monew.com";
          return email.length() > 255 ? email.substring(0, 255) : email;
        })
        .supply(field(User::getPassword), () -> this.preEncodedPassword)
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
    Timestamp createdAt = getUniformTimestamp(30);
    // 수정시간은 생성시간보다 이후, 랜덤
    long diffMillis = Instant.now().toEpochMilli() - createdAt.getTime();
    long randomOffset = ThreadLocalRandom.current().nextLong(0, diffMillis + 1);
    Timestamp updatedAt = new Timestamp(createdAt.getTime() + randomOffset);

    ps.setObject(1, user.getId());
    ps.setString(2, user.getEmail());
    ps.setString(3, user.getNickname());
    ps.setString(4, user.getPassword());
    ps.setString(5, "ACTIVE");
    ps.setTimestamp(6, createdAt);
    ps.setTimestamp(7, updatedAt);
  }
}