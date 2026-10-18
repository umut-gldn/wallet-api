package dev.umut.wallet_api.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByOwnerIdAndCurrency(UUID ownerId, CurrencyCode currency);

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    long nextAccountNumberSequence();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id= :id")
    Optional<Account> findByIdForUpdate(@Param("id")  UUID id);

    @Query("select a.currency from Account a where a.id= :id ")
    Optional<CurrencyCode> findCurrencyById(@Param("id")  UUID id);
}
