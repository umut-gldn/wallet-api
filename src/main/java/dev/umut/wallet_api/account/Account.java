package dev.umut.wallet_api.account;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal balance;

    public static Account open(String accountNumber,UUID ownerId,CurrencyCode currency){
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.ownerId = ownerId;
        account.currency = currency;
        account.status = AccountStatus.ACTIVE;
        account.createdAt = Instant.now();
        account.balance = BigDecimal.ZERO;
        return account;
    }

    void deposit(BigDecimal amount) {
        requireActive();
        balance = balance.add(amount);
    }

    void withdraw(BigDecimal amount) {
        requireActive();
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id);
        }
        balance = balance.subtract(amount);
    }

    private void requireActive(){
        if(status != AccountStatus.ACTIVE){
            throw new AccountNotActiveException(id,status);
        }
    }
}
