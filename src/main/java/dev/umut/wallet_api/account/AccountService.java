package dev.umut.wallet_api.account;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private static final String OWNER_CURRENCY_CONSTRAINT= "uk_account_owner_currency";

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AccountEntryRepository accountEntryRepository;

    @Transactional
    public AccountResponse open(OpenAccountRequest request){
        // Yarış durumunu DB constraint'i yakalar; bu kontrol sadece hızlı/okunaklı hata için.
        if(accountRepository.existsByOwnerIdAndCurrency(request.ownerId(),request.currency())){
            throw new AccountAlreadyExistsException(request.ownerId(), request.currency());
        }

        Account account= Account.open(accountNumberGenerator.generate(),request.ownerId(),request.currency());

        try {
            // flush: INSERT'ü commit'te değil şimdi çalıştır, ihlal burada yakalansın.
            accountRepository.saveAndFlush(account);
        }catch (DataIntegrityViolationException ex){
            if (violates(ex,OWNER_CURRENCY_CONSTRAINT)){
                throw new AccountAlreadyExistsException(request.ownerId(), request.currency());
            }
            throw ex;
        }
        log.info("Account opened: accountId={}, ownerId={}, currency={}",
                account.getId(), account.getOwnerId(), account.getCurrency());
        return AccountResponse.from(account);
    }

    public AccountResponse getById(UUID accountId){
        return accountRepository.findById(accountId)
                .map(AccountResponse::from)
                .orElseThrow(()->new AccountNotFoundException(accountId));
    }

    @Transactional
    public AccountEntryResponse deposit(UUID accountId, BigDecimal amount){
        return apply(accountId,EntryType.DEPOSIT,amount,null);
    }

    @Transactional
    public AccountEntryResponse withdraw(UUID accountId, BigDecimal amount){
        return  apply(accountId,EntryType.WITHDRAWAL,amount,null);
    }

    @Transactional
    public AccountEntryResponse transferOut(UUID accountId, BigDecimal amount, UUID transferId) {
        return apply(accountId, EntryType.TRANSFER_OUT, amount, transferId);
    }

    @Transactional
    public AccountEntryResponse transferIn(UUID accountId, BigDecimal amount, UUID transferId) {
        return apply(accountId, EntryType.TRANSFER_IN, amount, transferId);
    }

    public CurrencyCode getCurrency(UUID accountId){
        return accountRepository.findCurrencyById(accountId)
                .orElseThrow(()->new AccountNotFoundException(accountId));
    }

    private AccountEntryResponse apply(UUID accountId, EntryType type,BigDecimal amount,UUID transferId){
        Account account=accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(()->new AccountNotFoundException(accountId));

        if(type.increasesBalance()){
            account.deposit(amount);
        }
        else {
            account.withdraw(amount);
        }
        AccountEntry entry = accountEntryRepository.save(
                AccountEntry.of(account.getId(), type, amount, account.getBalance(), transferId));

        log.info("Entry applied: accountId={}, type={}, amount={}", accountId, type, amount);
        return AccountEntryResponse.from(entry);
    }

    private static boolean violates(DataIntegrityViolationException ex,String constraintName){
        return ex.getCause() instanceof ConstraintViolationException cve && constraintName.equalsIgnoreCase(cve.getConstraintName());
    }
}
