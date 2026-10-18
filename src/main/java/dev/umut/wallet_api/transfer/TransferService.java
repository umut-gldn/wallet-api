package dev.umut.wallet_api.transfer;

import dev.umut.wallet_api.account.AccountResponse;
import dev.umut.wallet_api.account.AccountService;
import dev.umut.wallet_api.account.CurrencyCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransferService {

    private final AccountService accountService;
    private final TransferRepository transferRepository;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
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

        Transfer transfer=transferRepository.save(
                Transfer.of(fromId,toId,request.amount(),fromCurrency));

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
}
