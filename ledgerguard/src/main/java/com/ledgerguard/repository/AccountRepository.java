package com.ledgerguard.repository;

import com.ledgerguard.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findAllByOrgId(String orgId);
    Optional<Account> findByIdAndOrgId(Long id, String orgId);
}