package dev.umut.wallet_api.account;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface AccountEntryRepository extends JpaRepository<AccountEntry, UUID> {

    Page<AccountEntry> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);

    List<AccountEntry> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Limit limit);

    @Query("""
            select e from AccountEntry e
            where e.accountId= :accountId 
                and (e.createdAt < :createdAt or (e.createdAt = :createdAt and e.id < :id))
                            order by e.createdAt desc, e.id desc           
            """)
    List<AccountEntry> findNextPage(@Param("accountId") UUID accountId,
                                    @Param("createdAt")Instant createdAt,
                                    @Param("id") UUID id,
                                    Limit limit);
}
