package dev.umut.wallet_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
    LoginRequest{
        email=Emails.normalize(email);
    }
}
