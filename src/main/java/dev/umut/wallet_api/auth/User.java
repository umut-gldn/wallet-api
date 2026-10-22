package dev.umut.wallet_api.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_email",columnNames = "email")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,updatable = false,length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false,length = 100)
    private String passwordHash;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    public static User register(String email, String passwordHash) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.createdAt = Instant.now();
        return user;
    }
}
