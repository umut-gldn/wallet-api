package dev.umut.wallet_api.transfer;

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
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ConcurrentTransferIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @Test
    void oppositeParallelTransfersDoNotDeadlockAndKeepTotalMoney() throws Exception {
        UUID a = accountService.open(new OpenAccountRequest(UUID.randomUUID(), CurrencyCode.TRY)).id();
        UUID b = accountService.open(new OpenAccountRequest(UUID.randomUUID(), CurrencyCode.TRY)).id();
        accountService.deposit(a, new BigDecimal("1000.00"));
        accountService.deposit(b, new BigDecimal("1000.00"));

        int tasks = 100;
        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(20)) {
            List<Future<Void>> results = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                boolean aToB = i % 2 == 0;
                Callable<Void> task = () -> {
                    startGate.await();
                    UUID from = aToB ? a : b;
                    UUID to = aToB ? b : a;
                    transferService.transfer(UUID.randomUUID().toString(),new TransferRequest(from, to, new BigDecimal("10.00")));
                    return null;
                };
                results.add(executor.submit(task));
            }

            startGate.countDown();

            for (Future<Void> result : results) {
                result.get();   // thread içinde hata olduysa burada fırlar ve test kırılır
            }
        }

        assertThat(accountService.getById(a).balance()).isEqualByComparingTo("1000.00");
        assertThat(accountService.getById(b).balance()).isEqualByComparingTo("1000.00");
    }
    @Test
    void sameKeyParallelRequestsMoveMoneyOnlyOnce() throws Exception {
        UUID a = accountService.open(new OpenAccountRequest(UUID.randomUUID(), CurrencyCode.TRY)).id();
        UUID b = accountService.open(new OpenAccountRequest(UUID.randomUUID(), CurrencyCode.TRY)).id();
        accountService.deposit(a, new BigDecimal("1000.00"));

        String key = UUID.randomUUID().toString();
        int threads = 10;
        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<UUID>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<UUID> task = () -> {
                    startGate.await();
                    try {
                        return transferService.transfer(key,
                                new TransferRequest(a, b, new BigDecimal("100.00"))).id();
                    } catch (IdempotentRequestInProgressException ex) {
                        return null;
                    }
                };
                results.add(executor.submit(task));
            }

            startGate.countDown();

            Set<UUID> transferIds = new HashSet<>();
            for (Future<UUID> result : results) {
                UUID id = result.get();
                if (id != null) {
                    transferIds.add(id);
                }
            }
            assertThat(transferIds).hasSize(1);
        }

        assertThat(accountService.getById(a).balance()).isEqualByComparingTo("900.00");
        assertThat(accountService.getById(b).balance()).isEqualByComparingTo("100.00");
    }
}