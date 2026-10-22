package dev.umut.wallet_api.auth;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED,"INVALID_CREDENTIALS","Invalid email or password");
    }
}
