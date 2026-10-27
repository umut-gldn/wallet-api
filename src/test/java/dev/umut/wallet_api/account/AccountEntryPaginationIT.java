package dev.umut.wallet_api.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AccountEntryPaginationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AccountService accountService;

    // 10, 20, 30, 40, 50 yatırılmış bir hesap açar (en yeni hareket 50)
    // 10, 20, 30, 40, 50 yatırılmış bir hesap açar (en yeni hareket 50)
    private UUID accountWithFiveDeposits(UUID owner) {
        UUID id = accountService.open(owner, new OpenAccountRequest(CurrencyCode.TRY)).id();
        for (int amount = 10; amount <= 50; amount += 10) {
            accountService.deposit(owner, id, new BigDecimal(amount));
        }
        return id;
    }

    private List<Integer> amounts(List<AccountEntryResponse> entries) {
        return entries.stream().map(e -> e.amount().intValue()).toList();
    }

    @Test
    void offsetPaginationRepeatsAnEntryWhenNewEntryArrivesBetweenPages() {
        UUID owner = UUID.randomUUID();
        UUID id = accountWithFiveDeposits(owner);

        Page<AccountEntryResponse> first = accountService.getEntries(owner, id, PageRequest.of(0, 2));
        accountService.deposit(owner, id, new BigDecimal("60"));
        Page<AccountEntryResponse> second = accountService.getEntries(owner, id, PageRequest.of(1, 2));

        assertThat(amounts(first.getContent())).containsExactly(50, 40);
        // beklenen 30, 20 idi; yeni kayıt listeyi kaydırdığı için 40 tekrar geldi
        assertThat(amounts(second.getContent())).containsExactly(40, 30);
    }

    @Test
    void cursorPaginationStaysStableWhenNewEntryArrivesBetweenPages() {
        UUID owner = UUID.randomUUID();
        UUID id = accountWithFiveDeposits(owner);

        CursorPage<AccountEntryResponse> first = accountService.getEntriesByCursor(owner, id, null, 2);
        accountService.deposit(owner, id, new BigDecimal("60"));
        CursorPage<AccountEntryResponse> second = accountService.getEntriesByCursor(owner, id, first.nextCursor(), 2);

        assertThat(amounts(first.content())).containsExactly(50, 40);
        assertThat(amounts(second.content())).containsExactly(30, 20);
    }

    @Test
    void cursorWalksAllEntriesExactlyOnceAndEndsWithNullCursor() {
        UUID owner = UUID.randomUUID();
        UUID id = accountWithFiveDeposits(owner);

        List<Integer> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            CursorPage<AccountEntryResponse> page = accountService.getEntriesByCursor(owner, id, cursor, 2);
            seen.addAll(amounts(page.content()));
            cursor = page.nextCursor();
            pages++;
        } while (cursor != null);

        assertThat(seen).containsExactly(50, 40, 30, 20, 10);
        assertThat(pages).isEqualTo(3);
    }

    @Test
    void unknownAccountThrowsNotFound() {
        UUID owner = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.getEntries(owner, unknown, PageRequest.of(0, 2)))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> accountService.getEntriesByCursor(owner, unknown, null, 2))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void malformedCursorThrowsInvalidCursor() {
        UUID owner = UUID.randomUUID();
        UUID id = accountWithFiveDeposits(owner);

        assertThatThrownBy(() -> accountService.getEntriesByCursor(owner, id, "abc", 2))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> accountService.getEntriesByCursor(owner, id, "aGVsbG8", 2))
                .isInstanceOf(InvalidCursorException.class);
    }
}