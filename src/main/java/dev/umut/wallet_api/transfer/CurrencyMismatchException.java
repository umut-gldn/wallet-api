package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.account.CurrencyCode;
import dev.umut.wallet_api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CurrencyMismatchException extends BusinessException {

    public CurrencyMismatchException(CurrencyCode from, CurrencyCode to) {
        super(HttpStatus.UNPROCESSABLE_CONTENT,"CURRENCY_MISMATCH",
                "Cannot transfer between %s and %s accounts.".formatted(from,to));
    }
}
