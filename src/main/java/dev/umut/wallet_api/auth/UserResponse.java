package dev.umut.wallet_api.auth;

import java.time.Instant;
import java.util.UUID;

record UserResponse(UUID id, String email, Instant createdAt) {

    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
