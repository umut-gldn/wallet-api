package dev.umut.wallet_api.account;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(UUID accountId) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "INSUFFICIENT_FUNDS",
                "Insufficient funds in account: " + accountId);
    }

}
