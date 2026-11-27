package com.stocksim.portfolio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    List<Portfolio> findByUserId(UUID userId);

    /** SELECT ... FOR UPDATE: a second withdrawal from the same portfolio waits until the first commits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Portfolio> findWithLockById(UUID id);
}
