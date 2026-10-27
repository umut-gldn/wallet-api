package dev.umut.wallet_api.account;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenAccountRequest(
        @NotNull CurrencyCode currency
) {
}
