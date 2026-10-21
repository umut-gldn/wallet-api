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
    private UUID accountWithFiveDeposits() {
        UUID id = accountService.open(new OpenAccountRequest(UUID.randomUUID(), CurrencyCode.TRY)).id();
        for (int amount = 10; amount <= 50; amount += 10) {
            accountService.deposit(id, new BigDecimal(amount));
        }
        return id;
    }

    private List<Integer> amounts(List<AccountEntryResponse> entries) {
        return entries.stream().map(e -> e.amount().intValue()).toList();
    }

    @Test
    void offsetPaginationRepeatsAnEntryWhenNewEntryArrivesBetweenPages() {
        UUID id = accountWithFiveDeposits();

        Page<AccountEntryResponse> first = accountService.getEntries(id, PageRequest.of(0, 2));
        accountService.deposit(id, new BigDecimal("60"));
        Page<AccountEntryResponse> second = accountService.getEntries(id, PageRequest.of(1, 2));

        assertThat(amounts(first.getContent())).containsExactly(50, 40);
        // beklenen 30, 20 idi; yeni kayıt listeyi kaydırdığı için 40 tekrar geldi
        assertThat(amounts(second.getContent())).containsExactly(40, 30);
    }

    @Test
    void cursorPaginationStaysStableWhenNewEntryArrivesBetweenPages() {
        UUID id = accountWithFiveDeposits();

        CursorPage<AccountEntryResponse> first = accountService.getEntriesByCursor(id, null, 2);
        accountService.deposit(id,new BigDecimal("60"));
        CursorPage<AccountEntryResponse> second = accountService.getEntriesByCursor(id, first.nextCursor(), 2);

        assertThat(amounts(first.content())).containsExactly(50, 40);
        assertThat(amounts(second.content())).containsExactly(30, 20);
    }

    @Test
    void cursorWalksAllEntriesExactlyOnceAndEndsWithNullCursor() {
        UUID id = accountWithFiveDeposits();

        List<Integer> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            CursorPage<AccountEntryResponse> page = accountService.getEntriesByCursor(id, cursor, 2);
            seen.addAll(amounts(page.content()));
            cursor = page.nextCursor();
            pages++;
        } while (cursor != null);

        assertThat(seen).containsExactly(50, 40, 30, 20, 10);
        assertThat(pages).isEqualTo(3);
    }

    @Test
    void unknownAccountThrowsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.getEntries(unknown, PageRequest.of(0, 2)))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> accountService.getEntriesByCursor(unknown, null, 2))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void malformedCursorThrowsInvalidCursor() {
        UUID id = accountWithFiveDeposits();

        assertThatThrownBy(() -> accountService.getEntriesByCursor(id, "abc", 2))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> accountService.getEntriesByCursor(id, "aGVsbG8", 2))
                .isInstanceOf(InvalidCursorException.class);
    }
}