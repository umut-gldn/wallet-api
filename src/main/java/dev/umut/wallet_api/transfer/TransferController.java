package dev.umut.wallet_api.transfer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
class TransferController {

    private final TransferService transferService;

    @PostMapping
    ResponseEntity<TransferResponse> transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                              @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.transfer(idempotencyKey,request));
    }
}
