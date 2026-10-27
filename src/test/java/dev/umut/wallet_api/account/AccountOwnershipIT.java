package dev.umut.wallet_api.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AccountOwnershipIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AccountService accountService;

    @Test
    void otherUserCannotReadOrChangeSomeoneElsesAccount() {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID id = accountService.open(owner, new OpenAccountRequest(CurrencyCode.TRY)).id();
        accountService.deposit(owner, id, new BigDecimal("100.00"));

        assertThatThrownBy(() -> accountService.getById(intruder, id))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> accountService.deposit(intruder, id, new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> accountService.withdraw(intruder, id, new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> accountService.getEntries(intruder, id, PageRequest.of(0, 2)))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> accountService.getEntriesByCursor(intruder, id, null, 2))
                .isInstanceOf(AccountNotFoundException.class);

        // saldırı denemeleri bakiyeyi değiştirmedi
        assertThat(accountService.getById(owner, id).balance()).isEqualByComparingTo("100.00");
    }
}