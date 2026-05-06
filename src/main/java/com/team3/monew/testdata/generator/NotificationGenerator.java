package com.team3.monew.testdata.generator;

import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.instancio.Model;
import java.util.concurrent.ThreadLocalRandom;
import org.instancio.Instancio;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import static org.instancio.Select.all;
import static org.instancio.Select.field;

@Component
@Profile("data-gen")
public class NotificationGenerator extends AbstractGenerator<Notification> {

  private List<User> userPool = new ArrayList<>(); // 사용자 ID 풀

  public NotificationGenerator(JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor executor) {
    super(jdbcTemplate, executor);
  }

  public void setUsers(List<User> users) {
    if (users == null || users.isEmpty()) {
      throw new IllegalStateException("userPool이 비어 있습니다. setUsers()에 유효한 사용자 목록을 전달해야 합니다.");
    }
    this.userPool = users;
  }

  @Override
  protected Model<Notification> getModel() {
    return Instancio.of(Notification.class)
        .supply(field(Notification::getContent), () -> "새로운 알림이 도착했습니다.")
        .supply(field(Notification::getResourceType), () ->
            ThreadLocalRandom.current().nextBoolean() ? NotificationResourceType.INTEREST
                : NotificationResourceType.COMMENT
        )
        //리소스 아이디는 실제 참조 관계가 아니기 때문에 랜덤 생성
        .supply(field(Notification::getResourceId), java.util.UUID::randomUUID)

        // 2. User매핑 - 유저 더미 데이터 활용
        .supply(field(Notification::getUser), () ->
            userPool.get(ThreadLocalRandom.current().nextInt(userPool.size()))
        )
        .supply(field(Notification::getActorUser), () ->
            userPool.get(ThreadLocalRandom.current().nextInt(userPool.size()))
        )
        .supply(field(Notification::isConfirmed), () ->
            ThreadLocalRandom.current().nextBoolean())
        .toModel();
  }

  @Override
  protected String getSql() {
    return
        "INSERT INTO notifications (id, user_id, content, resource_type, resource_id, actor_user_id, is_confirmed, confirmed_at, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected void setValues(PreparedStatement ps, Notification n) throws SQLException {
    Timestamp createdAt = getUniformTimestamp(7);
    Timestamp updatedAt;
    //확인여부에 따라 수정시간 변경
    if (n.isConfirmed()) {
      // 확인된 알림 - 생성 시간 ~ 현재 시간 사이의 랜덤 시간
      long diffMillis = Instant.now().toEpochMilli() - createdAt.getTime();
      long randomOffset = ThreadLocalRandom.current().nextLong(0, diffMillis + 1);
      updatedAt = new Timestamp(createdAt.getTime() + randomOffset);
    } else {
      // 미확인 알림 - 생성 시간과 수정 시간을 동일
      updatedAt = createdAt;
    }

    ps.setObject(1, n.getId());
    ps.setObject(2, n.getUser().getId());
    ps.setString(3, n.getContent());
    ps.setString(4, n.getResourceType().name());
    ps.setObject(5, n.getResourceId());

    // 리소스타입에 따라 액터유저 유무 변경
    if (n.getResourceType().equals(NotificationResourceType.COMMENT)) {
      ps.setObject(6, n.getActorUser().getId());
    } else {
      ps.setNull(6, java.sql.Types.OTHER);
    }

    ps.setBoolean(7, n.isConfirmed());

    if (n.isConfirmed()) {
      ps.setObject(8, updatedAt);
    } else {
      ps.setNull(8, java.sql.Types.OTHER);
    }
    ps.setTimestamp(9, createdAt);
    ps.setTimestamp(10, updatedAt);
  }

}
