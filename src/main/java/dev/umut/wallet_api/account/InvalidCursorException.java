package dev.umut.wallet_api.account;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidCursorException extends BusinessException {

    public InvalidCursorException() {
        super(HttpStatus.BAD_REQUEST,"INVALID_CURSOR","Cursor is invalid or malformed");
    }
}
