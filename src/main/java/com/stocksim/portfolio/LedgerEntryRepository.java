package com.stocksim.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /** The balance: summed by the database, 0 when there are no entries yet. */
    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.portfolioId = :portfolioId")
    BigDecimal balanceOf(UUID portfolioId);

    List<LedgerEntry> findByPortfolioIdOrderByCreatedAtAsc(UUID portfolioId);
}
