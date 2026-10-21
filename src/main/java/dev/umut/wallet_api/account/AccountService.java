package dev.umut.wallet_api.account;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
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

    public Page<AccountEntryResponse> getEntries(UUID accountId, Pageable pageable){
        if(!accountRepository.existsById(accountId)){
            throw new AccountNotFoundException(accountId);
        }
        return accountEntryRepository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId,pageable)
                .map(AccountEntryResponse::from);
    }

    public CursorPage<AccountEntryResponse> getEntriesByCursor(UUID accountId, String cursor, int size){

         if(!accountRepository.existsById(accountId)){
            throw new AccountNotFoundException(accountId);
         }
        int limit=Math.min(Math.max(size,1),100);

         List<AccountEntry> rows= cursor ==null ? accountEntryRepository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId,Limit.of(limit+1))
                 : findAfter(accountId,cursor,limit+1);

         boolean hasNext =rows.size() > limit;
         List<AccountEntry> page= hasNext ? rows.subList(0,limit) : rows;
         String nextCursor= hasNext ? encode(page.get(page.size()-1)) : null;

         return new CursorPage<>(page.stream().map(AccountEntryResponse::from).toList(),nextCursor);
    }
    private List<AccountEntry> findAfter(UUID accountId, String cursor, int limit){
        Instant createdAt;
        UUID id;
        try {
            String[] parts=new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            createdAt=Instant.parse(parts[0]);
            id=UUID.fromString(parts[1]);
        }catch (IllegalArgumentException | DateTimeParseException |ArrayIndexOutOfBoundsException e){
            throw new InvalidCursorException();
        }
        return accountEntryRepository.findNextPage(accountId, createdAt,id, Limit.of(limit));
    }
    private String encode(AccountEntry last){
        String raw=last.getCreatedAt()+"|"+last.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
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
