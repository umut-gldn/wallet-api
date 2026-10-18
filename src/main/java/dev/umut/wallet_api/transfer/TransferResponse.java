package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.account.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID id, UUID fromAccountId, UUID toAccountId,
                               BigDecimal amount, CurrencyCode currency, Instant createdAt) {

    static TransferResponse from(Transfer t) {
        return new TransferResponse(t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getCurrency(), t.getCreatedAt());
    }
}
