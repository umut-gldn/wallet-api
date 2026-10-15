package dev.umut.wallet_api.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByOwnerIdAndCurrency(UUID ownerId, CurrencyCode currency);

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    long nextAccountNumberSequence();
}
