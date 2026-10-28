package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.account.CurrencyCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer",
        uniqueConstraints = @UniqueConstraint(name = "uk_transfer_owner_idempotency_key",columnNames ={"owner_id", "idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "from_account_id", nullable = false, updatable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false, updatable = false)
    private UUID toAccountId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "idempotency_key",nullable = false, updatable = false,length = 64)
    private String idempotencyKey;

    static Transfer of(UUID ownerId,UUID fromAccountId, UUID toAccountId, BigDecimal amount, CurrencyCode currency,String idempotencyKey) {
        Transfer transfer = new Transfer();
        transfer.ownerId = ownerId;
        transfer.fromAccountId = fromAccountId;
        transfer.toAccountId = toAccountId;
        transfer.amount = amount;
        transfer.currency = currency;
        transfer.createdAt = Instant.now();
        transfer.idempotencyKey = idempotencyKey;
        return transfer;
    }

}
