package dev.umut.wallet_api.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountEntryResponse(UUID id, UUID accountId, EntryType type,
                                   BigDecimal amount, BigDecimal balanceAfter, Instant createdAt) {

    static AccountEntryResponse from(AccountEntry account) {
        return new AccountEntryResponse(account.getId(),account.getAccountId(),account.getType(),
                account.getAmount(),account.getBalanceAfter(),account.getCreatedAt());
    }
}
