package dev.umut.wallet_api.account;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountNotActiveException extends BusinessException {

    public AccountNotActiveException(UUID accountId, AccountStatus accountStatus) {
        super(HttpStatus.CONFLICT,"ACCOUNT_NOT_ACTIVE",
                "Account %s is %s.".formatted(accountId, accountStatus));
    }
}
