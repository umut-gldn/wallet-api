package dev.umut.wallet_api.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ConcurrentWithdrawalIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AccountService accountService;

    @Test
    void onlyOneOfTenParallelWithdrawalsSucceeds() throws Exception {
        UUID accountId = accountService
                .open(new OpenAccountRequest(UUID.randomUUID(), CurrencyCode.TRY)).id();
        accountService.deposit(accountId, new BigDecimal("100.00"));

        int threads = 10;
        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Boolean> task = () -> {
                    startGate.await();
                    try {
                        accountService.withdraw(accountId, new BigDecimal("80.00"));
                        return true;
                    } catch (InsufficientFundsException ex) {
                        return false;
                    }
                };
                results.add(executor.submit(task));
            }

            startGate.countDown();

            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
        }

        assertThat(accountService.getById(accountId).balance())
                .isEqualByComparingTo("20.00");
    }
}