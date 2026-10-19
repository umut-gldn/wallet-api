package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidIdempotencyKeyException extends BusinessException {

    public InvalidIdempotencyKeyException(){
        super(HttpStatus.BAD_REQUEST,"INVALID_IDEMPOTENCY_KEY",
                "Idempotency-Key must not be blank and must be at most 64 characters.");
    }
}
