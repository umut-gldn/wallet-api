package dev.umut.wallet_api.auth;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends BusinessException {

    public EmailAlreadyRegisteredException(String email) {
        super(HttpStatus.CONFLICT,"EMAIL_ALREADY_REGISTERED", "Email already registered: " + email);
    }
}
