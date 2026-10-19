package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class IdempotencyKeyReusedException extends BusinessException {

    public IdempotencyKeyReusedException(String key) {
        super(HttpStatus.UNPROCESSABLE_CONTENT,"IDEMPOTENCY_KEY_REUSED",
                "Idempotency-Key '%s' was already used with a different request.".formatted(key));
    }
}
