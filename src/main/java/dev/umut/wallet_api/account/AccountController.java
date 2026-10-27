package dev.umut.wallet_api.account;

import dev.umut.wallet_api.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
class AccountController {

    private final AccountService accountService;

    @PostMapping
    ResponseEntity<AccountResponse> open(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OpenAccountRequest request){
        AccountResponse created=accountService.open(CurrentUser.id(jwt),request);
        URI location= ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{accountId}")
    AccountResponse getById(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID accountId){
        return accountService.getById(CurrentUser.id(jwt),accountId);
    }

    @PostMapping("/{accountId}/deposits")
    ResponseEntity<AccountEntryResponse> deposit(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID accountId,
                                                 @Valid @RequestBody MoneyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.deposit(CurrentUser.id(jwt),accountId, request.amount()));
    }

    @PostMapping("/{accountId}/withdrawals")
    ResponseEntity<AccountEntryResponse> withdraw(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable UUID accountId,
                                                  @Valid @RequestBody MoneyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.withdraw(CurrentUser.id(jwt),accountId, request.amount()));
    }

    @GetMapping("/{accountId}/entries")
    PagedModel<AccountEntryResponse> getEntries(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable UUID accountId,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(accountService.getEntries(CurrentUser.id(jwt),accountId, pageable));
    }

    @GetMapping("/{accountId}/entries/cursor")
    CursorPage<AccountEntryResponse> getEntriesByCursor(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable UUID accountId,
                                                        @RequestParam(required = false) String cursor,
                                                        @RequestParam(defaultValue = "20") int size){
        return accountService.getEntriesByCursor(CurrentUser.id(jwt),accountId, cursor, size);
    }
}
