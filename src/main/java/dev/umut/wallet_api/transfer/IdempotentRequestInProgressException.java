package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class IdempotentRequestInProgressException extends BusinessException {

    public IdempotentRequestInProgressException(String key) {
        super(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                "A request with Idempotency-Key '%s' is still being processed. Retry shortly.".formatted(key));
    }
}
