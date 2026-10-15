package dev.umut.wallet_api.account;

import org.springframework.stereotype.Component;

@Component
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;

    AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    String generate(){
        String body= "%09d".formatted(accountRepository.nextAccountNumberSequence());
        return body+Luhn.checkDigit(body);
    }
}
