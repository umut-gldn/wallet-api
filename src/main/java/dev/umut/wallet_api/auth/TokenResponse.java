package dev.umut.wallet_api.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
