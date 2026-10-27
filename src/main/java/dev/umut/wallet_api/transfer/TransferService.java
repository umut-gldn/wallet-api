package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.account.AccountResponse;
import dev.umut.wallet_api.account.AccountService;
import dev.umut.wallet_api.account.CurrencyCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransferService {

    private final AccountService accountService;
    private final TransferRepository transferRepository;

    private static final String IDEMPOTENCY_CONSTRAINT= "uk_transfer_idempotency_key";

    @Transactional
    public TransferResponse transfer(UUID ownerId,String idempotencyKey,TransferRequest request) {
        if(idempotencyKey.isBlank() || idempotencyKey.length()>64){
            throw new InvalidIdempotencyKeyException();
        }
        accountService.requireOwner(ownerId,request.fromAccountId());

        Optional<Transfer> existing= transferRepository.findByIdempotencyKey(idempotencyKey);
        if(existing.isPresent()){
            return replay(existing.get(),idempotencyKey,request);
        }
        UUID fromId=request.fromAccountId();
        UUID toId=request.toAccountId();

        if(fromId.equals(toId)){
            throw new SameAccountTransferException(fromId);
        }

        CurrencyCode fromCurrency=accountService.getCurrency(fromId);
        CurrencyCode toCurrency=accountService.getCurrency(toId);
        if(fromCurrency != toCurrency){
            throw new CurrencyMismatchException(fromCurrency,toCurrency);
        }

        Transfer transfer;
        try {
            transfer=transferRepository.saveAndFlush(
                    Transfer.of(fromId,toId,request.amount(),fromCurrency,idempotencyKey));

        }catch (DataIntegrityViolationException ex) {
            if(violates(ex,IDEMPOTENCY_CONSTRAINT)){
                throw new IdempotentRequestInProgressException(idempotencyKey);
            }
            throw ex;
        }

        if(fromId.compareTo(toId)<0){
            accountService.transferOut(fromId,request.amount(),transfer.getId());
            accountService.transferIn(toId,request.amount(),transfer.getId());
        }else {
            accountService.transferIn(toId,request.amount(),transfer.getId());
            accountService.transferOut(fromId,request.amount(),transfer.getId());
        }

        log.info("Transfer completed: transferId={}, from={}, to={}, amount={}",
                transfer.getId(), fromId, toId, request.amount());
        return TransferResponse.from(transfer);
    }

    private TransferResponse replay(Transfer existing, String idempotencyKey, TransferRequest request){
        boolean sameRequest= existing.getFromAccountId().equals(request.fromAccountId())
                && existing.getToAccountId().equals(request.toAccountId())
                && existing.getAmount().compareTo(request.amount())==0;
        if(!sameRequest){
            throw new IdempotencyKeyReusedException(idempotencyKey);
        }
        return TransferResponse.from(existing);
    }

    private static boolean violates(DataIntegrityViolationException ex, String constraintName) {
        return ex.getCause() instanceof ConstraintViolationException cve
                && constraintName.equalsIgnoreCase(cve.getConstraintName());
    }
}
