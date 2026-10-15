package dev.umut.wallet_api.account;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(UUID accountId) {
        super(HttpStatus.NOT_FOUND,"ACCOUNT_NOT_FOUND","Account not found: "+accountId);
    }
}
