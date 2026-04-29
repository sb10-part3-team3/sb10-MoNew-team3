package com.team3.monew.entity;

import com.team3.monew.entity.base.SoftDeleteEntity;
import com.team3.monew.exception.user.InvalidNicknameException;
import com.team3.monew.exception.user.InvalidPasswordException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeleteEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    private Instant purgeScheduledAt;

    @OneToMany(mappedBy = "user")
    private List<Subscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications = new ArrayList<>();

    public static User create(String email, String nickname, String password) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.password = password;

        // 필요하면 기본값 설정
        user.purgeScheduledAt = null;

        return user;
    }

    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidPasswordException();
        }
        this.password = newPassword;
    }

    public void updateNickname(String nickname) {
        if (nickname == null || nickname.isBlank() || nickname.length() < 2 || nickname.length() > 10) {
            throw new InvalidNicknameException();
        }
        this.nickname = nickname;
    }

    @Override
    public void markDeleted() {
        super.markDeleted();
        this.purgeScheduledAt = Instant.now().plus(1, ChronoUnit.DAYS);
    }
}
