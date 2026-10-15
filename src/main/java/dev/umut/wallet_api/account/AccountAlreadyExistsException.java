package dev.umut.wallet_api.account;

import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountAlreadyExistsException extends BusinessException {

    public AccountAlreadyExistsException(UUID ownerId, CurrencyCode currency) {
        super(HttpStatus.CONFLICT,"ACCOUNT_ALREADY_EXISTS","Owner %s already has a %s account.".formatted(ownerId, currency));
    }
}
