package dev.umut.wallet_api.account;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_account_owner_currency",
                columnNames = {"owner_id","currency"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number",nullable = false,unique = true,updatable = false,length = 16)
    private String accountNumber;

    @Column(name = "owner_id",nullable = false,updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,updatable = false,length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 16)
    private AccountStatus status;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    public static Account open(String accountNumber,UUID ownerId,CurrencyCode currency){
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.ownerId = ownerId;
        account.currency = currency;
        account.status = AccountStatus.ACTIVE;
        account.createdAt = Instant.now();
        return account;
    }
}
