package dev.umut.wallet_api.account;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request){
        AccountResponse created=accountService.open(request);
        URI location= ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{accountId}")
    AccountResponse getById(@PathVariable UUID accountId){
        return accountService.getById(accountId);
    }

    @PostMapping("/{accountId}/deposits")
    ResponseEntity<AccountEntryResponse> deposit(@PathVariable UUID accountId,
                                                 @Valid @RequestBody MoneyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.deposit(accountId, request.amount()));
    }

    @PostMapping("/{accountId}/withdrawals")
    ResponseEntity<AccountEntryResponse> withdraw(@PathVariable UUID accountId,
                                                  @Valid @RequestBody MoneyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.withdraw(accountId, request.amount()));
    }
}
