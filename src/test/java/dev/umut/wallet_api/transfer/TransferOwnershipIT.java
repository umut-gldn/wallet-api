package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.account.AccountNotFoundException;
import dev.umut.wallet_api.account.AccountService;
import dev.umut.wallet_api.account.CurrencyCode;
import dev.umut.wallet_api.account.OpenAccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class TransferOwnershipIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @Test
    void cannotTransferFromSomeoneElsesAccount() {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID victim = accountService.open(owner, new OpenAccountRequest(CurrencyCode.TRY)).id();
        UUID mine = accountService.open(intruder, new OpenAccountRequest(CurrencyCode.TRY)).id();
        accountService.deposit(owner, victim, new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(intruder, UUID.randomUUID().toString(),
                new TransferRequest(victim, mine, new BigDecimal("50.00"))))
                .isInstanceOf(AccountNotFoundException.class);

        assertThat(accountService.getById(owner, victim).balance()).isEqualByComparingTo("100.00");
        assertThat(accountService.getById(intruder, mine).balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void canTransferToSomeoneElsesAccount() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID from = accountService.open(sender, new OpenAccountRequest(CurrencyCode.TRY)).id();
        UUID to = accountService.open(receiver, new OpenAccountRequest(CurrencyCode.TRY)).id();
        accountService.deposit(sender, from, new BigDecimal("100.00"));

        transferService.transfer(sender, UUID.randomUUID().toString(),
                new TransferRequest(from, to, new BigDecimal("40.00")));

        assertThat(accountService.getById(sender, from).balance()).isEqualByComparingTo("60.00");
        assertThat(accountService.getById(receiver, to).balance()).isEqualByComparingTo("40.00");
    }

    @Test
    void sameIdempotencyKeyFromDifferentUsersIsIndependent() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID from1 = accountService.open(user1, new OpenAccountRequest(CurrencyCode.TRY)).id();
        UUID from2 = accountService.open(user2, new OpenAccountRequest(CurrencyCode.TRY)).id();
        UUID to = accountService.open(receiver, new OpenAccountRequest(CurrencyCode.TRY)).id();
        accountService.deposit(user1, from1, new BigDecimal("100.00"));
        accountService.deposit(user2, from2, new BigDecimal("100.00"));

        String key = "same-key";
        TransferResponse first = transferService.transfer(user1, key,
                new TransferRequest(from1, to, new BigDecimal("10.00")));
        TransferResponse second = transferService.transfer(user2, key,
                new TransferRequest(from2, to, new BigDecimal("10.00")));
        // user1 aynı isteği tekrar gönderince ilk sonucu görür, para ikinci kez gitmez
        TransferResponse replay = transferService.transfer(user1, key,
                new TransferRequest(from1, to, new BigDecimal("10.00")));

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(accountService.getById(receiver, to).balance()).isEqualByComparingTo("20.00");
    }
}