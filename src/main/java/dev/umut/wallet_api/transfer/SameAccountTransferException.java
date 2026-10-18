package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SameAccountTransferException extends BusinessException {

    public SameAccountTransferException(UUID accountId) {
        super(HttpStatus.UNPROCESSABLE_CONTENT,"SAME_ACCOUNT_TRANSFER",
                "Cannot transfer to the same account: " + accountId);
    }
}
