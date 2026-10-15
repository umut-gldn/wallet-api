package dev.umut.wallet_api.account;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        UUID ownerId,
        CurrencyCode currency,
        AccountStatus status,
        Instant createdAt
) {
    static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerId(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

}
