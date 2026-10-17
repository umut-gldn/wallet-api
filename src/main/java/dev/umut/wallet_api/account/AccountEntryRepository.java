package dev.umut.wallet_api.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AccountEntryRepository extends JpaRepository<AccountEntry, UUID> {
}
