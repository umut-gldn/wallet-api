package dev.umut.wallet_api.account;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_entry",
        indexes = @Index(name = "idx_entry_account_created",columnList ="account_id, created_at" ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private EntryType type;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "transfer_id",updatable = false)
    private UUID transferId;

    static AccountEntry of(UUID accountId, EntryType type, BigDecimal amount, BigDecimal balanceAfter,UUID transferId) {
        AccountEntry entry = new AccountEntry();
        entry.accountId = accountId;
        entry.type = type;
        entry.amount = amount;
        entry.balanceAfter = balanceAfter;
        entry.createdAt = Instant.now();
        entry.transferId = transferId;
        return entry;
    }
}
